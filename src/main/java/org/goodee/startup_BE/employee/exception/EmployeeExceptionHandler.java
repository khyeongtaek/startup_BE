package org.goodee.startup_BE.employee.exception;


import org.goodee.startup_BE.common.dto.APIResponseDTO;
// 이 핸들러를 적용할 컨트롤러를 import 해야 함 (경로는 예시)
import org.goodee.startup_BE.employee.controller.EmployeeController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// assignableTypes 속성에 적용할 컨트롤러 클래스를 지정
@RestControllerAdvice(assignableTypes = EmployeeController.class)
public class EmployeeExceptionHandler {

    // 400 Bad Request (이메일 중복)
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<APIResponseDTO<Void>> handleDuplicateEmailException(DuplicateEmailException e) {
        APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 401 Unauthorized (로그인 실패 - /api/auth/login)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<APIResponseDTO<Void>> handleBadCredentialsException(BadCredentialsException e) {
        APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

}