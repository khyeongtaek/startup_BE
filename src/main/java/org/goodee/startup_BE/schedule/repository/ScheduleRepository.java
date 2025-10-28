package org.goodee.startup_BE.schedule.repository;

import org.goodee.startup_BE.schedule.dto.ScheduleResponseDTO;
import org.goodee.startup_BE.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // 삭제 되지않은 전체 일정 조회
    List<Schedule> findByIsDeletedFalse();

    // 기간별 일정 조회
    List<Schedule> findByStartTimeBetweenAndIsDeletedFalse(LocalDateTime startTimeAfter, LocalDateTime startTimeBefore);
}
