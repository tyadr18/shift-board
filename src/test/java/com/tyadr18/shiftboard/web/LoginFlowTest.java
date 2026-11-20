package com.tyadr18.shiftboard.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ログイン周りのテスト。
 * ユーザー名・パスワードをテスト用の値で上書きし、「設定値でログインできる」「デモ用の初期値は使えなくなる」ことを確認する。
 */
@SpringBootTest(properties = {
        "app.login.username=tester",
        "app.login.password=test-pass",
        "app.demo-data.enabled=false"
})
@AutoConfigureMockMvc
class LoginFlowTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("未ログインで一覧を開くとログイン画面へリダイレクトされる")
    void redirectsToLoginWhenAnonymous() throws Exception {
        mvc.perform(get("/shifts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("ログイン画面は未ログインでも表示でき、デモ用パスワード以外ならヒントは出ない")
    void showsLoginPage() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("デモ用の初期設定"))));
    }

    @Test
    @DisplayName("CSS は未ログインでも取得できる")
    void staticResourcesArePublic() throws Exception {
        mvc.perform(get("/css/style.css"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("設定したユーザー名・パスワードでログインすると一覧へ移動する")
    void loginSucceeds() throws Exception {
        mvc.perform(formLogin().user("tester").password("test-pass"))
                .andExpect(authenticated().withUsername("tester"))
                .andExpect(redirectedUrl("/shifts"));
    }

    @Test
    @DisplayName("パスワードを間違えるとエラー付きでログイン画面に戻る")
    void loginFailsWithWrongPassword() throws Exception {
        mvc.perform(formLogin().user("tester").password("wrong-pass"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("パスワードを設定した場合、デモ用の初期パスワードではログインできない")
    void demoPasswordIsRejectedWhenOverridden() throws Exception {
        mvc.perform(formLogin().user("tester").password("demo-pass"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("エラー表示付きのログイン画面")
    void showsErrorMessage() throws Exception {
        mvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ユーザー名またはパスワードが正しくありません")));
    }

    @Test
    @DisplayName("ログアウトするとログイン画面に戻る")
    void logoutRedirectsToLogin() throws Exception {
        mvc.perform(logout())
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?logout"));
    }
}
