package com.tyadr18.shiftboard.web;

import com.tyadr18.shiftboard.domain.ShiftRequest;
import com.tyadr18.shiftboard.service.ShiftSearchCriteria;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 一覧画面の絞り込みフォーム（GET パラメータ）。
 */
public class ShiftSearchForm {

    @Size(max = ShiftRequest.STAFF_NAME_MAX, message = "スタッフ名は{max}文字以内で入力してください")
    private String staffName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    @AssertTrue(message = "期間の開始日は終了日以前にしてください")
    public boolean isPeriodValid() {
        return from == null || to == null || !from.isAfter(to);
    }

    public ShiftSearchCriteria toCriteria() {
        return new ShiftSearchCriteria(staffName, from, to);
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }
}
