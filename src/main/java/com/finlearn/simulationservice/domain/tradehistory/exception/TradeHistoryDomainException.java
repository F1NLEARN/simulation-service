package com.finlearn.simulationservice.domain.tradehistory.exception;

import com.finlearn.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TradeHistoryDomainException extends CustomException {

    public TradeHistoryDomainException(TradeHistoryErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getField(), errorCode.getMessage(), HttpStatus.BAD_REQUEST);
    }
}