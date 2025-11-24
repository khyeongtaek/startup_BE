package org.goodee.startup_BE.attendance.service;

import org.goodee.startup_BE.attendance.entity.AnnualLeave;

import java.util.List;

public interface AnnualLeaveService {

    // 직원 연차 정보 조회
    AnnualLeave getAnnualLeave(Long employeeId);

    // 직원 연차 생성 (없을 경우)
    AnnualLeave createIfNotExists(Long employeeId);

    // 연차 사용
    AnnualLeave useAnnualLeave(Long employeeId, Double days);

    // 연차 환원
    AnnualLeave refundAnnualLeave(Long employeeId, Double days);

    // 전체 연차 조회 (관리자용)
    List<AnnualLeave> getAllAnnualLeaves();

}