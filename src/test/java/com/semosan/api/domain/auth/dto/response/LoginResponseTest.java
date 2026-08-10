package com.semosan.api.domain.auth.dto.response;

import com.semosan.api.common.jwt.TokenIssuance;
import com.semosan.api.domain.user.entity.User;
import com.semosan.api.domain.user.enums.user.OnboardingStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginResponseTest {

    @Test
    void ofReturnsOnboardingCompletedTrueWhenUserStatusIsComplete() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getOnboardingStatus()).thenReturn(OnboardingStatus.COMPLETE);

        LoginResponse response = LoginResponse.of(user, new TokenIssuance("access", "refresh"));

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.onboardingCompleted()).isTrue();
    }

    @Test
    void ofReturnsOnboardingCompletedFalseWhenUserStatusIsIncomplete() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getOnboardingStatus()).thenReturn(OnboardingStatus.INCOMPLETE);

        LoginResponse response = LoginResponse.of(user, new TokenIssuance("access", "refresh"));

        assertThat(response.onboardingCompleted()).isFalse();
    }
}
