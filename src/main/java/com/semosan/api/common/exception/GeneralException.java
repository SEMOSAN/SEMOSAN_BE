package com.semosan.api.common.exception;

import com.semosan.api.common.base.BaseStatus;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

    private final BaseStatus errorStatus;

    public GeneralException(BaseStatus errorStatus) {
        super(errorStatus.getMessage());
        validateErrorStatus(errorStatus);
        this.errorStatus = errorStatus;
    }

    public GeneralException(BaseStatus errorStatus, Throwable cause) {
        super(errorStatus.getMessage(), cause);
        validateErrorStatus(errorStatus);
        this.errorStatus = errorStatus;
    }

    private static void validateErrorStatus(BaseStatus errorStatus) {
        if (errorStatus == null) {
            throw new IllegalArgumentException("errorStatus must not be null");
        }
    }

}
