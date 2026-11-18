package com.app.nino.model.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventReminderTest {

    @Test
    void computeTriggerTime_withMinutesBefore_subtractsMinutesFromEventDateTime() {
        EventReminder reminder = EventReminder.builder().remindMinutesBefore(30).build();

        LocalDateTime trigger = reminder.computeTriggerTime(
            LocalDate.of(2026, 9, 7), LocalTime.of(17, 45));

        assertEquals(LocalDateTime.of(2026, 9, 7, 17, 15), trigger);
    }

    @Test
    void computeTriggerTime_mobileThirtyMinutesConvention_daysZeroHoursZero_subtracts30Minutes() {
        // Payload mobile gửi cho "30 phút trước" (sự kiện "a" 2026-11-18).
        EventReminder reminder = EventReminder.builder().remindDaysBefore(0).remindHoursBefore(0).build();

        LocalDateTime trigger = reminder.computeTriggerTime(
            LocalDate.of(2026, 11, 18), LocalTime.of(2, 0));

        assertEquals(LocalDateTime.of(2026, 11, 18, 1, 30), trigger);
    }

    @Test
    void computeTriggerTime_daysZeroOnly_isOnTime() {
        EventReminder reminder = EventReminder.builder().remindDaysBefore(0).build();

        LocalDateTime trigger = reminder.computeTriggerTime(
            LocalDate.of(2026, 11, 18), LocalTime.of(2, 0));

        assertEquals(LocalDateTime.of(2026, 11, 18, 2, 0), trigger);
    }

    @Test
    void computeTriggerTime_daysPositive_subtractsDays() {
        EventReminder reminder = EventReminder.builder().remindDaysBefore(3).build();

        LocalDateTime trigger = reminder.computeTriggerTime(
            LocalDate.of(2026, 11, 18), null);

        assertEquals(LocalDateTime.of(2026, 11, 15, 8, 0), trigger);
    }
}
