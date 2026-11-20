package com.tyadr18.shiftboard.repository;

import com.tyadr18.shiftboard.domain.ShiftRequest;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * 一覧フィルタ用の検索条件。条件ごとに小さく分けて、サービス層で組み合わせる。
 */
public final class ShiftRequestSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private ShiftRequestSpecifications() {
    }

    /** スタッフ名の部分一致（大文字小文字を区別しない）。% や _ は文字として扱う。 */
    public static Specification<ShiftRequest> staffNameContains(String keyword) {
        String pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("staffName")), pattern, LIKE_ESCAPE);
    }

    public static Specification<ShiftRequest> workDateOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("workDate"), from);
    }

    public static Specification<ShiftRequest> workDateOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("workDate"), to);
    }

    static String escapeLike(String value) {
        return value
                .replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }
}
