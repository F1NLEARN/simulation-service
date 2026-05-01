package com.finlearn.simulationservice.application.investment.service;

import com.finlearn.simulationservice.application.investment.dto.response.TradeHistoryListResponse;
import com.finlearn.simulationservice.application.investment.dto.response.TradeHistoryResponse;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.trade.entity.TradeHistory;
import com.finlearn.simulationservice.domain.trade.enums.TradeType;
import com.finlearn.simulationservice.domain.trade.repository.TradeHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestmentTradeService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final TradeHistoryRepository tradeHistoryRepository;

    public TradeHistoryListResponse getTradeHistories(
            UUID userId,
            String stockCode,
            String tradeType,
            int page,
            int size
    ) {
        InvestmentAccount account = investmentAccountRepository.findByUserIdAndStatus(userId, InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        String normalizedStockCode = normalizeStockCode(stockCode);
        TradeType parsedTradeType = parseTradeType(tradeType);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "tradeAt"));
        Page<TradeHistory> tradePage = tradeHistoryRepository.findByAccountIdWithFilters(
                account.getInvestmentAccountId(),
                normalizedStockCode,
                parsedTradeType,
                pageable
        );

        return new TradeHistoryListResponse(
                tradePage.getContent().stream().map(this::toResponse).toList(),
                tradePage.getNumber(),
                tradePage.getSize(),
                tradePage.getTotalElements(),
                tradePage.getTotalPages(),
                tradePage.hasNext()
        );
    }

    private TradeHistoryResponse toResponse(TradeHistory tradeHistory) {
        return new TradeHistoryResponse(
                tradeHistory.getTradeHistoryId(),
                tradeHistory.getInstrumentCode(),
                tradeHistory.getTradeType().name(),
                tradeHistory.getQuantity(),
                toMoney(tradeHistory.getTradePrice()),
                toMoney(tradeHistory.getTotalTradeAmount()),
                toMoney(tradeHistory.getCashBalanceAfterTrade()),
                tradeHistory.getTradeAt()
        );
    }

    private String normalizeStockCode(String stockCode) {
        if (stockCode == null || stockCode.isBlank()) {
            return null;
        }
        return stockCode.trim().toUpperCase();
    }

    private TradeType parseTradeType(String tradeType) {
        if (tradeType == null || tradeType.isBlank()) {
            return null;
        }
        try {
            return TradeType.valueOf(tradeType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_TRADE_TYPE);
        }
    }

    private BigDecimal toMoney(long amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }
}
