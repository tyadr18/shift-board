package com.tyadr18.shiftboard.repository;

import com.tyadr18.shiftboard.domain.ShiftRequest;
import com.tyadr18.shiftboard.domain.TimeSlot;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShiftRequestRepository
        extends JpaRepository<ShiftRequest, Long>, JpaSpecificationExecutor<ShiftRequest> {

    boolean existsByStaffNameAndWorkDateAndTimeSlot(String staffName, LocalDate workDate, TimeSlot timeSlot);

    boolean existsByStaffNameAndWorkDateAndTimeSlotAndIdNot(
            String staffName, LocalDate workDate, TimeSlot timeSlot, Long id);
}
