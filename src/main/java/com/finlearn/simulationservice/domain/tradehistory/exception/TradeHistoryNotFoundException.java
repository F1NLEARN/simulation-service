package com.finlearn.simulationservice.domain.tradehistory.exception;

import com.finlearn.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TradeHistoryNotFoundException extends CustomException {

    private static final String CODE = "TH_NOT_FOUND";
    private static final String FIELD = "tradeHistoryId";
    private static final String MESSAGE = "거래 내역을 찾을 수 없습니다.";

    public TradeHistoryNotFoundException() {
        super(CODE, FIELD, MESSAGE, HttpStatus.NOT_FOUND);
    }
}