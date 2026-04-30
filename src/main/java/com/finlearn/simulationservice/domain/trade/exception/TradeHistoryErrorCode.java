package com.finlearn.simulationservice.domain.trade.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TradeHistoryErrorCode {

    INVALID_ACCOUNT_ID("TH_INVALID_ACCOUNT_ID", "accountId", "accountId는 null일 수 없습니다."),
    INVALID_SEASON_ID("TH_INVALID_SEASON_ID", "seasonId", "seasonId는 null일 수 없습니다."),
    INVALID_SEASON_NUMBER("TH_INVALID_SEASON_NUMBER", "seasonNumber", "seasonNumber는 0보다 커야 합니다."),
    INVALID_INSTRUMENT_CODE("TH_INVALID_INSTRUMENT_CODE", "instrumentCode", "instrumentCode는 blank일 수 없습니다."),
    INVALID_TRADE_TYPE("TH_INVALID_TRADE_TYPE", "tradeType", "tradeType은 null일 수 없습니다."),
    INVALID_QUANTITY("TH_INVALID_QUANTITY", "quantity", "거래 수량은 0보다 커야 합니다."),
    INVALID_TRADE_PRICE("TH_INVALID_TRADE_PRICE", "tradePrice", "거래 가격은 0보다 커야 합니다."),
    INVALID_TRADE_AT("TH_INVALID_TRADE_AT", "tradeAt", "거래 일시는 null일 수 없습니다."),
    INVALID_CASH_BALANCE("TH_INVALID_CASH_BALANCE", "cashBalanceAfterTrade", "거래 후 현금 잔액은 0 이상이어야 합니다.");

    private final String code;
    private final String field;
    private final String message;
}