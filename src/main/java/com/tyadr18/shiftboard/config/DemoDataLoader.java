package com.tyadr18.shiftboard.config;

import com.tyadr18.shiftboard.domain.TimeSlot;
import com.tyadr18.shiftboard.repository.ShiftRequestRepository;
import com.tyadr18.shiftboard.service.ShiftRequestCommand;
import com.tyadr18.shiftboard.service.ShiftRequestService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 起動直後に画面を確認しやすいよう、架空のサンプルデータを投入する。
 * 日付は起動日からの相対日付にしているので、いつ起動しても「未来の希望」として表示される。
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataLoader implements CommandLineRunner {

    private final ShiftRequestRepository repository;
    private final ShiftRequestService service;
    private final Clock clock;

    public DemoDataLoader(ShiftRequestRepository repository, ShiftRequestService service, Clock clock) {
        this.repository = repository;
        this.service = service;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        service.create(new ShiftRequestCommand("スタッフA", today.plusDays(1), TimeSlot.EARLY, null));
        service.create(new ShiftRequestCommand("スタッフB", today.plusDays(1), TimeSlot.LATE, "17時で上がりたいです"));
        service.create(new ShiftRequestCommand("スタッフC", today.plusDays(2), TimeSlot.NIGHT, null));
        service.create(new ShiftRequestCommand("スタッフA", today.plusDays(3), TimeSlot.FULL, "通しでも大丈夫です"));
    }
}
