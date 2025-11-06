package org.goodee.startup_BE.schedule.repository;

import org.goodee.startup_BE.schedule.entity.ScheduleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {
    List<ScheduleParticipant> findByScheduleScheduleId(Long scheduleScheduleId);

    Optional<ScheduleParticipant> findBySchedule_ScheduleIdAndParticipant_EmployeeId(Long scheduleId, Long employeeId);
}
