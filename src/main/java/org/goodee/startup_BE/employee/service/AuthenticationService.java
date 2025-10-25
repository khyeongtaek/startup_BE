package org.goodee.startup_BE.employee.service;


import org.goodee.startup_BE.employee.dto.AuthenticationResponseDTO;
import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
import org.springframework.security.core.Authentication;

public interface AuthenticationService {
  AuthenticationResponseDTO signup(Authentication authentication, EmployeeRequestDTO request);
  AuthenticationResponseDTO login(EmployeeRequestDTO request, String ipAddress, String userAgent);
}
