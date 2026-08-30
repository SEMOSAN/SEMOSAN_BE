package com.semosan.api.domain.user.policy;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.enums.nickname.NicknameCheckResult;
import com.semosan.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class NicknamePolicy {

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9]{2,10}$");
    private static final Pattern JAMO_ONLY_PATTERN = Pattern.compile("^[ㄱ-ㅎㅏ-ㅣ]+$");
    private static final Pattern NUMBER_ONLY_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern CONTACT_PATTERN = Pattern.compile(".*(\\d{2,4}[- ]?\\d{3,4}[- ]?\\d{4}).*");
    private static final Pattern URL_PATTERN = Pattern.compile(".*(https?://|www\\.|\\.com|\\.net|\\.kr|\\.org).*", Pattern.CASE_INSENSITIVE);
    private static final Set<String> RESERVED_NICKNAME_WORDS = Set.of("admin", "official", "운영자", "관리자", "사업자", "세모산");
    private static final Set<String> BLOCKED_WORDS = Set.of(
            "시발",
            "씨발",
            "병신",
            "개새끼",
            "fuck",
            "shit"
    );

    private final UserRepository userRepository;

    // 닉네임 사용 가능 여부를 검증 결과로 반환합니다.
    @Transactional(readOnly = true)
    public NicknameCheckResult check(String nickname) {
        NicknameCheckResult staticResult = checkStaticRules(nickname);
        if (staticResult != NicknameCheckResult.AVAILABLE) {
            return staticResult;
        }
        if (isDuplicated(nickname)) {
            return NicknameCheckResult.DUPLICATED;
        }
        return NicknameCheckResult.AVAILABLE;
    }

    public NicknameCheckResult checkStaticRules(String nickname) {
        String normalizedNickname = nickname.toLowerCase();
        if (!NICKNAME_PATTERN.matcher(nickname).matches()
                || JAMO_ONLY_PATTERN.matcher(nickname).matches()
                || NUMBER_ONLY_PATTERN.matcher(nickname).matches()) {
            return NicknameCheckResult.INVALID_FORMAT;
        }
        // 관리자 사칭 가능성이 있는 표현은 닉네임 일부에 포함되어도 차단합니다.
        if (RESERVED_NICKNAME_WORDS.stream().anyMatch(normalizedNickname::contains)) {
            return NicknameCheckResult.RESERVED;
        }
        if (CONTACT_PATTERN.matcher(nickname).matches()
                || URL_PATTERN.matcher(nickname).matches()
                || BLOCKED_WORDS.stream().anyMatch(normalizedNickname::contains)) {
            return NicknameCheckResult.BLOCKED_WORD;
        }
        return NicknameCheckResult.AVAILABLE;
    }

    @Transactional(readOnly = true)
    public boolean isDuplicated(String nickname) {
        return userRepository.existsByNicknameAndDeletedFalse(nickname);
    }

    // 닉네임을 검증하고 사용할 수 없으면 예외를 발생시킵니다.
    public void validate(String nickname) {
        NicknameCheckResult result = check(nickname);
        switch (result) {
            case AVAILABLE -> {
            }
            case INVALID_FORMAT -> throw new GeneralException(ErrorStatus.INVALID_NICKNAME_FORMAT);
            case RESERVED -> throw new GeneralException(ErrorStatus.NICKNAME_RESERVED);
            case BLOCKED_WORD -> throw new GeneralException(ErrorStatus.NICKNAME_BLOCKED_WORD);
            case DUPLICATED -> throw new GeneralException(ErrorStatus.DUPLICATED_NICKNAME);
        }
    }
}
