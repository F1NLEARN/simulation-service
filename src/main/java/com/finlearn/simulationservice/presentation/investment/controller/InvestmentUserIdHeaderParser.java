package com.finlearn.simulationservice.presentation.investment.controller;

import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import java.util.UUID;

public final class InvestmentUserIdHeaderParser {

    private InvestmentUserIdHeaderParser() {
    }

    public static UUID parse(String rawUserId) {
        if (rawUserId == null || rawUserId.isBlank()) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_USER_ID);
        }

        try {
            return UUID.fromString(rawUserId.trim());
        } catch (IllegalArgumentException e) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_USER_ID);
        }
    }
}
