package com.finlearn.simulationservice.domain.tradehistory.exception;

import com.finlearn.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TradeHistoryForbiddenException extends CustomException {

    private static final String CODE = "TH_FORBIDDEN_ACCESS";
    private static final String FIELD = "accountId";
    private static final String MESSAGE = "해당 거래 내역에 접근할 권한이 없습니다.";

    public TradeHistoryForbiddenException() {
        super(CODE, FIELD, MESSAGE, HttpStatus.FORBIDDEN);
    }
}