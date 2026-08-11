package com.semosan.api.domain.hiking.policy;

import com.semosan.api.domain.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 자유기록의 기본 이름을 만든다 — 사용자가 이름을 입력하지 않고 종료한 경우.
 *
 * 형식: {yyMMdd}_{표시이름}의코스{N}   예) 260723_등산왕의코스1
 *
 * N 은 같은 날 몇 번째 자유기록인지다. 날짜만으로는 같은 날 두 번 기록했을 때
 * 이름이 완전히 겹치므로(실수로 종료 후 재시작하는 경우가 흔하다) 번호로 구분한다.
 *
 * 표시 이름은 {@link User#displayName()} 을 쓴다 — 닉네임은 nullable 이고
 * 탈퇴 시 null 로 밀리므로, 닉네임 → 실명 → "사용자" 순 fallback 이 이미 구현돼 있다.
 */
@Component
public class DefaultRecordNameGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");
    /** hiking_records.name 컬럼 길이와 맞춘다. */
    private static final int MAX_LENGTH = 100;
    private static final String SUFFIX_PREFIX = "의코스";
    private static final String DELIMITER = "_";

    public String generate(User user, LocalDateTime hikedAt, long sequence) {
        String datePart = DATE_FORMAT.format(hikedAt);
        String suffix = SUFFIX_PREFIX + sequence;

        // 표시 이름이 실명으로 fallback 되면 길이 상한이 없다.
        // 잘라내더라도 날짜와 순번은 살려야 구분이 가능하므로 이름 부분만 줄인다.
        int available = MAX_LENGTH - datePart.length() - DELIMITER.length() - suffix.length();
        String displayName = user.displayName();
        if (displayName.length() > available) {
            displayName = displayName.substring(0, Math.max(available, 0));
        }
        return datePart + DELIMITER + displayName + suffix;
    }
}
