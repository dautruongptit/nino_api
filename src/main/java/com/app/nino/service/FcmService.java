package com.app.nino.service;

import com.app.nino.model.entity.UserDevice;
import com.app.nino.repository.LoginHistoryRepository;
import com.app.nino.repository.UserDeviceRepository;
import java.time.LocalDateTime;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final UserDeviceRepository   deviceRepo;
    private final LoginHistoryRepository loginHistoryRepo;

    /**
     * Gui push notification toi TAT CA thiet bi cua 1 user.
     * Tu dong don dep token khong con hop le (app bi go, token het han).
     *
     * @param userId  ID user nhan thong bao
     * @param title   Tieu de push
     * @param body    Noi dung push
     * @param data    Du lieu kem theo (VD: eventId de deep-link khi bam vao push)
     */
    @Transactional
    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {

        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("[FCM] FirebaseApp chua duoc khoi tao — bo qua gui push (userId={})", userId);
            return;
        }

        // Kiem tra phien dang nhap hop le: neu user khong con phien nao hoat dong
        // (da dang xuat hoac tat ca phien da het han), tuyet doi khong gui push notification ve may.
        boolean hasActiveSession = loginHistoryRepo.findByUserIdAndRevokedAtIsNullAndSessionIdIsNotNull(userId)
            .stream()
            .anyMatch(h -> h.getRefreshExpiresAt() != null && h.getRefreshExpiresAt().isAfter(LocalDateTime.now()));

        if (!hasActiveSession) {
            log.info("[FCM] User {} khong con phien dang nhap hop le (da dang xuat hoac het han) — bo qua gui push", userId);
            return;
        }

        List<UserDevice> devices = deviceRepo.findByUserId(userId);
        if (devices.isEmpty()) {
            log.debug("[FCM] User {} chua dang ky thiet bi nao — bo qua push", userId);
            return;
        }

        // Am thanh/rung tren Android 8+ do channel quyet dinh, nen gom thiet bi
        // theo cai dat (chuong, rung) va gui moi nhom 1 lan voi channel tuong ung.
        Map<String, List<UserDevice>> groups = devices.stream()
            .collect(Collectors.groupingBy(d -> channelIdFor(isOn(d.getNotifySound()), isOn(d.getNotifyVibrate()))));

        groups.forEach((channelId, group) -> {
            boolean sound = isOn(group.get(0).getNotifySound());
            boolean vibrate = isOn(group.get(0).getNotifyVibrate());
            List<String> tokens = group.stream().map(UserDevice::getFcmToken).collect(Collectors.toList());

            MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(createDataPayload(title, body, data))
                .addAllTokens(tokens)
                .setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(androidNotification(channelId, sound, vibrate))
                    .build())
                .setApnsConfig(ApnsConfig.builder().setAps(apsFor(sound)).build())
                .build();

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                log.info("[FCM] Gui push userId={} channel={}: {} thanh cong, {} that bai",
                    userId, channelId, response.getSuccessCount(), response.getFailureCount());

                if (response.getFailureCount() > 0) {
                    cleanupInvalidTokens(tokens, response);
                }
            } catch (FirebaseMessagingException e) {
                log.error("[FCM] Loi gui push userId={}: {}", userId, e.getMessage());
            }
        });
    }

    // ── Channel theo cai dat chuong/rung ─────────────────────────────────────
    // Phai KHOP voi 4 channel app tao san (mobile: NotificationChannels).
    public static final String CHANNEL_SOUND_VIBRATE = "nino_event_reminders";
    public static final String CHANNEL_SOUND_ONLY     = "nino_event_reminders_sound";
    public static final String CHANNEL_VIBRATE_ONLY   = "nino_event_reminders_vibrate";
    public static final String CHANNEL_SILENT         = "nino_event_reminders_silent";

    public static String channelIdFor(boolean sound, boolean vibrate) {
        if (sound && vibrate) return CHANNEL_SOUND_VIBRATE;
        if (sound)            return CHANNEL_SOUND_ONLY;
        if (vibrate)          return CHANNEL_VIBRATE_ONLY;
        return CHANNEL_SILENT;
    }

    /** null (thiet bi dang ky truoc khi co cot) coi nhu bat. */
    private static boolean isOn(Boolean value) {
        return value == null || value;
    }

    private static AndroidNotification androidNotification(String channelId, boolean sound, boolean vibrate) {
        AndroidNotification.Builder builder = AndroidNotification.builder()
            .setChannelId(channelId)
            .setPriority(AndroidNotification.Priority.MAX)
            .setVisibility(AndroidNotification.Visibility.PUBLIC);
        // Android < 8 khong co channel nen cung dat theo tung message.
        if (sound) builder.setSound("default").setDefaultSound(true);
        if (vibrate) builder.setDefaultVibrateTimings(true);
        return builder.build();
    }

    /** iOS: tat chuong = khong dat sound (iOS khong cho app dieu khien rung rieng). */
    private static Aps apsFor(boolean sound) {
        Aps.Builder aps = Aps.builder().setBadge(1);
        if (sound) aps.setSound("default");
        return aps.build();
    }

    /**
     * Xoa nhung token khong con hop le (UNREGISTERED, INVALID_ARGUMENT)
     * de tranh gui push that bai lien tuc vao lan sau.
     */
    private void cleanupInvalidTokens(List<String> tokens, BatchResponse response) {
        List<String> invalidTokens = new java.util.ArrayList<>();
        List<SendResponse> responses = response.getResponses();

        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                MessagingErrorCode errorCode = sendResponse.getException().getMessagingErrorCode();
                if (errorCode == MessagingErrorCode.UNREGISTERED
                        || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    invalidTokens.add(tokens.get(i));
                }
            }
        }

        if (!invalidTokens.isEmpty()) {
            deviceRepo.deleteAllByFcmTokenIn(invalidTokens);
            log.info("[FCM] Da xoa {} token khong hop le", invalidTokens.size());
        }
    }

    /**
     * Gui push test (dung khi client vua dang ky token, xac nhan push hoat dong).
     */
    public void sendTestPush(String fcmToken) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("[FCM] FirebaseApp chua khoi tao — khong the gui test push");
            return;
        }

        Message message = Message.builder()
            .setToken(fcmToken)
            .setAndroidConfig(AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                    .setSound("default")
                    .setChannelId("nino_event_reminders")
                    .setDefaultSound(true)
                    .setDefaultVibrateTimings(true)
                    .setPriority(AndroidNotification.Priority.MAX)
                    .setVisibility(AndroidNotification.Visibility.PUBLIC)
                    .build())
                .build())
            .setNotification(Notification.builder()
                .setTitle("Kết nối thành công")
                .setBody("Thiết bị của bạn đã sẵn sàng nhận thông báo từ Nino")
                .build())
            .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] Test push gui thanh cong: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] Test push that bai: {}", e.getMessage());
        }
    }

    private Map<String, String> createDataPayload(String title, String body, Map<String, String> data) {
        Map<String, String> payload = new java.util.HashMap<>(data != null ? data : Map.of());
        if (title != null) payload.putIfAbsent("title", title);
        if (body != null) payload.putIfAbsent("body", body);
        return payload;
    }
}
