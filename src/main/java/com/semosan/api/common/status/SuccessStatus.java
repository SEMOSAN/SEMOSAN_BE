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
    LOGOUT_SUCCESS(HttpStatus.OK, "AUTH_200_3", "로그아웃에 성공했습니다."),

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
    CHECK_NICKNAME_SUCCESS(HttpStatus.OK, "USER_200_9", "닉네임 사용 가능 여부 조회에 성공했습니다."),

    /**
     * Mountain
     */
    MOUNTAIN_LIST_SUCCESS(HttpStatus.OK, "MTN_200_1", "산 목록 조회에 성공했습니다."),
    MOUNTAIN_SEARCH_SUCCESS(HttpStatus.OK, "MTN_200_2", "산 검색에 성공했습니다."),
    MOUNTAIN_DETAIL_SUCCESS(HttpStatus.OK, "MTN_200_3", "산 상세 정보 조회에 성공했습니다."),
    MOUNTAIN_LIKE_SUCCESS(HttpStatus.OK, "MTN_200_4", "산 좋아요 등록에 성공했습니다."),
    MOUNTAIN_UNLIKE_SUCCESS(HttpStatus.OK, "MTN_200_5", "산 좋아요 취소에 성공했습니다."),
    LIKED_MOUNTAIN_LIST_SUCCESS(HttpStatus.OK, "MTN_200_6", "좋아요한 산 목록 조회에 성공했습니다."),
    MOUNTAIN_MAP_SUCCESS(HttpStatus.OK, "MTN_200_7", "지도 영역 내 산 조회에 성공했습니다."),
    MOUNTAIN_RECOMMENDATION_SUCCESS(HttpStatus.OK, "MTN_200_8", "레벨 맞춤 산 추천 조회에 성공했습니다."),
    COURSE_DETAIL_SUCCESS(HttpStatus.OK, "MTN_200_9", "코스 상세 정보 조회에 성공했습니다."),
    COURSE_LIKE_TOGGLE_SUCCESS(HttpStatus.OK, "MTN_200_10", "코스 좋아요 처리에 성공했습니다."),

    /**
     * Hiking Record
     */
    GET_HIKING_RECORD_MOUNTAIN_LIST_SUCCESS(HttpStatus.OK, "HIKING_200_1", "내가 다녀온 산 목록 조회에 성공했습니다."),
    GET_HIKING_RECORD_SUMMARY_SUCCESS(HttpStatus.OK, "HIKING_200_2", "나의 등산 기록 요약 조회에 성공했습니다."),
    GET_HIKING_RECORD_LIST_SUCCESS(HttpStatus.OK, "HIKING_200_3", "나의 등산 기록 목록 조회에 성공했습니다."),
    GET_HIKING_RECORD_LIST_BY_MOUNTAIN_SUCCESS(HttpStatus.OK, "HIKING_200_4", "특정 산의 나의 등산 기록 목록 조회에 성공했습니다."),
    GET_HIKING_RECORD_DETAIL_SUCCESS(HttpStatus.OK, "HIKING_200_5", "등산 기록 상세 조회에 성공했습니다."),
    CREATE_COURSE_DIFFICULTY_FEEDBACK_SUCCESS(HttpStatus.CREATED, "HIKING_201_1", "코스 난이도 피드백 저장에 성공했습니다."),

    /**
     * Tracking — 진입 화면 (#45) & 세션 (#18)
     */
    TRACKING_NEAREST_MOUNTAIN_SUCCESS(HttpStatus.OK, "TRK_200_1", "현재 위치 기준 가까운 산과 코스 조회에 성공했습니다."),
    TRACKING_SESSION_GET_ACTIVE_SUCCESS(HttpStatus.OK, "TRK_200_2", "현재 진행 중인 트래킹 세션 조회에 성공했습니다."),
    TRACKING_SESSION_GET_SUCCESS(HttpStatus.OK, "TRK_200_3", "트래킹 세션 상세 조회에 성공했습니다."),
    TRACKING_SESSION_PAUSE_SUCCESS(HttpStatus.OK, "TRK_200_4", "트래킹 세션을 일시정지했습니다."),
    TRACKING_SESSION_RESUME_SUCCESS(HttpStatus.OK, "TRK_200_5", "트래킹 세션을 재개했습니다."),
    TRACKING_SESSION_COMPLETE_SUCCESS(HttpStatus.OK, "TRK_200_6", "트래킹 세션을 종료했습니다."),
    TRACKING_SESSION_ABANDON_SUCCESS(HttpStatus.OK, "TRK_200_7", "트래킹 세션을 포기 처리했습니다."),
    TRACKING_LIVE_ACTIVITY_COURSE_SUCCESS(HttpStatus.OK, "TRK_200_8", "라이브 액티비티용 코스 정보 조회에 성공했습니다."),
    TRACKING_PHOTO_LIST_SUCCESS(HttpStatus.OK, "TRK_200_9", "트래킹 사진 목록 조회에 성공했습니다."),
    TRACKING_SESSION_CREATE_SUCCESS(HttpStatus.CREATED, "TRK_201_1", "트래킹 세션이 시작되었습니다."),
    TRACKING_PHOTO_UPLOAD_SUCCESS(HttpStatus.CREATED, "TRK_201_2", "트래킹 사진이 저장되었습니다."),

    /**
     * SemoFeed
     */
    SEMOFEED_LIST_SUCCESS(HttpStatus.OK, "SF_200_1", "세모피드 목록 조회에 성공했습니다."),
    SEMOFEED_MY_LIST_SUCCESS(HttpStatus.OK, "SF_200_2", "내 세모피드 목록 조회에 성공했습니다."),
    SEMOFEED_TOGGLE_PUBLIC_SUCCESS(HttpStatus.OK, "SF_200_3", "세모피드 공개 상태가 변경되었습니다."),
    SEMOFEED_DELETE_SUCCESS(HttpStatus.OK, "SF_200_4", "세모피드가 삭제되었습니다."),
    SEMOFEED_EMOJI_TOGGLE_SUCCESS(HttpStatus.OK, "SF_200_5", "세모피드 이모지 처리에 성공했습니다."),
    SEMOFEED_CREATE_SUCCESS(HttpStatus.CREATED, "SF_201_1", "세모피드가 저장되었습니다."),

    /**
     * Image
     */
    PRESIGNED_URL_SUCCESS(HttpStatus.OK, "IMG_200_1", "Presigned URL 발급에 성공했습니다."),

    /**
     * FCM / Notification
     */
    FCM_TOKEN_REGISTER_SUCCESS(HttpStatus.OK, "FCM_200_1", "FCM 토큰이 등록되었습니다."),
    FCM_TOKEN_DELETE_SUCCESS(HttpStatus.OK, "FCM_200_2", "FCM 토큰이 삭제되었습니다."),
    NOTIFICATION_SEND_SUCCESS(HttpStatus.OK, "NOTIF_200_1", "알림 발송 요청에 성공했습니다."),

    /**
     * Record Post (기록공유 게시글)
     */
    RECORD_POST_CREATE_SUCCESS(HttpStatus.CREATED, "RPOST_201_1", "기록공유 게시글이 작성되었습니다."),
    RECORD_POST_LIST_SUCCESS(HttpStatus.OK, "RPOST_200_1", "기록공유 게시글 목록 조회에 성공했습니다."),
    RECORD_POST_MY_LIST_SUCCESS(HttpStatus.OK, "RPOST_200_2", "내 기록공유 게시글 목록 조회에 성공했습니다."),
    RECORD_POST_DETAIL_SUCCESS(HttpStatus.OK, "RPOST_200_3", "기록공유 게시글 상세 조회에 성공했습니다."),
    RECORD_POST_DELETE_SUCCESS(HttpStatus.OK, "RPOST_200_4", "기록공유 게시글이 삭제되었습니다."),

    /**
     * Comment (댓글/대댓글)
     */
    COMMENT_LIST_SUCCESS(HttpStatus.OK, "CMT_200_1", "댓글 목록 조회에 성공했습니다."),
    COMMENT_REPLY_LIST_SUCCESS(HttpStatus.OK, "CMT_200_2", "대댓글 목록 조회에 성공했습니다."),
    COMMENT_DELETE_SUCCESS(HttpStatus.OK, "CMT_200_3", "댓글이 삭제되었습니다."),
    COMMENT_CREATE_SUCCESS(HttpStatus.CREATED, "CMT_201_1", "댓글이 작성되었습니다."),
    COMMENT_REPLY_SUCCESS(HttpStatus.CREATED, "CMT_201_2", "대댓글이 작성되었습니다."),
    COMMENT_BLOCK_SUCCESS(HttpStatus.CREATED, "CMT_201_3", "사용자를 차단했습니다."),

    /**
     * Post Like (좋아요)
     */
    POST_LIKE_TOGGLE_SUCCESS(HttpStatus.OK, "LIKE_200_1", "좋아요 처리에 성공했습니다."),
    POST_LIKE_COUNT_SUCCESS(HttpStatus.OK, "LIKE_200_2", "좋아요 수 조회에 성공했습니다."),

    /**
     * Free Post (자유게시판 게시글)
     */
    FREE_POST_LIST_SUCCESS(HttpStatus.OK, "FPOST_200_1", "자유게시판 게시글 목록 조회에 성공했습니다."),
    FREE_POST_MY_LIST_SUCCESS(HttpStatus.OK, "FPOST_200_2", "내 자유게시판 게시글 목록 조회에 성공했습니다."),
    FREE_POST_DETAIL_SUCCESS(HttpStatus.OK, "FPOST_200_3", "자유게시판 게시글 상세 조회에 성공했습니다."),
    FREE_POST_DELETE_SUCCESS(HttpStatus.OK, "FPOST_200_4", "자유게시판 게시글이 삭제되었습니다."),
    FREE_POST_SEARCH_SUCCESS(HttpStatus.OK, "FPOST_200_5", "자유게시판 게시글 검색에 성공했습니다."),
    FREE_POST_CREATE_SUCCESS(HttpStatus.CREATED, "FPOST_201_1", "자유게시판 게시글이 작성되었습니다."),
    FREE_POST_REPORT_SUCCESS(HttpStatus.CREATED, "FPOST_201_2", "자유게시판 게시글 신고가 접수되었습니다."),
    FREE_POST_BLOCK_SUCCESS(HttpStatus.CREATED, "FPOST_201_3", "사용자를 차단했습니다."),

    /**
     * App Version
     */
    APP_VERSION_GET_SUCCESS(HttpStatus.OK, "APPV_200_1", "앱 버전 정보 조회에 성공했습니다."),
    APP_VERSION_UPDATE_SUCCESS(HttpStatus.OK, "APPV_200_2", "앱 버전 정보 수정에 성공했습니다."),

    /**
     * Admin
     */
    ADMIN_LOGIN_SUCCESS(HttpStatus.OK, "ADM_200_1", "관리자 로그인에 성공했습니다."),
    ADMIN_MOUNTAIN_UPDATE_SUCCESS(HttpStatus.OK, "ADM_200_2", "산 정보 수정에 성공했습니다."),
    ADMIN_MOUNTAIN_VISIBILITY_UPDATE_SUCCESS(HttpStatus.OK, "ADM_200_3", "산 공개 상태 변경에 성공했습니다."),
    ADMIN_RESTAURANT_SECTION_UPDATE_SUCCESS(HttpStatus.OK, "ADM_200_4", "맛집 섹션 수정에 성공했습니다."),
    ADMIN_RESTAURANT_SECTION_DELETE_SUCCESS(HttpStatus.OK, "ADM_200_5", "맛집 섹션 삭제에 성공했습니다."),
    ADMIN_RESTAURANT_UPDATE_SUCCESS(HttpStatus.OK, "ADM_200_6", "맛집 수정에 성공했습니다."),
    ADMIN_RESTAURANT_DELETE_SUCCESS(HttpStatus.OK, "ADM_200_7", "맛집 삭제에 성공했습니다."),
    ADMIN_RESTAURANT_SECTION_CREATE_SUCCESS(HttpStatus.CREATED, "ADM_201_1", "맛집 섹션 생성에 성공했습니다."),
    ADMIN_RESTAURANT_CREATE_SUCCESS(HttpStatus.CREATED, "ADM_201_2", "맛집 생성에 성공했습니다."),
    ADMIN_REPORTED_POST_LIST_SUCCESS(HttpStatus.OK, "ADM_200_8", "신고된 게시글 목록 조회에 성공했습니다."),
    ADMIN_POST_DELETE_SUCCESS(HttpStatus.OK, "ADM_200_9", "게시글 강제 삭제에 성공했습니다."),
    ADMIN_COMMENT_DELETE_SUCCESS(HttpStatus.OK, "ADM_200_10", "댓글 강제 삭제에 성공했습니다."),
    ADMIN_USER_SUSPEND_SUCCESS(HttpStatus.OK, "ADM_200_11", "사용자 정지 처리에 성공했습니다."),
    ADMIN_USER_UNSUSPEND_SUCCESS(HttpStatus.OK, "ADM_200_12", "사용자 정지 해제에 성공했습니다."),
    ADMIN_TRANSPORTATION_UPDATE_SUCCESS(HttpStatus.OK, "ADM_200_13", "교통정보 수정에 성공했습니다."),
    ADMIN_TRANSPORTATION_DELETE_SUCCESS(HttpStatus.OK, "ADM_200_14", "교통정보 삭제에 성공했습니다."),
    ADMIN_TRANSPORTATION_CREATE_SUCCESS(HttpStatus.CREATED, "ADM_201_3", "교통정보 생성에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
