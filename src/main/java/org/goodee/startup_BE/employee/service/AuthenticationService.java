package org.goodee.startup_BE.employee.service;


import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.Map;

public interface AuthenticationService {
    APIResponseDTO<EmployeeResponseDTO> signup(Authentication authentication, EmployeeRequestDTO request);

    Map<String, Object> login(EmployeeRequestDTO request, String ipAddress, String userAgent);

    // 사용자 비밀번호 변경
    EmployeeResponseDTO updateEmployeePassword(String username, EmployeeRequestDTO request);

    Map<String, Object> refreshToken(String refreshToken);
}
