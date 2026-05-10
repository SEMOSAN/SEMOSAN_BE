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
    WITHDRAW_SUCCESS(HttpStatus.OK, "USER_200_1", "회원 탈퇴가 완료되었습니다."),
    REGISTER_ONBOARDING_SUCCESS(HttpStatus.OK, "USER_200_2", "온보딩 등록에 성공했습니다."),
    UPDATE_PROFILE_SUCCESS(HttpStatus.OK, "USER_200_3", "프로필 수정에 성공했습니다."),
    GET_PROFILE_SUCCESS(HttpStatus.OK, "USER_200_4", "프로필 조회에 성공했습니다."),
    UPDATE_PUSH_NOTIFICATION_SETTING_SUCCESS(HttpStatus.OK, "USER_200_5", "푸시알림 설정 변경에 성공했습니다."),
    UPDATE_LIVE_ACTIVITY_SETTING_SUCCESS(HttpStatus.OK, "USER_200_6", "라이브 액티비티 설정 변경에 성공했습니다."),
    UPDATE_VOICE_SETTING_SUCCESS(HttpStatus.OK, "USER_200_7", "음성 설정 변경에 성공했습니다."),
    GET_NOTIFICATION_SETTING_SUCCESS(HttpStatus.OK, "USER_200_8", "알림 설정 조회에 성공했습니다."),

    /**
     * Mountain
     */
    MOUNTAIN_LIST_SUCCESS(HttpStatus.OK, "MTN_200_1", "산 목록 조회에 성공했습니다."),
    MOUNTAIN_SEARCH_SUCCESS(HttpStatus.OK, "MTN_200_2", "산 검색에 성공했습니다."),
    MOUNTAIN_DETAIL_SUCCESS(HttpStatus.OK, "MTN_200_3", "산 상세 정보 조회에 성공했습니다."),

    /**
     * Image
     */
    PRESIGNED_URL_SUCCESS(HttpStatus.OK, "IMG_200_1", "Presigned URL 발급에 성공했습니다."),

    /**
     * FCM / Notification
     */
    FCM_TOKEN_REGISTER_SUCCESS(HttpStatus.OK, "FCM_200_1", "FCM 토큰이 등록되었습니다."),
    FCM_TOKEN_DELETE_SUCCESS(HttpStatus.OK, "FCM_200_2", "FCM 토큰이 삭제되었습니다."),
    NOTIFICATION_SEND_SUCCESS(HttpStatus.OK, "NOTIF_200_1", "알림 발송 요청에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
