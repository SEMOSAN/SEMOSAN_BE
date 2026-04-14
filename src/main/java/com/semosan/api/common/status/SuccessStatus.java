package com.semosan.api.common.status;

import com.semosan.api.common.base.BaseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseStatus {

    COMMON_SUCCESS_STATUS(HttpStatus.OK, "COM_200", "성공적으로 처리되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
