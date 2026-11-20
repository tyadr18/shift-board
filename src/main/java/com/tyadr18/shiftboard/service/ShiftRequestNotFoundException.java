package com.tyadr18.shiftboard.service;

/**
 * 指定した ID のシフト希望が存在しないとき。画面では 404 として扱う。
 */
public class ShiftRequestNotFoundException extends RuntimeException {

    private final Long id;

    public ShiftRequestNotFoundException(Long id) {
        super("シフト希望が見つかりません（id=" + id + "）");
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
