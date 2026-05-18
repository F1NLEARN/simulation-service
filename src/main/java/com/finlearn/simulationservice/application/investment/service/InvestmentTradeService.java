package com.finlearn.simulationservice.application.investment.service;

import com.finlearn.simulationservice.application.investment.dto.response.TradeHistoryListResponse;
import com.finlearn.simulationservice.application.investment.dto.response.TradeHistoryResponse;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeHistory;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeType;
import com.finlearn.simulationservice.domain.tradehistory.repository.TradeHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final StockItemRepository stockItemRepository;

    public TradeHistoryListResponse getTradeHistories(
            UUID userId,
            String stockCode,
            String tradeType,
            int page,
            int size
    ) {
        InvestmentAccount account = investmentAccountRepository.findByParticipant_InvestorIdAndStatus(userId, InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        String normalizedStockCode = normalizeStockCode(stockCode);
        TradeType parsedTradeType = parseTradeType(tradeType);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "tradeAt"));
        Page<TradeHistory> tradePage = tradeHistoryRepository.findAllWithFilters(
                account.getAccountId(),
                parsedTradeType,
                null,
                normalizedStockCode,
                pageable
        );

        List<TradeHistory> trades = tradePage.getContent();
        List<String> stockCodes = trades.stream()
                .map(t -> t.getInstrumentCode().getValue())
                .distinct()
                .toList();
        Map<String, String> stockNameMap = stockItemRepository.findAllByStockCodeIn(stockCodes).stream()
                .collect(Collectors.toMap(StockItem::getStockCode, StockItem::getStockName));

        return new TradeHistoryListResponse(
                trades.stream().map(t -> toResponse(t, stockNameMap)).toList(),
                tradePage.getNumber(),
                tradePage.getSize(),
                tradePage.getTotalElements(),
                tradePage.getTotalPages(),
                tradePage.hasNext()
        );
    }

    private TradeHistoryResponse toResponse(TradeHistory tradeHistory, Map<String, String> stockNameMap) {
        String code = tradeHistory.getInstrumentCode().getValue();
        return new TradeHistoryResponse(
                tradeHistory.getTradeHistoryId(),
                code,
                stockNameMap.getOrDefault(code, code),
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
