package com.tyadr18.shiftboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * スタッフ1人が「この日のこの時間帯に入りたい」と出したシフト希望。
 * 同じスタッフ・同じ日・同じ時間帯の希望は1件まで（DBの一意制約でも保証する）。
 */
@Entity
@Table(name = "shift_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shift_requests_staff_date_slot",
                columnNames = {"staff_name", "work_date", "time_slot"}))
public class ShiftRequest {

    public static final int STAFF_NAME_MAX = 50;
    public static final int NOTE_MAX = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_name", nullable = false, length = STAFF_NAME_MAX)
    private String staffName;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_slot", nullable = false, length = 10)
    private TimeSlot timeSlot;

    @Column(name = "note", length = NOTE_MAX)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ShiftRequest() {
        // JPA 用
    }

    public ShiftRequest(String staffName, LocalDate workDate, TimeSlot timeSlot, String note, LocalDateTime now) {
        this.staffName = staffName;
        this.workDate = workDate;
        this.timeSlot = timeSlot;
        this.note = note;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String staffName, LocalDate workDate, TimeSlot timeSlot, String note, LocalDateTime now) {
        this.staffName = staffName;
        this.workDate = workDate;
        this.timeSlot = timeSlot;
        this.note = note;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getStaffName() {
        return staffName;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
