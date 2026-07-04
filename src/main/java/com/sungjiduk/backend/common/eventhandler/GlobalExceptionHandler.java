package com.sungjiduk.backend.common.eventhandler;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode code = exception.getErrorCode();
        return ResponseEntity
            .status(
                code.getStatus()
            ).body(
                ApiResponse.error(
                    code.name(),
                    exception.getMessage())
            );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
        MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse(ErrorCode.VALIDATION_FAILED.getDescription());

        return ResponseEntity
            .status(ErrorCode.VALIDATION_FAILED.getStatus())
            .body(
                ApiResponse.error(
                    ErrorCode.VALIDATION_FAILED.name(),
                    message)
            );
    }
}
