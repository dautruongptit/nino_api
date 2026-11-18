package com.app.nino.service;

import com.app.nino.model.entity.Event;
import com.app.nino.model.entity.EventCategory;
import com.app.nino.model.entity.EventReminder;
import com.app.nino.model.entity.Relative;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Sinh title/body cho Notification, thay cho chuoi hardcode truoc day o
 *  ReminderScheduler. Title suy ra tu du lieu category co san (displayName +
 *  icon) — khong hardcode rieng tung category — de category user tu tao sau
 *  nay (chua co UI, nhung schema da ho tro) tu dong co template hop ly ma
 *  khong can sua code. Body chi co 1 quy tac rieng cho SINH_NHAT (goi ten
 *  nguoi than), con lai deu dung event.getTitle(). */
@Service
public class NotificationTemplateService {

    public record NotificationContent(String title, String body) {}

    private static final String DEFAULT_ICON_EMOJI = "🔔";
    private static final Map<String, String> ICON_EMOJI = Map.of(
        "cake", "🎂",
        "favorite", "💝",
        "card_giftcard", "🎁",
        "home", "🏠",
        "bolt", "⚡",
        "shopping_bag", "🛍️",
        "more_horiz", "🔔"
    );

    private static final String BIRTHDAY_CATEGORY_CODE = "SINH_NHAT";

    /** Sai số cho phép giữa thời gian còn lại thực tế và mốc nhắc danh nghĩa
     *  trước khi chuyển sang ghi thời gian thực tế — bằng chu kỳ quét của
     *  ReminderScheduler (5 phút), để nhắc bắn đúng hạn vẫn giữ nhãn tròn
     *  "Sau 1 giờ" thay vì "Sau 57 phút". */
    static final int ON_TIME_TOLERANCE_MINUTES = 5;

    /** Nhãn theo mốc danh nghĩa — dùng khi không biết thời điểm bắn. */
    public NotificationContent build(Event event, EventReminder reminder) {
        return build(event, reminder, null);
    }

    /**
     * @param now thời điểm bắn thực tế. Nếu nhắc bị bắn trễ (VD: tạo sự kiện
     *            lúc 01:34 cho 02:00 với mốc "1 giờ trước" -> bắn ngay lúc
     *            01:35), nhãn ghi theo thời gian CÒN LẠI thật ("Sau 25 phút")
     *            thay vì mốc danh nghĩa ("Sau 1 giờ") gây hiểu nhầm.
     */
    public NotificationContent build(Event event, EventReminder reminder, LocalDateTime now) {
        EventCategory category = event.getCategory();
        String icon = category.getIcon();
        String emoji = icon != null ? ICON_EMOJI.getOrDefault(icon, DEFAULT_ICON_EMOJI) : DEFAULT_ICON_EMOJI;
        boolean daysScale = isDaysScale(reminder);
        String title = emoji + " " + category.getDisplayName() + (daysScale ? " sắp tới" : " sắp đến");
        String timing = (now != null && event.getEventDate() != null)
            ? actualTimingPhrase(event, reminder, now)
            : timingPhrase(reminder);
        String body = subject(event, category) + " · " + timing;
        return new NotificationContent(title, body);
    }

    private String actualTimingPhrase(Event event, EventReminder reminder, LocalDateTime now) {
        if (isDaysScale(reminder)) {
            long days = ChronoUnit.DAYS.between(now.toLocalDate(), event.getEventDate());
            if (days <= 0) return "Hôm nay";
            return days == 1 ? "Ngày mai" : "Còn " + days + " ngày";
        }
        LocalDateTime eventAt = LocalDateTime.of(event.getEventDate(),
            event.getEventTime() != null ? event.getEventTime() : LocalTime.of(8, 0));
        long remaining = ChronoUnit.MINUTES.between(now.truncatedTo(ChronoUnit.MINUTES), eventAt);
        if (Math.abs(remaining - reminder.offsetMinutes()) <= ON_TIME_TOLERANCE_MINUTES) {
            return timingPhrase(reminder);
        }
        if (remaining <= 0) return "Hôm nay";
        if (remaining < 60) return "Sau " + remaining + " phút";
        long h = remaining / 60;
        long m = remaining % 60;
        return m == 0 ? "Sau " + h + " giờ" : "Sau " + h + " giờ " + m + " phút";
    }

    private boolean isDaysScale(EventReminder reminder) {
        return reminder.getRemindDaysBefore() != null && reminder.getRemindDaysBefore() > 0;
    }

    private String subject(Event event, EventCategory category) {
        if (!BIRTHDAY_CATEGORY_CODE.equals(category.getCode())) {
            return event.getTitle();
        }
        // RelativeService tao Event "Sinh nhat" voi title da la "Sinh nhat " +
        // displayName (xem RelativeService.java:271) — khong duoc cong them
        // "Sinh nhat " lan nua o day khi khong co relative, se bi lap chu.
        if (event.getRelative() == null) {
            return event.getTitle();
        }
        Relative relative = event.getRelative();
        // Uu tien nickname hon ten that, giong quy tac hien thi da thiet lap
        // o RelativeService.java:267 — nickname than mat hon (vd "Me" thay vi
        // "Nguyen Thi A"), dong bo voi title cua chinh Event sinh nhat do
        // RelativeService tu tao.
        String displayName = relative.getNickname() != null ? relative.getNickname() : relative.getName();
        return "Sinh nhật " + displayName;
    }

    private String timingPhrase(EventReminder reminder) {
        Integer days = reminder.getRemindDaysBefore();
        Integer hours = reminder.getRemindHoursBefore();
        Integer minutes = reminder.getRemindMinutesBefore();

        if (days != null && days > 0) {
            return days == 1 ? "Ngày mai" : "Còn " + days + " ngày";
        }
        if (hours != null && hours == 0) {
            return "Sau 30 phút";
        }
        if (hours != null && hours > 0) {
            return "Sau " + hours + " giờ";
        }
        if (minutes != null && minutes > 0) {
            return "Sau " + minutes + " phút";
        }
        return "Hôm nay";
    }
}
