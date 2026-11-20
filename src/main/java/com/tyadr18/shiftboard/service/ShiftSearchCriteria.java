package com.tyadr18.shiftboard.service;

import java.time.LocalDate;

/**
 * 一覧の絞り込み条件。null の項目は条件に含めない。
 */
public record ShiftSearchCriteria(
        String staffName,
        LocalDate from,
        LocalDate to) {

    public static ShiftSearchCriteria none() {
        return new ShiftSearchCriteria(null, null, null);
    }
}
