package org.goodee.startup_BE.common.exception;


import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.employee.exception.DuplicateEmailException;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

import java.util.NoSuchElementException;

@RestControllerAdvice
@Slf4j
public class CommonExceptionHandler {

    // 400 Bad Request (잘못된 코드값 또는 리소스 조회 실패)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponseDTO<Void>> handleResourceNotFoundException(ResourceNotFoundException e) {
        APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 400 Bad Request (잘못된 상태)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<APIResponseDTO<Void>> handleIllegalStateException(IllegalStateException e) {
        APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 400 Bad Request (잘못된 접근)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponseDTO<Void>> handleAccessDeniedException(AccessDeniedException e) {
        APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    /**
     * @Validated 유효성 검사 실패 시 발생하는 예외 처리
     * (HTTP 400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponseDTO<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        log.warn("Validation failed: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        APIResponseDTO<Map<String, String>> responseDTO = APIResponseDTO.<Map<String, String>>builder()
                .message("입력값이 유효하지 않습니다.")
                .data(errors)
                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    /**
     * 엔티티 조회 실패 시 (ApprovalServiceImpl 등에서 발생)
     * (HTTP 404 Not Found)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public APIResponseDTO<String> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return APIResponseDTO.<String>builder()
                .message(ex.getMessage())
                .data("NOT_FOUND")
                .build();
    }


    @ExceptionHandler(java.nio.file.AccessDeniedException.class)
    public ResponseEntity<APIResponseDTO<Void>> handleFileAccessDeniedException(AccessDeniedException e) {
        APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<APIResponseDTO<Void>> handleNoSuchElementException(NoSuchElementException e) {
        APIResponseDTO<Void> response = APIResponseDTO.<Void>builder()
                .message(e.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}