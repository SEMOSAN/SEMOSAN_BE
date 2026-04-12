package com.semosan.api.common.exception;

import com.semosan.api.common.base.BaseStatus;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

    private final BaseStatus errorStatus;

    public GeneralException(BaseStatus errorStatus) {
        super(validated(errorStatus).getMessage());
        this.errorStatus = errorStatus;
    }

    public GeneralException(BaseStatus errorStatus, Throwable cause) {
        super(validated(errorStatus).getMessage(), cause);
        this.errorStatus = errorStatus;
    }

    private static BaseStatus validated(BaseStatus errorStatus) {
        if (errorStatus == null) {
            throw new IllegalArgumentException("errorStatus must not be null");
        }
        return errorStatus;
    }

}
