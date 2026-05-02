package com.finlearn.simulationservice.domain.holding.exception;

import com.finlearn.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class HoldingNotFoundException extends CustomException {

    private static final String CODE = "HOLDING_NOT_FOUND";
    private static final String FIELD = "holdingId";
    private static final String MESSAGE = "보유 종목을 찾을 수 없습니다.";

    public HoldingNotFoundException() {
        super(CODE, FIELD, MESSAGE, HttpStatus.NOT_FOUND);
    }
}