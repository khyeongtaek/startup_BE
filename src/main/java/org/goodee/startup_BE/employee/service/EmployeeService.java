package org.goodee.startup_BE.employee.service;

import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;

import java.util.List;

public interface EmployeeService {
    // 사용자 개인정보 변경
    EmployeeResponseDTO updateEmployeeByUser(String username, EmployeeRequestDTO request);

    //관리자의 사원 인사관리
    EmployeeResponseDTO updateEmployeeByAdmin(String username, EmployeeRequestDTO request);

    EmployeeResponseDTO initPassword(String username, EmployeeRequestDTO request);

    EmployeeResponseDTO getEmployee(Long employeeId);
    EmployeeResponseDTO getEmployee(String username);

    // 특정 부서의 소속원들 조회
    List<EmployeeResponseDTO> getDepartmentMembers(Long departmentId);

    void deleteEmployee(Long employeeId);


}
