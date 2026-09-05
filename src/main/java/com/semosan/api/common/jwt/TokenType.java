package com.semosan.api.common.jwt;

/**
 * JWT 의 용도 구분. 발급 시 claim 으로 직렬화되고, 검증 시 용도 밖 사용을 차단하는 기준이 된다.
 * claim 표현(키/값)은 JwtService 내부에 은닉되어 있으므로 외부에서는 이 enum 으로만 다룬다.
 */
public enum TokenType {
    ACCESS,
    REFRESH,
    ADMIN
}
