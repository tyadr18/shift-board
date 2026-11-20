package com.tyadr18.shiftboard.service;

/**
 * 入力の形式は正しいが、業務ルールに反しているとき（過去日・重複など）。
 * どの入力項目の問題かを field に持たせ、画面ではその項目の横にメッセージを出す。
 */
public class ShiftRuleViolationException extends RuntimeException {

    private final String field;

    public ShiftRuleViolationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
