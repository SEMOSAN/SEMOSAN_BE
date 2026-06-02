package com.semosan.api.common.exception;

import org.hibernate.exception.ConstraintViolationException;

import java.util.regex.Pattern;

public final class ConstraintViolationUtils {

    private ConstraintViolationUtils() {}

    public static boolean isViolation(Throwable exception, String constraintName) {
        Pattern constraintNamePattern = Pattern.compile(
                "(?<![A-Za-z0-9_])" + Pattern.quote(constraintName) + "(?![A-Za-z0-9_])"
        );
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && constraintName.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            String message = current.getMessage();
            // 드라이버마다 다른 예외 래핑에 대응하되, 다른 제약명 일부와 겹치는 오탐은 막는다.
            if (message != null && constraintNamePattern.matcher(message).find()) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
