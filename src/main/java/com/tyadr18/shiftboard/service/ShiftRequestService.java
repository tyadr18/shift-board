package com.tyadr18.shiftboard.service;

import static com.tyadr18.shiftboard.repository.ShiftRequestSpecifications.staffNameContains;
import static com.tyadr18.shiftboard.repository.ShiftRequestSpecifications.workDateOnOrAfter;
import static com.tyadr18.shiftboard.repository.ShiftRequestSpecifications.workDateOnOrBefore;

import com.tyadr18.shiftboard.domain.ShiftRequest;
import com.tyadr18.shiftboard.repository.ShiftRequestRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ShiftRequestService {

    /** 日付 → 時間帯（早番から順）→ スタッフ名 の順で並べる。 */
    static final Comparator<ShiftRequest> DISPLAY_ORDER = Comparator
            .comparing(ShiftRequest::getWorkDate)
            .thenComparing(ShiftRequest::getTimeSlot)
            .thenComparing(ShiftRequest::getStaffName);

    private final ShiftRequestRepository repository;
    private final Clock clock;

    public ShiftRequestService(ShiftRequestRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<ShiftRequest> search(ShiftSearchCriteria criteria) {
        List<Specification<ShiftRequest>> conditions = new ArrayList<>();
        String staffName = normalize(criteria.staffName());
        if (staffName != null) {
            conditions.add(staffNameContains(staffName));
        }
        if (criteria.from() != null) {
            conditions.add(workDateOnOrAfter(criteria.from()));
        }
        if (criteria.to() != null) {
            conditions.add(workDateOnOrBefore(criteria.to()));
        }
        return repository.findAll(Specification.allOf(conditions)).stream()
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    public ShiftRequest get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ShiftRequestNotFoundException(id));
    }

    @Transactional
    public ShiftRequest create(ShiftRequestCommand command) {
        ShiftRequestCommand normalized = normalize(command);
        validateWorkDate(normalized.workDate());
        if (repository.existsByStaffNameAndWorkDateAndTimeSlot(
                normalized.staffName(), normalized.workDate(), normalized.timeSlot())) {
            throw duplicated();
        }
        ShiftRequest shiftRequest = new ShiftRequest(
                normalized.staffName(), normalized.workDate(), normalized.timeSlot(), normalized.note(), now());
        return repository.save(shiftRequest);
    }

    @Transactional
    public ShiftRequest update(Long id, ShiftRequestCommand command) {
        ShiftRequest shiftRequest = get(id);
        ShiftRequestCommand normalized = normalize(command);
        validateWorkDate(normalized.workDate());
        if (repository.existsByStaffNameAndWorkDateAndTimeSlotAndIdNot(
                normalized.staffName(), normalized.workDate(), normalized.timeSlot(), id)) {
            throw duplicated();
        }
        shiftRequest.update(
                normalized.staffName(), normalized.workDate(), normalized.timeSlot(), normalized.note(), now());
        return shiftRequest;
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(get(id));
    }

    private void validateWorkDate(LocalDate workDate) {
        if (workDate.isBefore(LocalDate.now(clock))) {
            throw new ShiftRuleViolationException("workDate", "過去の日付には希望を出せません");
        }
    }

    private static ShiftRuleViolationException duplicated() {
        return new ShiftRuleViolationException("timeSlot", "同じ日・同じ時間帯の希望がすでに登録されています");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** 前後の空白を取り除き、空文字は null にそろえる。 */
    private static ShiftRequestCommand normalize(ShiftRequestCommand command) {
        ShiftRequestCommand normalized = new ShiftRequestCommand(
                normalize(command.staffName()), command.workDate(), command.timeSlot(), normalize(command.note()));
        // 画面からの入力はフォームで検証済み。ここに来るのはプログラムの誤りなので即座に失敗させる
        Objects.requireNonNull(normalized.staffName(), "staffName is required");
        Objects.requireNonNull(normalized.workDate(), "workDate is required");
        Objects.requireNonNull(normalized.timeSlot(), "timeSlot is required");
        return normalized;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
