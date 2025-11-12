package org.goodee.startup_BE.employee.service;

import org.goodee.startup_BE.employee.dto.EmployeeHistoryResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeHistoryService {

    /**
     * 특정 직원의 변경 이력을 최근순으로 조회 (페이징)
     * @param employeeId 대상 직원의 ID
     * @param pageable 페이징 및 정렬 정보
     * @return 변경 이력 DTO 페이지
     */
    Page<EmployeeHistoryResponseDTO> getEmployeeHistories(Long employeeId, Pageable pageable);

    /**
     * 직원 정보 변경 이력을 기록
     * @param employee 변경 대상 직원
     * @param updater 변경 수행자
     * @param fieldName 변경된 필드명
     * @param oldValue 변경 전 값
     * @param newValue 변경 후 값
     */
    void logHistory(Employee employee, Employee updater, String fieldName, String oldValue, String newValue);
}