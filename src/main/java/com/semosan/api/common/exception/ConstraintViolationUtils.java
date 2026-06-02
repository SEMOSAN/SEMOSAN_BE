package com.semosan.api.common.exception;

import org.hibernate.exception.ConstraintViolationException;

public final class ConstraintViolationUtils {

    private ConstraintViolationUtils() {}

    /**
     * 예외 체인 전체를 탐색해 특정 DB 제약 조건 이름이 원인인지 확인한다.
     * Hibernate ConstraintViolationException의 constraintName과 메시지 문자열을 모두 검사해
     * 드라이버마다 다른 예외 래핑 방식에 대응한다.
     */
    public static boolean isViolation(Throwable exception, String constraintName) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && constraintName.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
