package com.tyadr18.shiftboard.service;

import com.tyadr18.shiftboard.domain.TimeSlot;
import java.time.LocalDate;

/**
 * 登録・更新時にサービス層へ渡す入力値。画面のフォームとは切り離しておく。
 */
public record ShiftRequestCommand(
        String staffName,
        LocalDate workDate,
        TimeSlot timeSlot,
        String note) {
}
