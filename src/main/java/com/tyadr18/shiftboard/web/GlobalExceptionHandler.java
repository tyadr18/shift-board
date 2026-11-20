package com.tyadr18.shiftboard.web;

import com.tyadr18.shiftboard.service.ShiftRequestNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

/**
 * 画面向けの例外をまとめて扱う。
 * ここで拾わない想定外の例外は Spring Boot の /error（templates/error.html）が 500 として表示する。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    static final String ERROR_VIEW = "error";

    @ExceptionHandler(ShiftRequestNotFoundException.class)
    public ModelAndView handleNotFound(ShiftRequestNotFoundException e) {
        log.info("存在しないシフト希望へのアクセス: id={}", e.getId());
        return errorPage(HttpStatus.NOT_FOUND, "指定されたシフト希望は見つかりませんでした。すでに削除された可能性があります。");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.info("不正なパラメータ: name={}, value={}", e.getName(), e.getValue());
        return errorPage(HttpStatus.BAD_REQUEST, "URL の指定が正しくありません。");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDataIntegrity(DataIntegrityViolationException e) {
        // 事前チェックをすり抜けた同時登録など。DB の一意制約で最終的に防ぐ
        log.warn("データの整合性エラー", e);
        return errorPage(HttpStatus.CONFLICT, "ほかの操作と重なったため保存できませんでした。一覧を確認してからやり直してください。");
    }

    private static ModelAndView errorPage(HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView(ERROR_VIEW, status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", message);
        return mav;
    }
}
