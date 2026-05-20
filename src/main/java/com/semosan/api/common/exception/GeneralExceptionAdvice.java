package com.semosan.api.common.exception;

import com.semosan.api.common.base.BaseStatus;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice extends ResponseEntityExceptionHandler {

    // 커스텀 예외(GeneralException)를 잡아서 정의된 에러 상태로 응답 반환
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(
            GeneralException e
    ) {
        if (e.getErrorStatus().getHttpStatus().is5xxServerError()) {
            log.error("[*] GeneralException :", e);
        } else {
            log.warn("[*] GeneralException : {}", e.getMessage());
        }
        return ApiResponse.error(e.getErrorStatus());
    }

    // 잘못된 인자 전달 시 발생한 예외를 400 에러로 변환하여 응답
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException e
    ) {
        log.warn("[*] IllegalArgumentException : {}", e.getMessage());
        return ApiResponse.error(ErrorStatus.BAD_REQUEST);
    }

    // @Validated 컨트롤러의 @RequestParam/@PathVariable 단순 타입 검증 실패 (Spring 6 MethodValidationInterceptor 경로)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException e
    ) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath().toString() + ": " + v.getMessage())
                .orElse(ErrorStatus.BAD_REQUEST.getMessage());
        log.warn("[*] ConstraintViolationException : {}", message);
        return ApiResponse.error(ErrorStatus.BAD_REQUEST, message);
    }

    // Spring 6.1+ HandlerMethodValidationException (컨트롤러 메서드 파라미터 검증 실패 표준 경로)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
            HandlerMethodValidationException e
    ) {
        String message = e.getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : ErrorStatus.BAD_REQUEST.getMessage())
                .orElse(ErrorStatus.BAD_REQUEST.getMessage());
        log.warn("[*] HandlerMethodValidationException : {}", message);
        return ApiResponse.error(ErrorStatus.BAD_REQUEST, message);
    }

    // null 참조로 발생한 서버 오류를 500 에러로 응답
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(
            NullPointerException e
    ) {
        log.error("[*] NullPointerException :", e);
        return ApiResponse.error(ErrorStatus.INTERNAL_SERVER_ERROR);
    }

    // 처리되지 않은 모든 예외를 잡아 500 서버 오류로 응답
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception e
    ) {
        log.error("[*] Internal Server Error :", e);
        return ApiResponse.error(ErrorStatus.INTERNAL_SERVER_ERROR);
    }

    // @Valid 검증 실패 시 발생하는 예외를 잡아 필드 에러 메시지로 응답
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest webRequest
    ) {
        BaseStatus errorStatus = ErrorStatus.BAD_REQUEST;

        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .findFirst()
                .or(() -> e.getBindingResult().getGlobalErrors().stream()
                        .map(globalError -> globalError.getObjectName() + ": " + globalError.getDefaultMessage())
                        .findFirst()
                )
                .orElse(errorStatus.getMessage());

        ApiResponse<Void> body = createApiResponse(errorStatus, errorMessage);
        return handleExceptionInternal(e, body, headers, statusCode, webRequest);
    }

    // 공통 에러 응답(ApiResponse) 객체를 생성하는 헬퍼 메서드
    private ApiResponse<Void> createApiResponse(BaseStatus errorStatus, String errorMessage) {
        return new ApiResponse<>(
                false,
                errorStatus.getCode(),
                errorMessage,
                null
        );
    }

}
