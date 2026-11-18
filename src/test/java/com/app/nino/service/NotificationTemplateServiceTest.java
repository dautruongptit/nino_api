package com.app.nino.service;

import com.app.nino.model.entity.Event;
import com.app.nino.model.entity.EventCategory;
import com.app.nino.model.entity.EventReminder;
import com.app.nino.model.entity.Relative;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationTemplateServiceTest {

    private final NotificationTemplateService service = new NotificationTemplateService();

    private EventCategory category(String code, String displayName, String icon) {
        return EventCategory.builder().code(code).displayName(displayName).icon(icon).build();
    }

    private Event.EventBuilder baseEvent(EventCategory category) {
        return Event.builder().id(1L).title("Họp nhóm").category(category);
    }

    private EventReminder.EventReminderBuilder baseReminder() {
        return EventReminder.builder().id(1L);
    }

    // ── Title: icon->emoji + sắp tới/sắp đến theo timing ────────────────────

    @Test
    void build_daysBucket_titleUsesSapToi() {
        Event event = baseEvent(category("KHAC", "Khác", "more_horiz")).build();
        EventReminder reminder = baseReminder().remindDaysBefore(3).build();

        var content = service.build(event, reminder);

        assertEquals("🔔 Khác sắp tới", content.title());
    }

    @Test
    void build_hoursBucket_titleUsesSapDen() {
        Event event = baseEvent(category("KHAC", "Khác", "more_horiz")).build();
        EventReminder reminder = baseReminder().remindHoursBefore(1).build();

        var content = service.build(event, reminder);

        assertEquals("🔔 Khác sắp đến", content.title());
    }

    @Test
    void build_exactBucket_titleUsesSapDen() {
        Event event = baseEvent(category("KHAC", "Khác", "more_horiz")).build();
        EventReminder reminder = baseReminder().build();

        var content = service.build(event, reminder);

        assertEquals("🔔 Khác sắp đến", content.title());
    }

    @Test
    void build_sinhNhat_mapsCakeIcon() {
        Event event = baseEvent(category("SINH_NHAT", "Sinh nhật", "cake")).build();
        EventReminder reminder = baseReminder().remindDaysBefore(1).build();

        var content = service.build(event, reminder);

        assertEquals("🎂 Sinh nhật sắp tới", content.title());
    }

    @Test
    void build_kyNiem_mapsFavoriteIcon() {
        Event event = baseEvent(category("KY_NIEM", "Kỷ niệm", "favorite")).build();
        EventReminder reminder = baseReminder().remindDaysBefore(1).build();

        var content = service.build(event, reminder);

        assertEquals("💝 Kỷ niệm sắp tới", content.title());
    }

    @Test
    void build_le_mapsCardGiftcardIcon() {
        Event event = baseEvent(category("LE", "Lễ/Tết", "card_giftcard")).build();
        var content = service.build(event, baseReminder().remindDaysBefore(1).build());
        assertEquals("🎁 Lễ/Tết sắp tới", content.title());
    }

    @Test
    void build_nhaO_mapsHomeIcon() {
        Event event = baseEvent(category("NHA_O", "Nhà ở", "home")).build();
        var content = service.build(event, baseReminder().remindDaysBefore(1).build());
        assertEquals("🏠 Nhà ở sắp tới", content.title());
    }

    @Test
    void build_hoaDon_mapsBoltIcon() {
        Event event = baseEvent(category("HOA_DON", "Hóa đơn", "bolt")).build();
        var content = service.build(event, baseReminder().remindDaysBefore(1).build());
        assertEquals("⚡ Hóa đơn sắp tới", content.title());
    }

    @Test
    void build_muaSam_mapsShoppingBagIcon() {
        Event event = baseEvent(category("MUA_SAM", "Mua sắm", "shopping_bag")).build();
        var content = service.build(event, baseReminder().remindDaysBefore(1).build());
        assertEquals("🛍️ Mua sắm sắp tới", content.title());
    }

    @Test
    void build_unrecognizedIcon_fallsBackToBellEmoji() {
        // Danh mục tuỳ tạo trong tương lai có thể chọn icon bất kỳ.
        Event event = baseEvent(category("CUSTOM_1_123", "Du lịch", "flight")).build();
        var content = service.build(event, baseReminder().remindDaysBefore(1).build());
        assertEquals("🔔 Du lịch sắp tới", content.title());
    }

    @Test
    void build_nullIcon_fallsBackToBellEmoji() {
        // Defensive guard: even though EventCategory.icon is non-nullable at DB level,
        // defensive coding ensures null doesn't cause NullPointerException.
        Event event = baseEvent(category("KHAC", "Khác", null)).build();
        var content = service.build(event, baseReminder().remindDaysBefore(1).build());
        assertEquals("🔔 Khác sắp tới", content.title());
    }

    // ── Body: subject theo category + timing phrase ─────────────────────────

    @Test
    void build_nonBirthdayCategory_bodyUsesEventTitle() {
        Event event = baseEvent(category("KHAC", "Khác", "more_horiz")).build();
        var content = service.build(event, baseReminder().remindDaysBefore(1).build());
        assertEquals("Họp nhóm · Ngày mai", content.body());
    }

    @Test
    void build_birthdayWithRelative_bodyUsesRelativeName() {
        Relative relative = Relative.builder().id(5L).name("Mẹ").build();
        Event event = Event.builder().id(1L).title("Sinh nhật Mẹ")
            .category(category("SINH_NHAT", "Sinh nhật", "cake")).relative(relative).build();

        var content = service.build(event, baseReminder().remindDaysBefore(1).build());

        assertEquals("Sinh nhật Mẹ · Ngày mai", content.body());
    }

    @Test
    void build_birthdayWithRelativeNickname_bodyPrefersNicknameOverName() {
        // RelativeService.java:267 uu tien nickname hon ten that khi hien thi
        // — notification phai theo dung quy tac nay, khong duoc dung
        // getName() suong khi nickname da co san.
        Relative relative = Relative.builder().id(5L).name("Nguyễn Thị A").nickname("Mẹ").build();
        Event event = Event.builder().id(1L).title("Sinh nhật Mẹ")
            .category(category("SINH_NHAT", "Sinh nhật", "cake")).relative(relative).build();

        var content = service.build(event, baseReminder().remindDaysBefore(1).build());

        assertEquals("Sinh nhật Mẹ · Ngày mai", content.body());
    }

    @Test
    void build_birthdayWithoutRelative_bodyUsesEventTitleWithoutDoublingPrefix() {
        // event.getTitle() đã là "Sinh nhật của tôi" — KHÔNG được cộng thêm
        // "Sinh nhật " lần nữa.
        Event event = baseEvent(category("SINH_NHAT", "Sinh nhật", "cake"))
            .title("Sinh nhật của tôi").build();

        var content = service.build(event, baseReminder().remindDaysBefore(3).build());

        assertEquals("Sinh nhật của tôi · Còn 3 ngày", content.body());
    }

    // ── Timing phrase: từng nhánh ─────────────────────────────────────────

    @Test
    void timingPhrase_oneDayBefore_returnsNgayMai() {
        Event event = baseEvent(category("KHAC", "Khác", "more_horiz")).build();
        var content = service.build(event, baseReminder().remindDaysBefore(1).build());
        assertEquals("Họp nhóm · Ngày mai", content.body());
    }

    @Test
    void timingPhrase_multipleDaysBefore_returnsConNNgay() {
        Event event = baseEvent(category("KHAC", "Khác", "more_horiz")).build();
        var content = service.build(event, baseReminder().remindDaysBefore(7).build());
        assertEquals("Họp nhóm · Còn 7 ngày", content.body());
    }

    @Test
    void timingPhrase_hoursBeforeZero_returnsSau30Phut() {
        // Quy ước mobile: hoursBefore=0 nghĩa là "30 phút trước".
        Event event = baseEvent(category("KHAC", "Khác", "more_horiz")).build();
        var content = service.build(event, baseReminder().remindHoursBefore(0).build());
        assertEquals("Họp nhóm · Sau 30 phút", content.body());
    }

    @Test
    void timingPhrase_hoursBeforePositive_returnsSauNGio() {
        Event event = baseEvent(category("KHAC", "Khác", "more_horiz")).build();
        var content = service.build(event, baseReminder().remindHoursBefore(3).build());
        assertEquals("Họp nhóm · Sau 3 giờ", content.body());
    }

    @Test
    void timingPhrase_minutesBeforePositive_returnsSauNPhut() {
        Event event = baseEvent(category("KHAC", "Khác", "more_horiz")).build();
        var content = service.build(event, baseReminder().remindMinutesBefore(15).build());
        assertEquals("Họp nhóm · Sau 15 phút", content.body());
    }

    @Test
    void timingPhrase_noOffsets_returnsHomNay() {
        Event event = baseEvent(category("KHAC", "Khác", "more_horiz")).build();
        var content = service.build(event, baseReminder().build());
        assertEquals("Họp nhóm · Hôm nay", content.body());
    }

    // ── Timing phrase theo thời gian còn lại thực tế (có now) ───────────────

    private Event eventAt(int hour, int minute) {
        return baseEvent(category("KHAC", "Khác", "more_horiz"))
            .eventDate(LocalDate.of(2026, 11, 18)).eventTime(LocalTime.of(hour, minute)).build();
    }

    @Test
    void actualTiming_firedLate_usesRealRemainingTime() {
        // Sự kiện "a": tạo 01:34 cho 02:00, mốc "1 giờ trước" bắn ngay 01:35.
        var content = service.build(eventAt(2, 0), baseReminder().remindHoursBefore(1).build(),
            LocalDateTime.of(2026, 11, 18, 1, 35, 0, 340_000));
        assertEquals("Họp nhóm · Sau 25 phút", content.body());
    }

    @Test
    void actualTiming_onTime_keepsNominalLabel() {
        var content = service.build(eventAt(2, 0), baseReminder().remindHoursBefore(1).build(),
            LocalDateTime.of(2026, 11, 18, 1, 0, 0, 600_000));
        assertEquals("Họp nhóm · Sau 1 giờ", content.body());
    }

    @Test
    void actualTiming_withinScanTolerance_keepsNominalLabel() {
        // Giờ sự kiện lẻ (17:47) -> trigger 16:47, lượt quét 5' bắt lúc 16:50.
        var content = service.build(eventAt(17, 47), baseReminder().remindHoursBefore(1).build(),
            LocalDateTime.of(2026, 11, 18, 16, 50));
        assertEquals("Họp nhóm · Sau 1 giờ", content.body());
    }

    @Test
    void actualTiming_hoursAndMinutesRemaining() {
        var content = service.build(eventAt(12, 0), baseReminder().remindHoursBefore(3).build(),
            LocalDateTime.of(2026, 11, 18, 10, 20));
        assertEquals("Họp nhóm · Sau 1 giờ 40 phút", content.body());
    }

    @Test
    void actualTiming_daysScaleFiredLate_usesDateDifference() {
        // Mốc "3 ngày trước" nhưng chỉ bắn được khi còn 1 ngày.
        var content = service.build(eventAt(9, 0), baseReminder().remindDaysBefore(3).build(),
            LocalDateTime.of(2026, 11, 17, 9, 0));
        assertEquals("Họp nhóm · Ngày mai", content.body());
    }
}
