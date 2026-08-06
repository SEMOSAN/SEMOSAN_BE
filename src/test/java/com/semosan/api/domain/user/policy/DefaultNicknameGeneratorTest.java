package com.semosan.api.domain.user.policy;

import com.semosan.api.domain.user.enums.nickname.NicknameCheckResult;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultNicknameGeneratorTest {

    @Test
    void generateReturnsNonDuplicatedNicknameFromConfiguredPools() {
        NicknamePolicy nicknamePolicy = mock(NicknamePolicy.class);
        when(nicknamePolicy.checkStaticRules("빠른산0000")).thenReturn(NicknameCheckResult.AVAILABLE);
        when(nicknamePolicy.isDuplicated(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        DefaultNicknameGenerator generator = new DefaultNicknameGenerator(
                nicknamePolicy,
                resource("""
                        default-nickname.adjectives=빠른
                        default-nickname.nouns=산
                        """)
        );

        String nickname = generator.generate();

        assertThat(nickname).startsWith("빠른산");
        assertThat(nickname).hasSize("빠른산0000".length());
    }

    @Test
    void generateThrowsWhenAllAttemptsAreDuplicated() {
        NicknamePolicy nicknamePolicy = mock(NicknamePolicy.class);
        when(nicknamePolicy.checkStaticRules("빠른산0000")).thenReturn(NicknameCheckResult.AVAILABLE);
        when(nicknamePolicy.isDuplicated(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        DefaultNicknameGenerator generator = new DefaultNicknameGenerator(
                nicknamePolicy,
                resource("""
                        default-nickname.adjectives=빠른
                        default-nickname.nouns=산
                        """)
        );

        assertThatThrownBy(generator::generate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용 가능한 기본 닉네임을 생성할 수 없습니다.");
    }

    @Test
    void constructorTrimsValuesAndIgnoresBlankItems() {
        NicknamePolicy nicknamePolicy = mock(NicknamePolicy.class);
        when(nicknamePolicy.checkStaticRules("빠른산0000")).thenReturn(NicknameCheckResult.AVAILABLE);
        when(nicknamePolicy.isDuplicated(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        DefaultNicknameGenerator generator = new DefaultNicknameGenerator(
                nicknamePolicy,
                resource("""
                        default-nickname.adjectives= 빠른 ,
                        default-nickname.nouns= 산 ,
                        """)
        );

        String nickname = generator.generate();

        assertThat(nickname).startsWith("빠른산");
    }

    @Test
    void constructorThrowsWhenPoolsAreBlank() {
        NicknamePolicy nicknamePolicy = mock(NicknamePolicy.class);

        assertThatThrownBy(() -> new DefaultNicknameGenerator(
                nicknamePolicy,
                resource("default-nickname.adjectives=\ndefault-nickname.nouns=산")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("기본 닉네임 리소스 값이 비어 있습니다.");
    }

    @Test
    void constructorThrowsWhenNoValidPrefixExists() {
        NicknamePolicy nicknamePolicy = mock(NicknamePolicy.class);
        when(nicknamePolicy.checkStaticRules("나쁜말0000")).thenReturn(NicknameCheckResult.BLOCKED_WORD);

        assertThatThrownBy(() -> new DefaultNicknameGenerator(
                nicknamePolicy,
                resource("""
                        default-nickname.adjectives=나쁜
                        default-nickname.nouns=말
                        """)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("사용 가능한 기본 닉네임 조합이 없습니다.");
    }

    @Test
    void constructorThrowsWhenResourceCannotBeRead() {
        NicknamePolicy nicknamePolicy = mock(NicknamePolicy.class);

        assertThatThrownBy(() -> new DefaultNicknameGenerator(
                nicknamePolicy,
                new ByteArrayResource(new byte[0]) {
                    @Override
                    public java.io.InputStream getInputStream() throws IOException {
                        throw new IOException("broken");
                    }
                }
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("기본 닉네임 리소스를 읽을 수 없습니다.");
    }

    private ByteArrayResource resource(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
    }
}
