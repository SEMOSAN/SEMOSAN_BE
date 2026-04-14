package com.semosan.api.common.status;

import com.semosan.api.common.base.BaseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseStatus {

    COMMON_SUCCESS_STATUS(HttpStatus.OK, "COM_200", "성공적으로 처리되었습니다."),

    /**
     * Auth
     */
    LOGIN_SUCCESS(HttpStatus.OK, "AUTH_200_1", "로그인에 성공했습니다."),
    REISSUE_SUCCESS(HttpStatus.OK, "AUTH_200_2", "토큰 재발급에 성공했습니다."),

    /**
     * User
     */
    WITHDRAW_SUCCESS(HttpStatus.OK, "USER_200_1", "회원 탈퇴가 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
