package com.semosan.api.domain.tracking.websocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdPrincipalTest {

    @Test
    void getNameReturnsUserIdAsString() {
        UserIdPrincipal principal = new UserIdPrincipal(123L);

        assertThat(principal.getUserId()).isEqualTo(123L);
        assertThat(principal.getName()).isEqualTo("123");
    }
}
