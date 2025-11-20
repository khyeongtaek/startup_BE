package org.goodee.startup_BE.schedule.repository;

import org.goodee.startup_BE.schedule.dto.ScheduleResponseDTO;
import org.goodee.startup_BE.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {


    // 삭제 되지않은 전체 일정 조회
    List<Schedule> findByIsDeletedFalse();

    // 기간별 일정 조회
    List<Schedule> findByStartTimeBetweenAndIsDeletedFalse(LocalDateTime startTimeAfter, LocalDateTime startTimeBefore);

    // 내가 만든 일정(참여자 fetch join 금지)
    @Query("""
           select s
           from Schedule s
           where s.isDeleted = false
             and s.employee.employeeId = :employeeId
           order by s.startTime asc
           """)
    List<Schedule> findOwnedVisible(@Param("employeeId") Long employeeId);

    // 내가 초대받은 일정(참여자 테이블 기준)
    @Query("""
           select distinct sp.schedule
           from ScheduleParticipant sp
           where sp.is_deleted = false
             and sp.schedule.isDeleted = false
             and sp.participant.employeeId = :employeeId
           order by sp.schedule.startTime asc
           """)
    List<Schedule> findInvitedVisible(@Param("employeeId") Long employeeId);


    // 직원 휴가 체크
    @Query("""
    select count(s) > 0
    from Schedule s
    where s.isDeleted = false
      and s.employee.employeeId = :employeeId
      and s.startTime <= :endOfDay
      and s.endTime >= :startOfDay
      and s.category.value1 in ('VACATION', 'MORNING_HALF', 'AFTERNOON_HALF')
""")
    boolean existsVacationOn(
            @Param("employeeId") Long employeeId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}

