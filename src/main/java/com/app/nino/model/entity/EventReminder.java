package com.app.nino.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "event_reminders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "remind_days_before")
    private Integer remindDaysBefore;

    @Column(name = "remind_hours_before")
    private Integer remindHoursBefore;

    @Column(name = "remind_minutes_before")
    private Integer remindMinutesBefore;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    /** Thời điểm lần gần nhất đã gửi nhắc nhở cho lần trigger hiện tại — dùng
     * để ReminderScheduler tránh gửi trùng khi quét lại (NULL = chưa từng gửi). */
    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    /** Nếu có giá trị: sau lần bắn đầu tiên, tự bắn lại mỗi N phút cho tới khi
     * người dùng đọc thông báo ở chuông (VD: nhắc uống thuốc mỗi 30 phút). */
    @Column(name = "repeat_interval_minutes")
    private Integer repeatIntervalMinutes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** Quy ước mobile: remindHoursBefore == 0 nghĩa là "30 phút trước"
     *  (event_form_screen.dart._reminderLabel, local_notification_service.dart). */
    public static final int HOURS_ZERO_MEANS_MINUTES = 30;

    /** Tính thời điểm trigger thực sự dựa trên ngày/giờ sự kiện + mốc nhắc */
    public LocalDateTime computeTriggerTime(LocalDate eventDate, LocalTime eventTime) {
        LocalDateTime base = LocalDateTime.of(eventDate, eventTime != null ? eventTime : LocalTime.of(8, 0));
        return base.minusMinutes(offsetMinutes());
    }

    /**
     * Số phút trước sự kiện mà mốc nhắc này bắn — PHẢI khớp mobile
     * (ReminderModel.displayText / _computeTriggerTime). Mobile gửi "30 phút
     * trước" dưới dạng {remindDaysBefore: 0, remindHoursBefore: 0}; trước đây
     * nhánh days != null bắt trước nên ra minusDays(0) = đúng giờ sự kiện,
     * trong khi nhãn thông báo lại ghi "Sau 30 phút" (sự kiện "a" 2026-11-18).
     * Vì vậy days chỉ tính khi > 0.
     */
    public int offsetMinutes() {
        if (remindDaysBefore != null && remindDaysBefore > 0) {
            return remindDaysBefore * 24 * 60;
        }
        if (remindHoursBefore != null) {
            return remindHoursBefore == 0 ? HOURS_ZERO_MEANS_MINUTES : remindHoursBefore * 60;
        }
        if (remindMinutesBefore != null && remindMinutesBefore > 0) {
            return remindMinutesBefore;
        }
        return 0;
    }
}
