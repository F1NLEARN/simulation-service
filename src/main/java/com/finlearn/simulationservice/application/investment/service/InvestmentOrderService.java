package com.finlearn.simulationservice.application.investment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.application.investment.dto.request.BuyOrderRequest;
import com.finlearn.simulationservice.application.investment.dto.request.SellOrderRequest;
import com.finlearn.simulationservice.application.investment.dto.response.BuyStockResponse;
import com.finlearn.simulationservice.application.investment.dto.response.SellStockResponse;
import com.finlearn.simulationservice.domain.holding.command.CreateHoldingCommand;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.enums.StockPriceSource;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockPriceRepository;
import com.finlearn.simulationservice.domain.outbox.entity.OutboxEvent;
import com.finlearn.simulationservice.domain.outbox.repository.OutboxEventRepository;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeHistory;
import com.finlearn.simulationservice.domain.tradehistory.repository.TradeHistoryRepository;
import com.finlearn.simulationservice.infrastructure.kafka.KafkaTopics;
import com.finlearn.simulationservice.infrastructure.kafka.event.PortfolioSnapshotEvent;
import com.finlearn.simulationservice.infrastructure.kafka.event.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestmentOrderService {

    // TODO: Season 도메인 연동 후 실제 seasonId/seasonNumber를 조회하도록 변경
    private static final UUID MVP_DEFAULT_SEASON_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int MVP_DEFAULT_SEASON_NUMBER = 1;

    private final InvestmentAccountRepository investmentAccountRepository;
    private final StockItemRepository stockItemRepository;
    private final StockPriceRepository stockPriceRepository;
    private final HoldingRepository holdingRepository;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public BuyStockResponse buy(UUID userId, BuyOrderRequest request) {
        if (request.quantity() <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ORDER_QUANTITY);
        }

        String normalizedStockCode = normalizeStockCode(request.stockCode());
        StockItem stockItem = stockItemRepository.findByStockCode(normalizedStockCode)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_ITEM_NOT_FOUND));

        Long currentPrice = resolveCurrentPriceAndCache(normalizedStockCode, stockItem)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_NOT_TRADABLE));

        InvestmentAccount account = investmentAccountRepository.findByInvestorIdAndStatusForUpdate(userId, InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        long totalAmount = currentPrice * request.quantity();

        account.buy(normalizedStockCode, stockItem.getStockName(), request.quantity(), currentPrice, LocalDateTime.now());
        investmentAccountRepository.save(account);

        Holding holding = holdingRepository.findByAccountIdAndInstrumentCode(account.getAccountId(), normalizedStockCode)
                .map(existing -> {
                    existing.updateCurrentPrice(currentPrice);
                    existing.addBuy(request.quantity(), currentPrice);
                    return existing;
                })
                .orElseGet(() -> Holding.create(new CreateHoldingCommand(
                        account.getAccountId(),
                        stockItem.getStockName(),
                        MVP_DEFAULT_SEASON_ID,
                        MVP_DEFAULT_SEASON_NUMBER,
                        normalizedStockCode,
                        request.quantity(),
                        currentPrice,
                        currentPrice
                )));
        holdingRepository.save(holding);

        LocalDateTime executedAt = LocalDateTime.now();
        long cashBalanceAfterTrade = account.getCurrentCashBalance();
        TradeHistory tradeHistory = TradeHistory.buy(
                account.getAccountId(),
                MVP_DEFAULT_SEASON_ID,
                MVP_DEFAULT_SEASON_NUMBER,
                normalizedStockCode,
                request.quantity(),
                currentPrice,
                executedAt,
                cashBalanceAfterTrade
        );
        tradeHistoryRepository.save(tradeHistory);

        saveOutboxEvents(userId, account, stockItem, normalizedStockCode, "BUY", executedAt);

        return new BuyStockResponse(
                account.getAccountId(),
                normalizedStockCode,
                stockItem.getStockName(),
                "BUY",
                request.quantity(),
                currentPrice,
                totalAmount,
                cashBalanceAfterTrade
        );
    }

    @Transactional
    public SellStockResponse sell(UUID userId, SellOrderRequest request) {
        if (request.quantity() <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ORDER_QUANTITY);
        }

        String normalizedStockCode = normalizeStockCode(request.stockCode());
        StockItem stockItem = stockItemRepository.findByStockCode(normalizedStockCode)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_ITEM_NOT_FOUND));

        Long currentPrice = resolveCurrentPriceAndCache(normalizedStockCode, stockItem)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_NOT_TRADABLE));

        InvestmentAccount account = investmentAccountRepository.findByInvestorIdAndStatusForUpdate(userId, InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        Holding holding = holdingRepository.findByAccountIdAndInstrumentCode(account.getAccountId(), normalizedStockCode)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.HOLDING_STOCK_NOT_FOUND));

        if (holding.getQuantity() < request.quantity()) {
            throw new InvestmentException(InvestmentErrorCode.INSUFFICIENT_HOLDING_QUANTITY);
        }

        long totalSellAmount = currentPrice * request.quantity();

        account.sell(normalizedStockCode, request.quantity(), currentPrice, LocalDateTime.now());
        investmentAccountRepository.save(account);

        holding.updateCurrentPrice(currentPrice);
        holding.sell(request.quantity());

        long remainingQuantity = holding.getQuantity();
        if (holding.isEmpty()) {
            holdingRepository.delete(holding);
        } else {
            holdingRepository.save(holding);
        }

        LocalDateTime executedAt = LocalDateTime.now();
        long cashBalanceAfterTrade = account.getCurrentCashBalance();
        TradeHistory tradeHistory = TradeHistory.sell(
                account.getAccountId(),
                MVP_DEFAULT_SEASON_ID,
                MVP_DEFAULT_SEASON_NUMBER,
                normalizedStockCode,
                request.quantity(),
                currentPrice,
                executedAt,
                cashBalanceAfterTrade
        );
        tradeHistoryRepository.save(tradeHistory);

        saveOutboxEvents(userId, account, stockItem, normalizedStockCode, "SELL", executedAt);

        return new SellStockResponse(
                normalizedStockCode,
                stockItem.getStockName(),
                request.quantity(),
                currentPrice,
                totalSellAmount,
                remainingQuantity,
                cashBalanceAfterTrade
        );
    }

    private void saveOutboxEvents(UUID userId, InvestmentAccount account, StockItem stockItem,
                                  String stockCode, String tradeType, LocalDateTime executedAt) {
        UUID accountId = account.getAccountId();
        UUID seasonId = account.getParticipant().getSeasonId();
        int seasonNumber = account.getParticipant().getSeasonNumber();
        String userNickname = account.getParticipant().getInvestorName();

        List<Holding> holdings = holdingRepository.findAllWithFilter(accountId, null);

        StockAssetType currentAssetType = stockItem.getAssetType();
        Map<String, StockAssetType> assetTypeMap = holdings.isEmpty() ? Map.of() :
                stockItemRepository.findAllByStockCodeIn(
                        holdings.stream().map(h -> h.getInstrumentCode().getValue()).toList()
                ).stream().collect(Collectors.toMap(StockItem::getStockCode, StockItem::getAssetType));

        int holdCount = (int) holdings.stream()
                .filter(h -> assetTypeMap.getOrDefault(
                        h.getInstrumentCode().getValue(), currentAssetType) == currentAssetType)
                .count();
        double returnRate = computeReturnRate(holdings, assetTypeMap, currentAssetType).doubleValue();

        TradeExecutedEvent tradeEvent = new TradeExecutedEvent(
                userId, accountId, seasonId, seasonNumber,
                tradeType, currentAssetType.name(), stockCode,
                holdCount, returnRate, userNickname, executedAt
        );
        outboxEventRepository.save(OutboxEvent.of(KafkaTopics.TRADE_EXECUTED, toJson(tradeEvent)));

        PortfolioSnapshotEvent snapshotEvent = buildSnapshotEvent(
                userId, accountId, seasonId, holdings, executedAt);
        outboxEventRepository.save(OutboxEvent.of(KafkaTopics.PORTFOLIO_SNAPSHOT, toJson(snapshotEvent)));
    }

    private PortfolioSnapshotEvent buildSnapshotEvent(UUID userId, UUID accountId, UUID seasonId,
                                                      List<Holding> holdings, LocalDateTime updatedAt) {
        Map<String, StockAssetType> assetTypeMap = holdings.isEmpty() ? Map.of() :
                stockItemRepository.findAllByStockCodeIn(
                        holdings.stream().map(h -> h.getInstrumentCode().getValue()).toList()
                ).stream().collect(Collectors.toMap(StockItem::getStockCode, StockItem::getAssetType));

        BigDecimal overallReturnRate = computeReturnRate(holdings, assetTypeMap, null);
        BigDecimal stockReturnRate = computeReturnRate(holdings, assetTypeMap, StockAssetType.STOCK);
        BigDecimal etfReturnRate = computeReturnRate(holdings, assetTypeMap, StockAssetType.ETF);

        int stockHoldingCount = (int) holdings.stream()
                .filter(h -> assetTypeMap.getOrDefault(
                        h.getInstrumentCode().getValue(), StockAssetType.STOCK) == StockAssetType.STOCK)
                .count();
        int etfHoldingCount = (int) holdings.stream()
                .filter(h -> assetTypeMap.getOrDefault(
                        h.getInstrumentCode().getValue(), StockAssetType.STOCK) == StockAssetType.ETF)
                .count();

        return new PortfolioSnapshotEvent(
                userId, accountId, seasonId,
                overallReturnRate, stockReturnRate, etfReturnRate,
                stockHoldingCount, etfHoldingCount, updatedAt
        );
    }

    private BigDecimal computeReturnRate(List<Holding> holdings, Map<String, StockAssetType> assetTypeMap,
                                         StockAssetType targetType) {
        List<Holding> filtered = targetType == null ? holdings : holdings.stream()
                .filter(h -> assetTypeMap.getOrDefault(
                        h.getInstrumentCode().getValue(), StockAssetType.STOCK) == targetType)
                .toList();

        long totalBuyAmount = filtered.stream().mapToLong(Holding::getTotalBuyAmount).sum();
        if (totalBuyAmount == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long totalUnrealizedProfitLoss = filtered.stream().mapToLong(Holding::getUnrealizedProfitLoss).sum();
        return BigDecimal.valueOf(totalUnrealizedProfitLoss)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBuyAmount), 2, RoundingMode.HALF_UP);
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox 이벤트 직렬화 실패: " + event.getClass().getSimpleName(), e);
        }
    }

    private String normalizeStockCode(String stockCode) {
        return stockCode == null ? null : stockCode.trim().toUpperCase();
    }

    private Optional<Long> resolveCurrentPriceAndCache(String stockCode, StockItem stockItem) {
        return stockPriceRepository.findCurrentPriceWithSource(stockCode)
                .map(resolvedPrice -> {
                    if (resolvedPrice.source() == StockPriceSource.KIS) {
                        stockItem.updateCurrentPrice(resolvedPrice.currentPrice(), LocalDateTime.now());
                    }
                    return resolvedPrice.currentPrice();
                });
    }
}