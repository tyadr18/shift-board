package com.tyadr18.shiftboard.domain;

/**
 * シフトの時間帯。宣言順が一覧での並び順になる。
 */
public enum TimeSlot {

    EARLY("早番", "09:00〜13:00"),
    LATE("遅番", "13:00〜18:00"),
    NIGHT("夜番", "18:00〜22:00"),
    FULL("通し", "09:00〜18:00");

    private final String label;
    private final String hours;

    TimeSlot(String label, String hours) {
        this.label = label;
        this.hours = hours;
    }

    public String getLabel() {
        return label;
    }

    public String getHours() {
        return hours;
    }

    /** 画面表示用。例:「早番（09:00〜13:00）」 */
    public String getDisplayName() {
        return label + "（" + hours + "）";
    }
}
