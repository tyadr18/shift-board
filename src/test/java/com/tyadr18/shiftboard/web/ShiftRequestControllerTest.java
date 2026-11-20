package com.tyadr18.shiftboard.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tyadr18.shiftboard.domain.ShiftRequest;
import com.tyadr18.shiftboard.domain.TimeSlot;
import com.tyadr18.shiftboard.repository.ShiftRequestRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 画面（コントローラー＋テンプレート＋例外ハンドリング）のテスト。ログイン済みユーザーとして操作する。
 */
@SpringBootTest(properties = {
        "app.login.username=tester",
        "app.login.password=test-pass",
        "app.demo-data.enabled=false"
})
@AutoConfigureMockMvc
@WithMockUser(username = "tester")
class ShiftRequestControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ShiftRequestRepository repository;

    @Autowired
    Clock clock;

    LocalDate today;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        today = LocalDate.now(clock);
    }

    private ShiftRequest saveShift(String staff, LocalDate date, TimeSlot slot) {
        return repository.save(new ShiftRequest(staff, date, slot, null, LocalDateTime.now(clock)));
    }

    @Nested
    @DisplayName("一覧")
    @WithMockUser(username = "tester")
    class ListPage {

        @Test
        @DisplayName("登録済みの希望が表示される")
        void showsShifts() throws Exception {
            saveShift("スタッフA", today.plusDays(1), TimeSlot.EARLY);
            saveShift("スタッフB", today.plusDays(2), TimeSlot.LATE);

            mvc.perform(get("/shifts"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(ShiftRequestController.LIST_VIEW))
                    .andExpect(model().attribute("shifts", hasSize(2)))
                    .andExpect(content().string(containsString("スタッフA")))
                    .andExpect(content().string(containsString("ログイン中")));
        }

        @Test
        @DisplayName("トップページは一覧へリダイレクトする")
        void rootRedirectsToList() throws Exception {
            mvc.perform(get("/"))
                    .andExpect(redirectedUrl("/shifts"));
        }

        @Test
        @DisplayName("スタッフ名と期間で絞り込める")
        void filters() throws Exception {
            saveShift("スタッフA", today.plusDays(1), TimeSlot.EARLY);
            saveShift("スタッフB", today.plusDays(1), TimeSlot.LATE);
            saveShift("スタッフB", today.plusDays(5), TimeSlot.LATE);

            mvc.perform(get("/shifts")
                            .param("staffName", "フB")
                            .param("from", today.toString())
                            .param("to", today.plusDays(2).toString()))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("shifts", hasSize(1)));
        }

        @Test
        @DisplayName("開始日が終了日より後ならエラーを表示し、検索しない")
        void rejectsReversedPeriod() throws Exception {
            saveShift("スタッフA", today.plusDays(1), TimeSlot.EARLY);

            mvc.perform(get("/shifts")
                            .param("from", today.plusDays(3).toString())
                            .param("to", today.toString()))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("search", "periodValid"))
                    .andExpect(model().attribute("shifts", empty()))
                    .andExpect(content().string(containsString("期間の開始日は終了日以前にしてください")));
        }

        @Test
        @DisplayName("日付の形式が不正ならエラーメッセージを表示する")
        void rejectsMalformedDate() throws Exception {
            mvc.perform(get("/shifts").param("from", "2026/12/01"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("search", "from"))
                    .andExpect(content().string(containsString("日付は yyyy-MM-dd の形式で入力してください")));
        }
    }

    @Nested
    @DisplayName("登録")
    @WithMockUser(username = "tester")
    class Create {

        @Test
        @DisplayName("登録画面を表示できる")
        void showsForm() throws Exception {
            mvc.perform(get("/shifts/new"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(ShiftRequestController.FORM_VIEW))
                    .andExpect(content().string(containsString("早番（09:00〜13:00）")));
        }

        @Test
        @DisplayName("正しい入力なら保存して一覧へリダイレクトする")
        void createsShift() throws Exception {
            mvc.perform(post("/shifts").with(csrf())
                            .param("staffName", "スタッフA")
                            .param("workDate", today.plusDays(1).toString())
                            .param("timeSlot", "EARLY")
                            .param("note", "午前のみ希望"))
                    .andExpect(redirectedUrl("/shifts"))
                    .andExpect(flash().attribute("message", "シフト希望を登録しました"));

            assertThat(repository.findAll())
                    .singleElement()
                    .satisfies(s -> {
                        assertThat(s.getStaffName()).isEqualTo("スタッフA");
                        assertThat(s.getNote()).isEqualTo("午前のみ希望");
                    });
        }

        @Test
        @DisplayName("必須項目が空なら入力画面にエラーを表示し、保存しない")
        void rejectsMissingRequiredFields() throws Exception {
            mvc.perform(post("/shifts").with(csrf())
                            .param("staffName", " ")
                            .param("workDate", "")
                            .param("timeSlot", ""))
                    .andExpect(status().isOk())
                    .andExpect(view().name(ShiftRequestController.FORM_VIEW))
                    .andExpect(model().attributeHasFieldErrors("form", "staffName", "workDate", "timeSlot"))
                    .andExpect(content().string(containsString("スタッフ名を入力してください")));

            assertThat(repository.count()).isZero();
        }

        @Test
        @DisplayName("文字数の上限を超えるとエラーになる")
        void rejectsTooLongValues() throws Exception {
            mvc.perform(post("/shifts").with(csrf())
                            .param("staffName", "あ".repeat(ShiftRequest.STAFF_NAME_MAX + 1))
                            .param("workDate", today.plusDays(1).toString())
                            .param("timeSlot", "EARLY")
                            .param("note", "い".repeat(ShiftRequest.NOTE_MAX + 1)))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("form", "staffName", "note"))
                    .andExpect(content().string(containsString("備考は200文字以内で入力してください")));
        }

        @Test
        @DisplayName("存在しない時間帯の値はエラーになる")
        void rejectsUnknownTimeSlot() throws Exception {
            mvc.perform(post("/shifts").with(csrf())
                            .param("staffName", "スタッフA")
                            .param("workDate", today.plusDays(1).toString())
                            .param("timeSlot", "MIDNIGHT"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("form", "timeSlot"))
                    .andExpect(content().string(containsString("時間帯の選択が正しくありません")));
        }

        @Test
        @DisplayName("過去の日付は業務ルール違反として日付欄にエラーを表示する")
        void rejectsPastDate() throws Exception {
            mvc.perform(post("/shifts").with(csrf())
                            .param("staffName", "スタッフA")
                            .param("workDate", today.minusDays(1).toString())
                            .param("timeSlot", "EARLY"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrorCode("form", "workDate", "shiftRule"))
                    .andExpect(content().string(containsString("過去の日付には希望を出せません")));
        }

        @Test
        @DisplayName("重複する希望は時間帯欄にエラーを表示する")
        void rejectsDuplicate() throws Exception {
            saveShift("スタッフA", today.plusDays(1), TimeSlot.EARLY);

            mvc.perform(post("/shifts").with(csrf())
                            .param("staffName", "スタッフA")
                            .param("workDate", today.plusDays(1).toString())
                            .param("timeSlot", "EARLY"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrorCode("form", "timeSlot", "shiftRule"));

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("CSRF トークンのない POST は 403 で拒否する")
        void rejectsWithoutCsrfToken() throws Exception {
            mvc.perform(post("/shifts")
                            .param("staffName", "スタッフA")
                            .param("workDate", today.plusDays(1).toString())
                            .param("timeSlot", "EARLY"))
                    .andExpect(status().isForbidden());

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("編集・削除")
    @WithMockUser(username = "tester")
    class EditAndDelete {

        @Test
        @DisplayName("編集画面に現在の値が入っている")
        void showsEditForm() throws Exception {
            ShiftRequest saved = saveShift("スタッフA", today.plusDays(1), TimeSlot.EARLY);

            mvc.perform(get("/shifts/{id}/edit", saved.getId()))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("shiftId", saved.getId()))
                    .andExpect(content().string(containsString("value=\"スタッフA\"")))
                    .andExpect(content().string(containsString("更新する")));
        }

        @Test
        @DisplayName("更新すると一覧へリダイレクトし、内容が変わる")
        void updatesShift() throws Exception {
            ShiftRequest saved = saveShift("スタッフA", today.plusDays(1), TimeSlot.EARLY);

            mvc.perform(post("/shifts/{id}", saved.getId()).with(csrf())
                            .param("staffName", "スタッフA")
                            .param("workDate", today.plusDays(2).toString())
                            .param("timeSlot", "NIGHT"))
                    .andExpect(redirectedUrl("/shifts"))
                    .andExpect(flash().attribute("message", "シフト希望を更新しました"));

            ShiftRequest updated = repository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getWorkDate()).isEqualTo(today.plusDays(2));
            assertThat(updated.getTimeSlot()).isEqualTo(TimeSlot.NIGHT);
        }

        @Test
        @DisplayName("更新時の入力エラーでは編集画面のまま（更新先 ID を保持）")
        void keepsEditModeOnError() throws Exception {
            ShiftRequest saved = saveShift("スタッフA", today.plusDays(1), TimeSlot.EARLY);

            mvc.perform(post("/shifts/{id}", saved.getId()).with(csrf())
                            .param("staffName", "")
                            .param("workDate", today.plusDays(1).toString())
                            .param("timeSlot", "EARLY"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("shiftId", saved.getId()))
                    .andExpect(content().string(containsString("/shifts/" + saved.getId() + "\"")));
        }

        @Test
        @DisplayName("削除すると一覧へリダイレクトし、データが消える")
        void deletesShift() throws Exception {
            ShiftRequest saved = saveShift("スタッフA", today.plusDays(1), TimeSlot.EARLY);

            mvc.perform(post("/shifts/{id}/delete", saved.getId()).with(csrf()))
                    .andExpect(redirectedUrl("/shifts"))
                    .andExpect(flash().attribute("message", "シフト希望を削除しました"));

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("例外ハンドリング")
    @WithMockUser(username = "tester")
    class Errors {

        @Test
        @DisplayName("存在しない ID の編集画面は 404 のエラー画面")
        void editNotFound() throws Exception {
            mvc.perform(get("/shifts/{id}/edit", 9999))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name(GlobalExceptionHandler.ERROR_VIEW))
                    .andExpect(content().string(containsString("指定されたシフト希望は見つかりませんでした")));
        }

        @Test
        @DisplayName("存在しない ID の削除も 404")
        void deleteNotFound() throws Exception {
            mvc.perform(post("/shifts/{id}/delete", 9999).with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ID が数値でなければ 400 のエラー画面")
        void invalidIdIsBadRequest() throws Exception {
            mvc.perform(get("/shifts/{id}/edit", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("URL の指定が正しくありません")));
        }
    }
}
