-- ============================================================
-- V38_20261117_add_device_notify_preferences.sql
-- Cai dat am thanh / rung cua thong bao, luu THEO TUNG THIET BI.
-- Tren Android 8+ am thanh va rung do "kenh thong bao" (channel) quyet dinh,
-- app khong the doi sau khi push da toi may — nen backend phai biet cai dat
-- cua tung thiet bi de chon dung channel khi gui (xem FcmService).
-- Mac dinh bat ca hai (giu nguyen hanh vi cu).
-- ============================================================

ALTER TABLE `user_devices`
    ADD COLUMN `notify_sound`   TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Phat chuong thong bao',
    ADD COLUMN `notify_vibrate` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Rung khi co thong bao';
