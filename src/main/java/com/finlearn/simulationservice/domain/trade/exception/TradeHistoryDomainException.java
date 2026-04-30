package com.finlearn.simulationservice.domain.trade.exception;

import com.finlearn.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TradeHistoryDomainException extends CustomException {

    public TradeHistoryDomainException(TradeHistoryErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getField(), errorCode.getMessage(), HttpStatus.BAD_REQUEST);
    }
}