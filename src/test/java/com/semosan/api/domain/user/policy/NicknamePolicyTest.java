package com.semosan.api.domain.user.policy;

import com.semosan.api.common.exception.GeneralException;
import com.semosan.api.common.status.ErrorStatus;
import com.semosan.api.domain.user.enums.nickname.NicknameCheckResult;
import com.semosan.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NicknamePolicyTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NicknamePolicy nicknamePolicy;

    @Test
    void checkStaticRulesReturnsAvailableForValidNickname() {
        assertThat(nicknamePolicy.checkStaticRules("푸름123")).isEqualTo(NicknameCheckResult.AVAILABLE);
    }

    @Test
    void checkStaticRulesRejectsInvalidFormat() {
        assertThat(nicknamePolicy.checkStaticRules("a")).isEqualTo(NicknameCheckResult.INVALID_FORMAT);
        assertThat(nicknamePolicy.checkStaticRules("ㅋㅋㅋ")).isEqualTo(NicknameCheckResult.INVALID_FORMAT);
        assertThat(nicknamePolicy.checkStaticRules("1234")).isEqualTo(NicknameCheckResult.INVALID_FORMAT);
    }

    @Test
    void checkStaticRulesRejectsReservedAndBlockedWords() {
        assertThat(nicknamePolicy.checkStaticRules("admin1")).isEqualTo(NicknameCheckResult.RESERVED);
        assertThat(nicknamePolicy.checkStaticRules("Official1")).isEqualTo(NicknameCheckResult.RESERVED);
        assertThat(nicknamePolicy.checkStaticRules("01012345678")).isEqualTo(NicknameCheckResult.INVALID_FORMAT);
        assertThat(nicknamePolicy.checkStaticRules("fuckyou")).isEqualTo(NicknameCheckResult.BLOCKED_WORD);
    }

    @Test
    void checkStaticRulesRejectsKoreanBlockedWord() {
        assertThat(nicknamePolicy.checkStaticRules("시발놈")).isEqualTo(NicknameCheckResult.BLOCKED_WORD);
    }

    @Test
    void checkStaticRulesRejectsContactAndUrlByFormatBeforeBlockedWordCheck() {
        assertThat(nicknamePolicy.checkStaticRules("연락01012345678")).isEqualTo(NicknameCheckResult.INVALID_FORMAT);
        assertThat(nicknamePolicy.checkStaticRules("abc.com")).isEqualTo(NicknameCheckResult.INVALID_FORMAT);
    }

    @Test
    void checkReturnsStaticErrorWithoutCheckingDuplication() {
        assertThat(nicknamePolicy.check("a")).isEqualTo(NicknameCheckResult.INVALID_FORMAT);
    }

    @Test
    void checkReturnsAvailableWhenStaticRulesPassAndNicknameIsNotDuplicated() {
        when(userRepository.existsByNicknameAndDeletedFalse("푸름")).thenReturn(false);

        assertThat(nicknamePolicy.check("푸름")).isEqualTo(NicknameCheckResult.AVAILABLE);
    }

    @Test
    void validateDoesNotThrowWhenNicknameIsAvailable() {
        when(userRepository.existsByNicknameAndDeletedFalse("푸름")).thenReturn(false);

        nicknamePolicy.validate("푸름");
    }

    @Test
    void checkReturnsDuplicatedWhenRepositoryFindsNickname() {
        when(userRepository.existsByNicknameAndDeletedFalse("푸름")).thenReturn(true);

        assertThat(nicknamePolicy.check("푸름")).isEqualTo(NicknameCheckResult.DUPLICATED);
    }

    @Test
    void validateThrowsMappedErrors() {
        assertThatThrownBy(() -> nicknamePolicy.validate("a"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.INVALID_NICKNAME_FORMAT);
        assertThatThrownBy(() -> nicknamePolicy.validate("admin1"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.NICKNAME_RESERVED);
        assertThatThrownBy(() -> nicknamePolicy.validate("fuckyou"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.NICKNAME_BLOCKED_WORD);
    }

    @Test
    void validateThrowsDuplicatedNickname() {
        when(userRepository.existsByNicknameAndDeletedFalse("푸름")).thenReturn(true);

        assertThatThrownBy(() -> nicknamePolicy.validate("푸름"))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus")
                .isEqualTo(ErrorStatus.DUPLICATED_NICKNAME);
    }
}
