package com.app.nino.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FcmServiceChannelTest {

    @Test
    void channelFollowsSoundAndVibrateSettings() {
        assertEquals("nino_event_reminders", FcmService.channelIdFor(true, true));
        assertEquals("nino_event_reminders_sound", FcmService.channelIdFor(true, false));
        assertEquals("nino_event_reminders_vibrate", FcmService.channelIdFor(false, true));
        assertEquals("nino_event_reminders_silent", FcmService.channelIdFor(false, false));
    }
}
