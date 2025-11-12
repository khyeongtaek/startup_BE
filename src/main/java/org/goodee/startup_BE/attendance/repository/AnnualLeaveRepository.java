package org.goodee.startup_BE.attendance.repository;

import org.goodee.startup_BE.attendance.entity.AnnualLeave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnnualLeaveRepository extends JpaRepository<AnnualLeave, Long> {

    // 직원 ID와 연도를 기준으로 연차 정보 조회
    Optional<AnnualLeave> findByEmployeeEmployeeIdAndYear(Long employeeId, Long year);

    Optional<AnnualLeave> findByEmployeeEmployeeId(Long employeeEmployeeId);
}