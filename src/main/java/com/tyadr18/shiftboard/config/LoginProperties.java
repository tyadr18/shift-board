package com.tyadr18.shiftboard.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * ログインユーザーの設定。
 * application.properties で環境変数 APP_USERNAME / APP_PASSWORD から読み込む。
 */
@Validated
@ConfigurationProperties(prefix = "app.login")
public record LoginProperties(
        @NotBlank String username,
        @NotBlank String password) {

    /** 環境変数を設定しなかったときに使われるデモ用パスワード。 */
    public static final String DEMO_PASSWORD = "demo-pass";

    public boolean usesDemoPassword() {
        return DEMO_PASSWORD.equals(password);
    }
}
