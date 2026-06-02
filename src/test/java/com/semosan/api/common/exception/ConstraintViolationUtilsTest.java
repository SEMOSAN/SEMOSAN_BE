package com.semosan.api.common.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintViolationUtilsTest {

    private static final String CONSTRAINT_NAME = "uk_user_blocks_blocker_blocked";

    @Test
    void isViolationReturnsTrueWhenHibernateConstraintNameMatches() {
        ConstraintViolationException cause = new ConstraintViolationException(
                "duplicate",
                new SQLException("unique violation"),
                CONSTRAINT_NAME
        );

        boolean result = ConstraintViolationUtils.isViolation(
                new DataIntegrityViolationException("duplicate", cause),
                CONSTRAINT_NAME
        );

        assertThat(result).isTrue();
    }

    @Test
    void isViolationReturnsTrueWhenMessageContainsExactConstraintToken() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"" + CONSTRAINT_NAME + "\""
        );

        boolean result = ConstraintViolationUtils.isViolation(exception, CONSTRAINT_NAME);

        assertThat(result).isTrue();
    }

    @Test
    void isViolationReturnsFalseWhenMessageContainsOnlyConstraintNamePrefix() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"" + CONSTRAINT_NAME + "_idx\""
        );

        boolean result = ConstraintViolationUtils.isViolation(exception, CONSTRAINT_NAME);

        assertThat(result).isFalse();
    }
}
