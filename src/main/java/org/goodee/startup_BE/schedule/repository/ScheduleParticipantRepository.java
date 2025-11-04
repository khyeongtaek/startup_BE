package org.goodee.startup_BE.schedule.repository;

import org.goodee.startup_BE.schedule.entity.ScheduleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {
    List<ScheduleParticipant> findByScheduleScheduleId(Long scheduleScheduleId);
}
