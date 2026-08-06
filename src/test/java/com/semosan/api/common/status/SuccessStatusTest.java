package com.semosan.api.common.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class SuccessStatusTest {

    @Test
    void representativeStatusesExposeHttpStatusCodeAndMessage() {
        assertThat(SuccessStatus.LOGIN_SUCCESS.getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(SuccessStatus.LOGIN_SUCCESS.getCode()).isEqualTo("AUTH_200_1");
        assertThat(SuccessStatus.LOGIN_SUCCESS.getMessage()).isEqualTo("로그인에 성공했습니다.");

        assertThat(SuccessStatus.TRACKING_SESSION_CREATE_SUCCESS.getHttpStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(SuccessStatus.TRACKING_SESSION_CREATE_SUCCESS.getCode()).isEqualTo("TRK_201_1");
    }

    @Test
    void allSuccessStatusCodesAreNotBlank() {
        assertThat(SuccessStatus.values())
                .allSatisfy(status -> {
                    assertThat(status.getHttpStatus()).isNotNull();
                    assertThat(status.getCode()).isNotBlank();
                    assertThat(status.getMessage()).isNotBlank();
                });
    }
}
