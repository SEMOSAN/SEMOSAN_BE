package com.semosan.api.domain.user.policy;

import com.semosan.api.domain.user.enums.nickname.NicknameCheckResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DefaultNicknameGenerator {

    private static final String ADJECTIVES_KEY = "default-nickname.adjectives";
    private static final String NOUNS_KEY = "default-nickname.nouns";
    private static final int NUMBER_BOUND = 10_000;
    private static final int MAX_ATTEMPTS = 100;

    private final NicknamePolicy nicknamePolicy;
    private final List<String> nicknamePrefixes;

    public DefaultNicknameGenerator(
            NicknamePolicy nicknamePolicy,
            @Value("classpath:user/default-nickname-pools.properties") Resource nicknamePoolsResource
    ) {
        this.nicknamePolicy = nicknamePolicy;
        Properties properties = loadProperties(nicknamePoolsResource);
        List<String> adjectives = splitValues(properties.getProperty(ADJECTIVES_KEY));
        List<String> nouns = splitValues(properties.getProperty(NOUNS_KEY));
        this.nicknamePrefixes = buildValidNicknamePrefixes(adjectives, nouns);
    }

    public String generate() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String nickname = randomNickname();
            if (!nicknamePolicy.isDuplicated(nickname)) {
                return nickname;
            }
        }
        throw new IllegalStateException("사용 가능한 기본 닉네임을 생성할 수 없습니다.");
    }

    private String randomNickname() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String prefix = nicknamePrefixes.get(random.nextInt(nicknamePrefixes.size()));
        int number = random.nextInt(NUMBER_BOUND);
        return "%s%04d".formatted(prefix, number);
    }

    private List<String> buildValidNicknamePrefixes(List<String> adjectives, List<String> nouns) {
        List<String> prefixes = new ArrayList<>();
        for (String adjective : adjectives) {
            for (String noun : nouns) {
                String nicknameCandidate = "%s%s0000".formatted(adjective, noun);
                if (nicknamePolicy.checkStaticRules(nicknameCandidate) == NicknameCheckResult.AVAILABLE) {
                    prefixes.add(adjective + noun);
                }
            }
        }
        if (prefixes.isEmpty()) {
            throw new IllegalStateException("사용 가능한 기본 닉네임 조합이 없습니다.");
        }
        return List.copyOf(prefixes);
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
