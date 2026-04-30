package com.finlearn.simulationservice.domain.holding.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HoldingErrorCode {

    INVALID_ACCOUNT_ID("HOLDING_INVALID_ACCOUNT_ID", "accountId", "accountId는 null일 수 없습니다."),
    INVALID_HOLDING_NAME("HOLDING_INVALID_NAME", "holdingName", "holdingName은 blank일 수 없습니다."),
    INVALID_SEASON_ID("HOLDING_INVALID_SEASON_ID", "seasonId", "seasonId는 null일 수 없습니다."),
    INVALID_SEASON_NUMBER("HOLDING_INVALID_SEASON_NUMBER", "seasonNumber", "seasonNumber는 0보다 커야 합니다."),
    INVALID_INSTRUMENT_CODE("HOLDING_INVALID_INSTRUMENT_CODE", "instrumentCode", "instrumentCode는 blank일 수 없습니다."),
    INVALID_QUANTITY("HOLDING_INVALID_QUANTITY", "quantity", "quantity는 0 이상이어야 합니다."),
    INVALID_AVERAGE_BUY_PRICE("HOLDING_INVALID_AVG_BUY_PRICE", "averageBuyPrice", "averageBuyPrice는 0 이상이어야 합니다."),
    INVALID_CURRENT_PRICE("HOLDING_INVALID_CURRENT_PRICE", "currentPrice", "currentPrice는 0보다 커야 합니다."),
    INVALID_BUY_QUANTITY("HOLDING_INVALID_BUY_QUANTITY", "buyQuantity", "추가 매수 수량은 0보다 커야 합니다."),
    INVALID_BUY_PRICE("HOLDING_INVALID_BUY_PRICE", "buyPrice", "매수 가격은 0보다 커야 합니다."),
    INVALID_SELL_QUANTITY("HOLDING_INVALID_SELL_QUANTITY", "sellQuantity", "매도 수량은 0보다 커야 합니다."),
    EXCEED_SELL_QUANTITY("HOLDING_EXCEED_SELL_QUANTITY", "sellQuantity", "매도 수량이 보유 수량을 초과할 수 없습니다."),
    INVALID_UPDATE_PRICE("HOLDING_INVALID_UPDATE_PRICE", "currentPrice", "현재가는 0보다 커야 합니다.");

    private final String code;
    private final String field;
    private final String message;
}