package com.finlearn.simulationservice.domain.investment.exception;

import com.finlearn.common.exception.CustomException;

public class InvestmentException extends CustomException {

    private final InvestmentErrorCode errorCode;

    public InvestmentException(InvestmentErrorCode errorCode) {
        super(errorCode.getCode(), null, errorCode.getMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
    }

    public InvestmentErrorCode getErrorCode() {
        return errorCode;
    }
}
