package com.semosan.api.domain.user.policy;

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
public class DefaultUserNameGenerator {

    private static final String ADJECTIVES_KEY = "default-name.adjectives";
    private static final String NOUNS_KEY = "default-name.nouns";
    private static final int NUMBER_BOUND = 10_000;

    private final SecureRandom random = new SecureRandom();
    private final List<String> adjectives;
    private final List<String> nouns;

    public DefaultUserNameGenerator(
            @Value("classpath:user/default-name-pools.properties") Resource namePoolsResource
    ) {
        Properties properties = loadProperties(namePoolsResource);
        this.adjectives = splitValues(properties.getProperty(ADJECTIVES_KEY));
        this.nouns = splitValues(properties.getProperty(NOUNS_KEY));
    }

    public String generate() {
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
            throw new IllegalStateException("기본 이름 리소스를 읽을 수 없습니다.", e);
        }
    }

    private List<String> splitValues(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("기본 이름 리소스 값이 비어 있습니다.");
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
