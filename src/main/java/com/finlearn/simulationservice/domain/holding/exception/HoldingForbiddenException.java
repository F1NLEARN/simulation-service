package com.finlearn.simulationservice.domain.holding.exception;

import com.finlearn.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class HoldingForbiddenException extends CustomException {

    private static final String CODE = "HOLDING_FORBIDDEN_ACCESS";
    private static final String FIELD = "accountId";
    private static final String MESSAGE = "해당 보유 종목에 접근할 권한이 없습니다.";

    public HoldingForbiddenException() {
        super(CODE, FIELD, MESSAGE, HttpStatus.FORBIDDEN);
    }
}