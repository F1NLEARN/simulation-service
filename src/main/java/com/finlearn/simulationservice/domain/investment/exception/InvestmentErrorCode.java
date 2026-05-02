package com.finlearn.simulationservice.domain.investment.exception;

import org.springframework.http.HttpStatus;

public enum InvestmentErrorCode {
    INVALID_ACCOUNT_STATUS(HttpStatus.CONFLICT, "INVEST_001", "투자계좌 상태가 올바르지 않습니다."),
    INSUFFICIENT_CASH(HttpStatus.BAD_REQUEST, "INVEST_002", "예수금이 부족합니다."),
    INSUFFICIENT_HOLDING_QUANTITY(HttpStatus.BAD_REQUEST, "INVEST_003", "보유 수량이 부족합니다."),
    HOLDING_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "INVEST_004", "보유 종목을 찾을 수 없습니다."),
    INVALID_ORDER_QUANTITY(HttpStatus.BAD_REQUEST, "INVEST_005", "주문 수량은 1 이상이어야 합니다."),
    INVALID_STOCK_PRICE(HttpStatus.BAD_REQUEST, "INVEST_006", "주문 단가는 0보다 커야 합니다."),
    INVALID_SEED_MONEY(HttpStatus.BAD_REQUEST, "INVEST_007", "시드머니는 0 이상이어야 합니다."),
    INVESTMENT_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "INVEST_008", "투자계좌를 찾을 수 없습니다."),
    STOCK_PRICE_NOT_FOUND(HttpStatus.NOT_FOUND, "INVEST_009", "현재 시세를 찾을 수 없습니다."),
    FAVORITE_STOCK_ALREADY_EXISTS(HttpStatus.CONFLICT, "INVEST_010", "이미 등록된 관심 종목입니다."),
    FAVORITE_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "INVEST_011", "삭제할 관심 종목이 없습니다."),
    INVALID_ASSET_TYPE(HttpStatus.BAD_REQUEST, "INVEST_012", "지원하지 않는 자산 유형입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    InvestmentErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
