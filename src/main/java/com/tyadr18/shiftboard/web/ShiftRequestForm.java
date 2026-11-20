package com.tyadr18.shiftboard.web;

import com.tyadr18.shiftboard.domain.ShiftRequest;
import com.tyadr18.shiftboard.domain.TimeSlot;
import com.tyadr18.shiftboard.service.ShiftRequestCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 登録・編集画面の入力フォーム。形式チェックはここ、業務ルールはサービス層で行う。
 */
public class ShiftRequestForm {

    @NotBlank(message = "スタッフ名を入力してください")
    @Size(max = ShiftRequest.STAFF_NAME_MAX, message = "スタッフ名は{max}文字以内で入力してください")
    private String staffName;

    @NotNull(message = "日付を入力してください")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate workDate;

    @NotNull(message = "時間帯を選択してください")
    private TimeSlot timeSlot;

    @Size(max = ShiftRequest.NOTE_MAX, message = "備考は{max}文字以内で入力してください")
    private String note;

    public static ShiftRequestForm from(ShiftRequest shiftRequest) {
        ShiftRequestForm form = new ShiftRequestForm();
        form.setStaffName(shiftRequest.getStaffName());
        form.setWorkDate(shiftRequest.getWorkDate());
        form.setTimeSlot(shiftRequest.getTimeSlot());
        form.setNote(shiftRequest.getNote());
        return form;
    }

    public ShiftRequestCommand toCommand() {
        return new ShiftRequestCommand(staffName, workDate, timeSlot, note);
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
