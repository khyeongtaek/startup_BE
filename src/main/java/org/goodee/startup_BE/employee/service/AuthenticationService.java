package org.goodee.startup_BE.employee.service;


import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.Map;

public interface AuthenticationService {
    APIResponseDTO<EmployeeResponseDTO> signup(Authentication authentication, EmployeeRequestDTO request);

    Map<String, Object> login(EmployeeRequestDTO request, String ipAddress, String userAgent);

    Map<String, Object> refreshToken(String refreshToken);
}
