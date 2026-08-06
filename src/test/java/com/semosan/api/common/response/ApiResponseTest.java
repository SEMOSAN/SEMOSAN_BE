package com.semosan.api.common.response;

import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.common.status.SuccessStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successWithoutDataUsesSuccessStatus() {
        ResponseEntity<ApiResponse<Void>> response = ApiResponse.success(SuccessStatus.LOGIN_SUCCESS);

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.LOGIN_SUCCESS.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsSuccess()).isTrue();
        assertThat(response.getBody().getCode()).isEqualTo(SuccessStatus.LOGIN_SUCCESS.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(SuccessStatus.LOGIN_SUCCESS.getMessage());
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    void successWithDataContainsPayload() {
        ResponseEntity<ApiResponse<String>> response = ApiResponse.success(SuccessStatus.LOGIN_SUCCESS, "payload");

        assertThat(response.getStatusCode()).isEqualTo(SuccessStatus.LOGIN_SUCCESS.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsSuccess()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo("payload");
    }

    @Test
    void errorUsesErrorStatusMessage() {
        ResponseEntity<ApiResponse<Void>> response = ApiResponse.error(ErrorStatus.BAD_REQUEST);

        assertThat(response.getStatusCode()).isEqualTo(ErrorStatus.BAD_REQUEST.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorStatus.BAD_REQUEST.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorStatus.BAD_REQUEST.getMessage());
    }

    @Test
    void errorCanOverrideMessage() {
        ResponseEntity<ApiResponse<Void>> response = ApiResponse.error(ErrorStatus.BAD_REQUEST, "custom");

        assertThat(response.getStatusCode()).isEqualTo(ErrorStatus.BAD_REQUEST.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("custom");
    }
}
