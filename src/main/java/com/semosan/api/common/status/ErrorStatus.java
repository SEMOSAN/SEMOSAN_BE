package com.semosan.api.common.status;

import com.semosan.api.common.base.BaseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseStatus {

    /**
     * Common
     */
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COM_400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COM_401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COM_403", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COM_404", "요청한 자원을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COM_405", "허용되지 않은 메소드입니다."),
    CONFLICT(HttpStatus.CONFLICT, "COM_409", "이미 존재하는 리소스입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COM_415", "지원하지 않는 형식입니다."),
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, "COM_422", "처리할 수 없는 요청입니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "COM_429", "요청이 너무 많습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COM_500", "서버 내부 오류입니다."),

    /**
     * JWT
     */
    JWT_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "JWT_401_1", "토큰이 존재하지 않습니다."),
    JWT_INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "JWT_401_2", "잘못된 JWT 서명입니다."),
    JWT_MALFORMED(HttpStatus.UNAUTHORIZED, "JWT_401_3", "잘못된 JWT 형식입니다."),
    JWT_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT_401_4", "만료된 JWT 토큰입니다."),
    JWT_UNSUPPORTED(HttpStatus.UNAUTHORIZED, "JWT_401_5", "지원되지 않는 JWT 토큰입니다."),
    JWT_INVALID(HttpStatus.UNAUTHORIZED, "JWT_401_6", "JWT 토큰이 잘못되었습니다."),
    JWT_EXTRACT_ID_FAILED(HttpStatus.UNAUTHORIZED, "JWT_401_7", "토큰에서 사용자 정보를 추출할 수 없습니다."),
    JWT_GENERAL_ERROR(HttpStatus.UNAUTHORIZED, "JWT_401_8", "JWT 토큰 처리 중 알 수 없는 오류가 발생했습니다."),
    JWT_INVALID_TYPE(HttpStatus.UNAUTHORIZED, "JWT_401_9", "토큰 타입이 유효하지 않습니다."),
    JWT_BLACKLISTED(HttpStatus.UNAUTHORIZED, "JWT_401_10", "로그아웃된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "JWT_401_11", "리프레시 토큰이 존재하지 않습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "JWT_401_12", "리프레시 토큰 정보가 사용자 정보와 일치하지 않습니다."),
    JWT_EXTRACT_ROLE_FAILED(HttpStatus.UNAUTHORIZED, "JWT_401_13", "토큰에서 사용자 Role을 추출할 수 없습니다."),

    /**
     * Kakao OAuth
     */
    KAKAO_TOKEN_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "KAKAO_502_1", "카카오 토큰 발급에 실패했습니다."),
    KAKAO_USER_INFO_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "KAKAO_502_2", "카카오 사용자 정보 조회에 실패했습니다."),

    /**
     * User
     */
    PREFERRED_DIFFICULTY_REQUIRED(HttpStatus.BAD_REQUEST, "USER_400_1", "숙련자는 선호 난이도를 선택해야 합니다."),
    PREFERRED_DIFFICULTY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "USER_400_2", "선호 난이도는 숙련자만 선택할 수 있습니다."),
    PROFILE_UPDATE_FIELD_REQUIRED(HttpStatus.BAD_REQUEST, "USER_400_3", "수정할 프로필 정보가 없습니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "USER_400_4", "사용할 수 없는 닉네임입니다."),
    UNDER_AGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "USER_400_5", "만 14세 미만은 가입할 수 없습니다."),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "USER_400_6", "닉네임 형식이 올바르지 않습니다."),
    NICKNAME_RESERVED(HttpStatus.BAD_REQUEST, "USER_400_7", "사용할 수 없는 사칭 표현이 포함된 닉네임입니다."),
    NICKNAME_BLOCKED_WORD(HttpStatus.BAD_REQUEST, "USER_400_8", "금칙어가 포함된 닉네임입니다."),
    EXERCISE_DETAIL_REQUIRED(HttpStatus.BAD_REQUEST, "USER_400_9", "운동 빈도와 운동 시간을 입력해야 합니다."),
    EXERCISE_DETAIL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "USER_400_10", "운동 안함 선택 시 운동 빈도와 운동 시간을 입력할 수 없습니다."),
    ONBOARDING_NOT_COMPLETED(HttpStatus.FORBIDDEN, "USER_403_1", "온보딩을 완료해야 이용할 수 있습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_1", "사용자를 찾을 수 없습니다."),
    NOTIFICATION_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_2", "알림 설정을 찾을 수 없습니다."),
    ONBOARDING_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_3", "온보딩 정보를 찾을 수 없습니다."),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "USER_409_1", "이미 온보딩을 완료한 사용자입니다."),
    DUPLICATED_NICKNAME(HttpStatus.CONFLICT, "USER_409_2", "이미 사용 중인 닉네임입니다."),

    /**
     * Notification
     */
    NOTIFICATION_PARAMS_INVALID(HttpStatus.BAD_REQUEST, "NOTIF_400_1", "알림 파라미터가 유효하지 않습니다."),

    /**
     * Apple OAuth
     */
    APPLE_PUBLIC_KEY_NOT_FOUND(HttpStatus.UNAUTHORIZED, "APPLE_401_1", "유효한 애플 공개키를 찾을 수 없습니다."),
    APPLE_IDENTITY_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "APPLE_401_2", "애플 identity token이 유효하지 않습니다."),
    APPLE_PUBLIC_KEY_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "APPLE_502_1", "애플 공개키 조회에 실패했습니다."),

    /**
     * Mountain
     */
    MOUNTAIN_NOT_FOUND(HttpStatus.NOT_FOUND, "MTN_404_1", "산을 찾을 수 없습니다."),
    MOUNTAIN_LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "MTN_409_1", "이미 좋아요한 산입니다."),
    MOUNTAIN_BBOX_PARTIAL(HttpStatus.BAD_REQUEST, "MTN_400_1", "BBox 좌표는 4개(swLat, swLng, neLat, neLng) 모두 보내거나 모두 비워주세요."),
    MOUNTAIN_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "MTN_404_2", "좋아요한 산이 아닙니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "MTN_404_3", "코스를 찾을 수 없습니다."),

    /**
     * Image
     */
    INVALID_IMAGE_BUCKET(HttpStatus.BAD_REQUEST, "IMG_400_1", "허용되지 않은 버킷입니다."),
    INVALID_IMAGE_EXTENSION(HttpStatus.BAD_REQUEST, "IMG_400_2", "허용되지 않은 이미지 확장자입니다. (jpg, jpeg, png, webp)"),
    IMAGE_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "IMG_502_1", "이미지 업로드 URL 생성에 실패했습니다."),

    /**
     * Withdraw
     */
    KAKAO_UNLINK_FAILED(HttpStatus.BAD_GATEWAY, "KAKAO_502_3", "카카오 연동 해제에 실패했습니다."),
    APPLE_REVOKE_FAILED(HttpStatus.BAD_GATEWAY, "APPLE_502_1", "애플 토큰 폐기에 실패했습니다."),

    /**
     * Hiking (등산 기록)
     */
    HIKING_RECORD_COURSE_REQUIRED(HttpStatus.BAD_REQUEST, "HIKE_400_1", "코스 기반 등산 기록에만 난이도 피드백을 남길 수 있습니다."),
    HIKING_RECORD_FORBIDDEN(HttpStatus.FORBIDDEN, "HIKE_403_1", "본인이 참여한 등산 기록만 공유할 수 있습니다."),
    HIKING_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "HIKE_404_1", "등산 기록을 찾을 수 없습니다."),
    COURSE_DIFFICULTY_FEEDBACK_ALREADY_EXISTS(HttpStatus.CONFLICT, "HIKE_409_1", "이미 난이도 피드백을 남긴 등산 기록입니다."),

    /**
     * Tracking (트래킹 세션)
     */
    TRACKING_COURSE_MOUNTAIN_MISMATCH(HttpStatus.BAD_REQUEST, "TRK_400_1", "선택한 코스가 해당 산의 코스가 아닙니다."),
    TRACKING_COURSE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "TRK_400_2", "자유 기록이 아니면 코스 ID는 필수입니다."),
    TRACKING_SESSION_FORBIDDEN(HttpStatus.FORBIDDEN, "TRK_403_1", "본인의 트래킹 세션만 조작할 수 있습니다."),
    TRACKING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "TRK_404_1", "트래킹 세션을 찾을 수 없습니다."),
    TRACKING_PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "TRK_404_2", "트래킹 사진을 찾을 수 없습니다."),
    TRACKING_SESSION_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "TRK_409_1", "이미 진행 중인 트래킹 세션이 있습니다."),
    TRACKING_SESSION_INVALID_STATE(HttpStatus.CONFLICT, "TRK_409_2", "현재 상태에서는 수행할 수 없는 작업입니다."),
    TRACKING_PHOTO_DUPLICATE(HttpStatus.CONFLICT, "TRK_409_3", "해당 마일스톤에 이미 업로드된 사진이 있습니다."),
    TRACKING_PHOTO_SESSION_INACTIVE(HttpStatus.CONFLICT, "TRK_409_4", "활성 상태가 아닌 세션에는 사진을 업로드할 수 없습니다."),
    TRACKING_COURSE_POLYLINE_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "TRK_422_1", "코스 경로 좌표가 등록되지 않았습니다."),

    /**
     * Post (게시글 공통)
     */
    POST_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "POST_400_1", "본문은 비어있을 수 없습니다."),
    POST_IMAGE_INDEX_INVALID(HttpStatus.BAD_REQUEST, "POST_400_2", "대표 이미지 인덱스가 잘못되었습니다."),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "POST_403_1", "본인의 게시글만 처리할 수 있습니다."),
    POST_AUTHOR_BLOCKED(HttpStatus.FORBIDDEN, "POST_403_2", "차단한 사용자의 게시글입니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_404_1", "게시글을 찾을 수 없습니다."),
    POST_DELETED(HttpStatus.NOT_FOUND, "POST_404_2", "삭제된 게시글입니다."),

    /**
     * Comment (댓글/대댓글)
     */
    COMMENT_PARENT_POST_MISMATCH(HttpStatus.BAD_REQUEST, "CMT_400_1", "부모 댓글이 같은 게시글의 댓글이 아닙니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "CMT_403_1", "본인의 댓글만 처리할 수 있습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CMT_404_1", "댓글을 찾을 수 없습니다."),
    COMMENT_DELETED(HttpStatus.NOT_FOUND, "CMT_404_2", "삭제된 댓글입니다."),

    /**
     * SemoFeed (세모피드)
     */
    SEMOFEED_FORBIDDEN(HttpStatus.FORBIDDEN, "SF_403_1", "본인의 세모피드만 처리할 수 있습니다."),
    SEMOFEED_NOT_FOUND(HttpStatus.NOT_FOUND, "SF_404_1", "세모피드를 찾을 수 없습니다."),

    /**
     * Free Post Report / Block
     */
    FREE_POST_REPORT_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FPR_400_1", "본인 게시글은 신고할 수 없습니다."),
    FREE_POST_REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "FPR_409_1", "이미 신고한 게시글입니다."),
    USER_BLOCK_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "UB_400_1", "자기 자신은 차단할 수 없습니다."),

    /**
     * App Version
     */
    APP_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "APPV_404_1", "앱 버전 정보를 찾을 수 없습니다."),
    APP_VERSION_READ_FAILED(HttpStatus.BAD_GATEWAY, "APPV_502_1", "앱 버전 정보 조회에 실패했습니다."),
    APP_VERSION_UPDATE_FAILED(HttpStatus.BAD_GATEWAY, "APPV_502_2", "앱 버전 정보 저장에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
