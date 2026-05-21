package com.semosan.api.domain.user.policy;

import com.semosan.api.domain.user.enums.nickname.NicknameCheckResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Component
public class DefaultNicknameGenerator {

    private static final String ADJECTIVES_KEY = "default-nickname.adjectives";
    private static final String NOUNS_KEY = "default-nickname.nouns";
    private static final int NUMBER_BOUND = 10_000;
    private static final int MAX_ATTEMPTS = 100;

    private final SecureRandom random = new SecureRandom();
    private final NicknamePolicy nicknamePolicy;
    private final List<String> adjectives;
    private final List<String> nouns;

    public DefaultNicknameGenerator(
            NicknamePolicy nicknamePolicy,
            @Value("classpath:user/default-nickname-pools.properties") Resource nicknamePoolsResource
    ) {
        this.nicknamePolicy = nicknamePolicy;
        Properties properties = loadProperties(nicknamePoolsResource);
        this.adjectives = splitValues(properties.getProperty(ADJECTIVES_KEY));
        this.nouns = splitValues(properties.getProperty(NOUNS_KEY));
    }

    public String generate() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String nickname = randomNickname();
            if (nicknamePolicy.check(nickname) == NicknameCheckResult.AVAILABLE) {
                return nickname;
            }
        }
        throw new IllegalStateException("사용 가능한 기본 닉네임을 생성할 수 없습니다.");
    }

    private String randomNickname() {
        String adjective = adjectives.get(random.nextInt(adjectives.size()));
        String noun = nouns.get(random.nextInt(nouns.size()));
        int number = random.nextInt(NUMBER_BOUND);
        return "%s%s%04d".formatted(adjective, noun, number);
    }

    private Properties loadProperties(Resource resource) {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("기본 닉네임 리소스를 읽을 수 없습니다.", e);
        }
    }

    private List<String> splitValues(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("기본 닉네임 리소스 값이 비어 있습니다.");
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
