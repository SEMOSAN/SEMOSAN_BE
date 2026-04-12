package com.semosan.api.common.base;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus {

    COMMON_ERROR_STATUS(HttpStatus.BAD_REQUEST, "COM_400", "잘못된 요청입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
