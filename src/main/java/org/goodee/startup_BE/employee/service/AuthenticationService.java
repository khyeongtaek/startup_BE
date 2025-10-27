package org.goodee.startup_BE.employee.service;


import jakarta.servlet.http.HttpServletRequest;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.employee.dto.AuthenticationResponseDTO;
import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;
import org.springframework.security.core.Authentication;

public interface AuthenticationService {
  APIResponseDTO<EmployeeResponseDTO> signup(Authentication authentication, EmployeeRequestDTO request);
  AuthenticationResponseDTO login(EmployeeRequestDTO request, String ipAddress, String userAgent);
  AuthenticationResponseDTO refreshToken(String refreshToken);
}
