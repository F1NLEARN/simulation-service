package com.finlearn.simulationservice.domain.holding.exception;

import com.finlearn.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class HoldingDomainException extends CustomException {

    public HoldingDomainException(HoldingErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getField(), errorCode.getMessage(), HttpStatus.BAD_REQUEST);
    }
}