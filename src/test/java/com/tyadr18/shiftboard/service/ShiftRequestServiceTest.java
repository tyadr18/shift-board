package com.tyadr18.shiftboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.tyadr18.shiftboard.domain.ShiftRequest;
import com.tyadr18.shiftboard.domain.TimeSlot;
import com.tyadr18.shiftboard.repository.ShiftRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * サービス層のテスト。H2 上で実際にクエリを流し、検索条件や一意性チェックまで確認する。
 * 「今日」は 2026-12-01（日本時間）に固定する。
 */
@DataJpaTest
@Import({ShiftRequestService.class, ShiftRequestServiceTest.FixedClockConfig.class})
class ShiftRequestServiceTest {

    static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    static final LocalDate TODAY = LocalDate.of(2026, 12, 1);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-12-01T00:00:00Z"), TOKYO); // 日本時間 09:00
        }
    }

    @Autowired
    ShiftRequestService service;

    @Autowired
    ShiftRequestRepository repository;

    private static ShiftRequestCommand command(String staff, LocalDate date, TimeSlot slot, String note) {
        return new ShiftRequestCommand(staff, date, slot, note);
    }

    @Nested
    @DisplayName("登録")
    class Create {

        @Test
        @DisplayName("前後の空白を取り除き、空の備考は null にして保存する")
        void savesNormalizedValues() {
            ShiftRequest saved = service.create(command("  スタッフA  ", TODAY.plusDays(1), TimeSlot.EARLY, "   "));

            ShiftRequest found = repository.findById(saved.getId()).orElseThrow();
            assertThat(found.getStaffName()).isEqualTo("スタッフA");
            assertThat(found.getNote()).isNull();
            assertThat(found.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 12, 1, 9, 0));
            assertThat(found.getUpdatedAt()).isEqualTo(found.getCreatedAt());
        }

        @Test
        @DisplayName("今日の日付は登録できる")
        void acceptsToday() {
            ShiftRequest saved = service.create(command("スタッフA", TODAY, TimeSlot.NIGHT, null));

            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("過去の日付は workDate のルール違反になる")
        void rejectsPastDate() {
            assertThatThrownBy(() -> service.create(command("スタッフA", TODAY.minusDays(1), TimeSlot.EARLY, null)))
                    .isInstanceOf(ShiftRuleViolationException.class)
                    .hasMessageContaining("過去の日付")
                    .extracting("field").isEqualTo("workDate");
            assertThat(repository.count()).isZero();
        }

        @Test
        @DisplayName("同じスタッフ・日付・時間帯の重複は timeSlot のルール違反になる")
        void rejectsDuplicate() {
            service.create(command("スタッフA", TODAY.plusDays(1), TimeSlot.EARLY, null));

            assertThatThrownBy(() -> service.create(command(" スタッフA ", TODAY.plusDays(1), TimeSlot.EARLY, "再送")))
                    .isInstanceOf(ShiftRuleViolationException.class)
                    .extracting("field").isEqualTo("timeSlot");
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("時間帯が違えば同じ日に複数登録できる")
        void allowsDifferentSlotsOnSameDay() {
            service.create(command("スタッフA", TODAY.plusDays(1), TimeSlot.EARLY, null));
            service.create(command("スタッフA", TODAY.plusDays(1), TimeSlot.NIGHT, null));

            assertThat(repository.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("更新")
    class Update {

        @Test
        @DisplayName("内容と更新日時が変わる")
        void updatesFields() {
            ShiftRequest saved = service.create(command("スタッフA", TODAY.plusDays(1), TimeSlot.EARLY, null));

            service.update(saved.getId(), command("スタッフA", TODAY.plusDays(2), TimeSlot.LATE, "遅番に変更"));
            repository.flush();

            ShiftRequest found = repository.findById(saved.getId()).orElseThrow();
            assertThat(found.getWorkDate()).isEqualTo(TODAY.plusDays(2));
            assertThat(found.getTimeSlot()).isEqualTo(TimeSlot.LATE);
            assertThat(found.getNote()).isEqualTo("遅番に変更");
        }

        @Test
        @DisplayName("自分自身と同じ内容のまま保存しても重複扱いにしない")
        void sameValuesIsNotDuplicate() {
            ShiftRequest saved = service.create(command("スタッフA", TODAY.plusDays(1), TimeSlot.EARLY, null));

            service.update(saved.getId(), command("スタッフA", TODAY.plusDays(1), TimeSlot.EARLY, "備考だけ追加"));

            assertThat(service.get(saved.getId()).getNote()).isEqualTo("備考だけ追加");
        }

        @Test
        @DisplayName("ほかの希望と重なる内容には変更できない")
        void rejectsDuplicateWithOther() {
            service.create(command("スタッフA", TODAY.plusDays(1), TimeSlot.EARLY, null));
            ShiftRequest other = service.create(command("スタッフA", TODAY.plusDays(1), TimeSlot.LATE, null));

            assertThatThrownBy(() -> service.update(other.getId(),
                    command("スタッフA", TODAY.plusDays(1), TimeSlot.EARLY, null)))
                    .isInstanceOf(ShiftRuleViolationException.class)
                    .extracting("field").isEqualTo("timeSlot");
        }

        @Test
        @DisplayName("存在しない ID は ShiftRequestNotFoundException")
        void notFound() {
            assertThatThrownBy(() -> service.update(999L, command("スタッフA", TODAY, TimeSlot.EARLY, null)))
                    .isInstanceOf(ShiftRequestNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("削除")
    class Delete {

        @Test
        @DisplayName("削除すると取得できなくなる")
        void deletes() {
            ShiftRequest saved = service.create(command("スタッフA", TODAY.plusDays(1), TimeSlot.EARLY, null));

            service.delete(saved.getId());

            assertThat(repository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("存在しない ID は ShiftRequestNotFoundException")
        void notFound() {
            assertThatThrownBy(() -> service.delete(999L))
                    .isInstanceOf(ShiftRequestNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("一覧・絞り込み")
    class Search {

        private void seed() {
            service.create(command("スタッフB", TODAY.plusDays(2), TimeSlot.EARLY, null));
            service.create(command("スタッフA", TODAY.plusDays(1), TimeSlot.NIGHT, null));
            service.create(command("スタッフA", TODAY.plusDays(1), TimeSlot.EARLY, null));
            service.create(command("Staff_X", TODAY.plusDays(3), TimeSlot.FULL, null));
        }

        @Test
        @DisplayName("条件なしなら全件を 日付→時間帯→スタッフ名 の順で返す")
        void returnsAllInDisplayOrder() {
            seed();

            List<ShiftRequest> result = service.search(ShiftSearchCriteria.none());

            assertThat(result)
                    .extracting(ShiftRequest::getWorkDate, ShiftRequest::getTimeSlot, ShiftRequest::getStaffName)
                    .containsExactly(
                            tuple(TODAY.plusDays(1), TimeSlot.EARLY, "スタッフA"),
                            tuple(TODAY.plusDays(1), TimeSlot.NIGHT, "スタッフA"),
                            tuple(TODAY.plusDays(2), TimeSlot.EARLY, "スタッフB"),
                            tuple(TODAY.plusDays(3), TimeSlot.FULL, "Staff_X"));
        }

        @Test
        @DisplayName("スタッフ名は部分一致・大文字小文字を区別しない")
        void filtersByStaffName() {
            seed();

            assertThat(service.search(new ShiftSearchCriteria("フa", null, null)))
                    .extracting(ShiftRequest::getStaffName)
                    .containsOnly("スタッフA")
                    .hasSize(2);
            assertThat(service.search(new ShiftSearchCriteria("staff", null, null)))
                    .extracting(ShiftRequest::getStaffName)
                    .containsExactly("Staff_X");
        }

        @Test
        @DisplayName("% や _ はワイルドカードではなく文字として検索する")
        void escapesLikeWildcards() {
            seed();

            assertThat(service.search(new ShiftSearchCriteria("_", null, null)))
                    .extracting(ShiftRequest::getStaffName)
                    .containsExactly("Staff_X");
            assertThat(service.search(new ShiftSearchCriteria("%", null, null))).isEmpty();
        }

        @Test
        @DisplayName("期間の開始日・終了日はどちらもその日を含む")
        void filtersByPeriodInclusive() {
            seed();

            assertThat(service.search(new ShiftSearchCriteria(null, TODAY.plusDays(2), TODAY.plusDays(3))))
                    .extracting(ShiftRequest::getWorkDate)
                    .containsExactly(TODAY.plusDays(2), TODAY.plusDays(3));
            assertThat(service.search(new ShiftSearchCriteria(null, null, TODAY.plusDays(1))))
                    .hasSize(2);
        }

        @Test
        @DisplayName("スタッフ名と期間を組み合わせられる。空白だけのスタッフ名は条件にしない")
        void combinesConditions() {
            seed();

            assertThat(service.search(new ShiftSearchCriteria("スタッフ", TODAY.plusDays(2), null)))
                    .extracting(ShiftRequest::getStaffName)
                    .containsExactly("スタッフB");
            assertThat(service.search(new ShiftSearchCriteria("   ", null, null))).hasSize(4);
        }
    }
}
