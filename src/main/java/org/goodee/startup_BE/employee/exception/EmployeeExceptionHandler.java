package org.goodee.startup_BE.employee.exception;


import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// assignableTypes 속성에 적용할 컨트롤러 클래스를 지정
@RestControllerAdvice(basePackages = "org.goodee.startup_BE.employee.controller")
public class EmployeeExceptionHandler {

    // 400 Bad Request (이메일 중복)
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<APIResponseDTO<Void>> handleDuplicateEmailException(DuplicateEmailException e) {
        APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 400 Bad Request (로그인 실패 - /api/auth/login)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<APIResponseDTO<Void>> handleBadCredentialsException(BadCredentialsException e) {
        APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}