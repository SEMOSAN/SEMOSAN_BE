package com.semosan.api.common.exception;

import com.semosan.api.common.status.ErrorStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneralExceptionTest {

    @Test
    void constructorUsesErrorStatusMessage() {
        GeneralException exception = new GeneralException(ErrorStatus.BAD_REQUEST);

        assertThat(exception.getErrorStatus()).isEqualTo(ErrorStatus.BAD_REQUEST);
        assertThat(exception.getMessage()).isEqualTo(ErrorStatus.BAD_REQUEST.getMessage());
    }

    @Test
    void constructorWithCauseKeepsCause() {
        RuntimeException cause = new RuntimeException("cause");

        GeneralException exception = new GeneralException(ErrorStatus.INTERNAL_SERVER_ERROR, cause);

        assertThat(exception.getErrorStatus()).isEqualTo(ErrorStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorThrowsWhenErrorStatusIsNull() {
        assertThatThrownBy(() -> new GeneralException(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("errorStatus must not be null");
    }
}
