package com.tyadr18.shiftboard.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 「今日」の判定に使う時計。テストでは固定時計に差し替える。
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Asia/Tokyo"));
    }
}
