package com.semosan.api.common.exception;

import com.semosan.api.common.alert.ServerErrorAlertService;
import com.semosan.api.common.response.ApiResponse;
import com.semosan.api.common.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneralExceptionAdviceTest {

    @Mock
    private ServerErrorAlertService serverErrorAlertService;

    @InjectMocks
    private GeneralExceptionAdvice advice;

    @Test
    void handleGeneralExceptionReturnsConfiguredErrorWithoutAlertFor4xx() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<ApiResponse<Void>> response =
                advice.handleGeneralException(new GeneralException(ErrorStatus.BAD_REQUEST), request);

        assertThat(response.getStatusCode()).isEqualTo(ErrorStatus.BAD_REQUEST.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorStatus.BAD_REQUEST.getCode());
        verify(serverErrorAlertService, never()).notify(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void handleGeneralExceptionSendsAlertFor5xx() {
        HttpServletRequest request = mockRequest();
        GeneralException exception = new GeneralException(ErrorStatus.INTERNAL_SERVER_ERROR);

        ResponseEntity<ApiResponse<Void>> response = advice.handleGeneralException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(ErrorStatus.INTERNAL_SERVER_ERROR.getHttpStatus());
        verify(serverErrorAlertService).notify(
                org.mockito.ArgumentMatchers.eq(500),
                org.mockito.ArgumentMatchers.eq(exception),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void handleIllegalArgumentExceptionReturnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                advice.handleIllegalArgumentException(new IllegalArgumentException("bad"));

        assertThat(response.getStatusCode()).isEqualTo(ErrorStatus.BAD_REQUEST.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorStatus.BAD_REQUEST.getCode());
    }

    @Test
    void handleConstraintViolationReturnsFirstViolationMessage() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("mountainId");
        when(violation.getMessage()).thenReturn("필수입니다.");

        ResponseEntity<ApiResponse<Void>> response =
                advice.handleConstraintViolation(new ConstraintViolationException(Set.of(violation)));

        assertThat(response.getStatusCode()).isEqualTo(ErrorStatus.BAD_REQUEST.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("mountainId: 필수입니다.");
    }

    @Test
    void handleExceptionReturnsInternalServerErrorAndSendsAlert() {
        HttpServletRequest request = mockRequest();
        RuntimeException exception = new RuntimeException("boom");

        ResponseEntity<ApiResponse<Void>> response = advice.handleException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(ErrorStatus.INTERNAL_SERVER_ERROR.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorStatus.INTERNAL_SERVER_ERROR.getCode());
        verify(serverErrorAlertService).notify(
                org.mockito.ArgumentMatchers.eq(500),
                org.mockito.ArgumentMatchers.eq(exception),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private HttpServletRequest mockRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURL()).thenReturn(new StringBuffer("https://api.example.com/fail"));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        return request;
    }
}
