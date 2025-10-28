package org.goodee.startup_BE.common.exception;


import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.employee.exception.DuplicateEmailException;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;

@RestControllerAdvice
public class CommonExceptionHandler {

    // 400 Bad Request (잘못된 코드값 또는 리소스 조회 실패)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponseDTO<Void>> handleResourceNotFoundException(ResourceNotFoundException e) {
        APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponseDTO<Void>> handleAccessDeniedException(AccessDeniedException e) {
        APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }


}