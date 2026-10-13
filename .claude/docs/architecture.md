# 아키텍처 참고

## 패키지 구조

```text
com.semosan.api
├── common/            # 공통 인프라
│   ├── alert/           Discord 알림
│   ├── config/          Security, Redis, Swagger 등 설정
│   ├── exception/       GeneralException, 전역 핸들러
│   ├── fcm/             Firebase 발송
│   ├── filter/ jwt/     JWT 필터, 토큰, 블랙리스트
│   ├── ratelimit/       Redis 기반 rate limiter
│   ├── response/ status/  ApiResponse, SuccessStatus, ErrorStatus
│   └── weather/ util/ constant/ base/
└── domain/
    ├── admin/         # 관리자 API(/api/admin): 로그인(잠금), 산, 코스, 커뮤니티, 세모피드 관리
    ├── appversion/    # 앱 버전 확인(/api/app-version)
    ├── auth/          # JWT 로그인, 로그아웃, 재발급, 회원탈퇴
    ├── oauth/         # 카카오, 애플 소셜 로그인
    ├── user/          # 프로필, 온보딩, 알림 설정
    ├── mountain/      # 산, 코스, 좋아요, 지도 검색 (PostGIS)
    ├── hiking/        # 등산 기록
    ├── tracking/      # 실시간 GPS 트래킹 (STOMP + Redis Stream)
    ├── community/     # 자유게시글, 기록게시글, 댓글, 좋아요, 신고
    ├── semofeed/      # 세모피드 (공식 계정 피드, /api/semofeed)
    ├── image/         # MinIO presigned URL
    ├── notification/  # FCM 토큰, 알림, 이벤트
    └── review/        # 산 리뷰
```

## 인증
- Stateless JWT. `JwtFilter`가 `Authorization: Bearer <token>`을 검증한다.
- WebSocket(`/ws/tracking/**`)은 HTTP 필터에서 permitAll이고, `StompAuthChannelInterceptor`가 CONNECT 프레임에서 JWT를 검증한다.
- 퍼블릭: Swagger, OAuth 로그인, 토큰 재발급, `/api/auth/test/login`, `/api/admin/login`, `GET /api/app-version`. 최신 목록은 `SecurityConfig.securityFilterChain()`이 기준이다.

## 인프라

| 컴포넌트 | 용도 |
|---|---|
| PostgreSQL + PostGIS | 메인 DB, 공간 검색 (`hibernate-spatial` + JTS) |
| Redis | refresh token, 블랙리스트, GPS 실시간 통계(Hash), GPS 스트림(Stream), rate limit |
| MinIO | 이미지 저장 (presigned URL) |
| Firebase FCM | 푸시 알림 |

## 프로파일, 마이그레이션
- local, test, prod 모두 Flyway 활성화, `ddl-auto: validate`.
- 스크립트: `src/main/resources/db/migration/V{N}__description.sql`.

## 환경변수
`application.yaml`은 환경변수만 참조한다. 로컬 기본값은 `application-local.yaml`(gitignore)에 있다.
prod 주요 변수: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `JWT_SECRET`, `JWT_ACCESS_TOKEN_EXPIRATION`, `JWT_REFRESH_TOKEN_EXPIRATION`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `KAKAO_REDIRECT_URI`, `KAKAO_ADMIN_KEY`, `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_PUBLIC_URL`, `TRACKING_STREAM_KEY`, `TRACKING_CONSUMER_GROUP`, `FIREBASE_SERVICE_ACCOUNT_PATH`, `DISCORD_ALERT_ENABLED`, `DISCORD_WEBHOOK_URL`

## 배포
GitHub Actions `deploy.yml` → 이미지 태그 갱신 커밋(`chore: update image to ...`) → ArgoCD(`argocd-app.yaml`, `k8s/`).
