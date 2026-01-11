package com.ndh.ShopTechnology.exception;

import com.ndh.ShopTechnology.constant.MessageConstant;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j(topic = "GLOBAL_EXCEPTION_HANDLER")
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = NotFoundEntityException.class)
    public ResponseEntity<APIResponse<Void>> handleNotFound(NotFoundEntityException ex) {
        log.error("NotFoundEntityException: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.of(
                false,
                ex.getMessage(),
                null,
                List.of(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build()),
                null
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(value = AuthenticationFailedException.class)
    public ResponseEntity<APIResponse<Void>> handleAuthFailed(AuthenticationFailedException ex) {
        log.error("AuthenticationFailedException: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.of(
                false,
                ex.getMessage(),
                null,
                List.of(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build()),
                null
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());

        List<ErrorResponse> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> ErrorResponse.builder()
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .rejectedValue(fieldError.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        APIResponse<Void> response = APIResponse.of(
                false,
                MessageConstant.VALIDATION_FAILED,
                null,
                errors,
                null
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<APIResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.error("AccessDeniedException: {}", ex.getMessage());

        APIResponse<Void> response = APIResponse.of(
                false,
                MessageConstant.ACCESS_DENIED,
                null,
                List.of(ErrorResponse.builder()
                        .message(MessageConstant.ACCESS_DENIED)
                        .build()),
                null
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    @ExceptionHandler(value = CustomApiException.class)
    public ResponseEntity<APIResponse<Void>> handleCustomApiException(CustomApiException ex) {
        log.error("CustomApiException: status={}, message={}", ex.getStatus(), ex.getMessage());

        APIResponse<Void> response = APIResponse.of(
                false,
                ex.getMessage(),
                null,
                List.of(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build()),
                null
        );

        return ResponseEntity
                .status(ex.getStatus())
                .body(response);
    }
}