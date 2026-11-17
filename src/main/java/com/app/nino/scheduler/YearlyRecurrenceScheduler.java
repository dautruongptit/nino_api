package com.app.nino.scheduler;

import com.app.nino.model.entity.Event;
import com.app.nino.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Dời event_date của sự kiện SINH NHẬT (danh mục SINH_NHAT, lặp YEARLY theo
 * Dương lịch) sang năm sau — nhưng chỉ khi sinh nhật hiện tại đã qua hơn 1
 * tuần ({@value #GRACE_DAYS} ngày). Trong tuần đó sự kiện vẫn mang ngày vừa qua
 * nên còn hiện ở màn Chi tiết người thân; sau đó mới chuyển sang năm sau.
 *
 * Chỉ áp dụng cho Sinh nhật. Ngày giỗ lặp theo Âm lịch (LUNAR_YEARLY) do
 * LunarRecurrenceScheduler dời. Các sự kiện YEARLY khác (kỷ niệm, nhắc việc...)
 * không bị job này đụng tới.
 *
 * Chạy 6:30 sáng (trước LunarRecurrenceScheduler 7:00 và ReminderScheduler
 * 8:00) và 1 lần lúc app khởi động để sửa dữ liệu đã trễ sẵn. Idempotent.
 *
 * Nhắc nhở không cần reset: ReminderScheduler.isDue coi reminder có notifiedAt
 * cũ hơn mốc nhắc mới là chưa báo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YearlyRecurrenceScheduler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final String BIRTHDAY_CATEGORY_CODE = "SINH_NHAT";
    /** Số ngày sinh nhật vừa qua vẫn được giữ nguyên ngày cũ trước khi dời sang năm sau. */
    static final int GRACE_DAYS = 7;

    private final EventRepository eventRepo;

    @Scheduled(cron = "0 30 6 * * *", zone = "Asia/Ho_Chi_Minh")
    public void scheduledRoll() {
        rollForwardYearlyEvents();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void rollOnStartup() {
        rollForwardYearlyEvents();
    }

    @Transactional
    public void rollForwardYearlyEvents() {
        LocalDate today = LocalDate.now(ZONE);
        // Chỉ dời khi đã qua hơn GRACE_DAYS ngày: eventDate < today - 7.
        List<Event> stale = eventRepo.findByRecurrenceTypeAndCategory_CodeAndIsActiveTrueAndEventDateBefore(
            Event.RecurrenceType.YEARLY, BIRTHDAY_CATEGORY_CODE, today.minusDays(GRACE_DAYS));

        for (Event event : stale) {
            // Lần tới không sớm hơn hôm nay (đã qua hơn tuần nên chắc chắn sang năm sau).
            LocalDate next = nextYearlyOccurrence(event.getEventDate(), today);
            log.info("[YearlyScheduler] Doi event id={} '{}': {} -> {}",
                event.getId(), event.getTitle(), event.getEventDate(), next);
            event.setEventDate(next);
            eventRepo.save(event);
        }

        if (!stale.isEmpty()) {
            log.info("[YearlyScheduler] Da doi {} su kien lap hang nam sang nam sau", stale.size());
        }
    }

    /**
     * Cùng ngày/tháng với [stored] nhưng ở năm nhỏ nhất mà kết quả không sớm hơn
     * [today]. Luôn cộng từ ngày gốc (không cộng dồn từ ngày đã kẹp) để ngày
     * 29/2 trở lại đúng 29/2 ở năm nhuận kế tiếp.
     */
    public static LocalDate nextYearlyOccurrence(LocalDate stored, LocalDate today) {
        long years = Math.max(0, today.getYear() - stored.getYear());
        LocalDate candidate = stored.plusYears(years);
        while (candidate.isBefore(today)) {
            years++;
            candidate = stored.plusYears(years);
        }
        return candidate;
    }
}
