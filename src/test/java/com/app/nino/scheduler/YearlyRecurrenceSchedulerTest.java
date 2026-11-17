package com.app.nino.scheduler;

import com.app.nino.model.entity.Event;
import com.app.nino.repository.EventRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YearlyRecurrenceSchedulerTest {

    @Test
    void pastDateRollsToNextYear() {
        assertEquals(LocalDate.of(2027, 10, 10),
            YearlyRecurrenceScheduler.nextYearlyOccurrence(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 11, 17)));
    }

    @Test
    void dateStillAheadThisYearIsKept() {
        assertEquals(LocalDate.of(2026, 12, 12),
            YearlyRecurrenceScheduler.nextYearlyOccurrence(LocalDate.of(2026, 12, 12), LocalDate.of(2026, 11, 17)));
    }

    @Test
    void today_isNotInThePast() {
        assertEquals(LocalDate.of(2026, 11, 17),
            YearlyRecurrenceScheduler.nextYearlyOccurrence(LocalDate.of(2025, 11, 17), LocalDate.of(2026, 11, 17)));
    }

    @Test
    void manyYearsBehindCatchesUp() {
        assertEquals(LocalDate.of(2027, 3, 5),
            YearlyRecurrenceScheduler.nextYearlyOccurrence(LocalDate.of(2020, 3, 5), LocalDate.of(2026, 11, 17)));
    }

    @Test
    void leapDayKeepsFeb29InNextLeapYear() {
        // 29/2/2024 đã qua; năm 2027 không nhuận -> 28/2/2027 là ứng viên đầu
        // tiên không sớm hơn hôm nay khi hôm nay là 2026-11-17.
        assertEquals(LocalDate.of(2027, 2, 28),
            YearlyRecurrenceScheduler.nextYearlyOccurrence(LocalDate.of(2024, 2, 29), LocalDate.of(2026, 11, 17)));
        // Cộng từ ngày gốc nên đến 2028 lại là 29/2.
        assertEquals(LocalDate.of(2028, 2, 29),
            YearlyRecurrenceScheduler.nextYearlyOccurrence(LocalDate.of(2024, 2, 29), LocalDate.of(2027, 3, 1)));
    }

    @Test
    void schedulerSavesRolledEvents() {
        EventRepository repo = mock(EventRepository.class);
        Event event = new Event();
        event.setTitle("Sinh nhật");
        event.setEventDate(LocalDate.of(2020, 1, 1));
        when(repo.findByRecurrenceTypeAndCategory_CodeAndIsActiveTrueAndEventDateBefore(eq(Event.RecurrenceType.YEARLY), eq("SINH_NHAT"), any()))
            .thenReturn(List.of(event));

        new YearlyRecurrenceScheduler(repo).rollForwardYearlyEvents();

        verify(repo).save(event);
        assertEquals(false, event.getEventDate().isBefore(LocalDate.now().minusDays(1)));
    }

    @Test
    void schedulerOnlyQueriesBirthdaysOlderThanOneWeek() {
        EventRepository repo = mock(EventRepository.class);
        when(repo.findByRecurrenceTypeAndCategory_CodeAndIsActiveTrueAndEventDateBefore(any(), any(), any()))
            .thenReturn(List.of());

        new YearlyRecurrenceScheduler(repo).rollForwardYearlyEvents();

        org.mockito.ArgumentCaptor<LocalDate> cutoff = org.mockito.ArgumentCaptor.forClass(LocalDate.class);
        verify(repo).findByRecurrenceTypeAndCategory_CodeAndIsActiveTrueAndEventDateBefore(
            eq(Event.RecurrenceType.YEARLY), eq("SINH_NHAT"), cutoff.capture());
        // mốc = hôm nay - 7 ngày (cho phép lệch 1 ngày vì múi giờ)
        long diff = java.time.temporal.ChronoUnit.DAYS.between(cutoff.getValue(), LocalDate.now());
        org.junit.jupiter.api.Assertions.assertTrue(diff >= 6 && diff <= 8, "cutoff diff=" + diff);
    }

    @Test
    void schedulerDoesNothingWhenNothingStale() {
        EventRepository repo = mock(EventRepository.class);
        when(repo.findByRecurrenceTypeAndCategory_CodeAndIsActiveTrueAndEventDateBefore(any(), any(), any())).thenReturn(List.of());

        new YearlyRecurrenceScheduler(repo).rollForwardYearlyEvents();

        verify(repo, never()).save(any());
    }
}
