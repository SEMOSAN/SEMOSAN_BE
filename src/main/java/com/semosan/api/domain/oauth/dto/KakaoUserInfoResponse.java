package com.semosan.api.domain.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfoResponse(

        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {
    public record KakaoAccount(

            String email,

            @JsonProperty("profile")
            Profile profile
    ) {
        public record Profile(

                String nickname,

                @JsonProperty("profile_image_url")
                String profileImageUrl
        ) {}
    }
}
