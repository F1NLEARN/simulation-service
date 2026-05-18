package com.finlearn.simulationservice.application.investment.service;

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
import com.finlearn.simulationservice.domain.investment.enums.StockPriceSource;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockPriceRepository;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeHistory;
import com.finlearn.simulationservice.domain.tradehistory.event.TradeCompletedEvent;
import com.finlearn.simulationservice.domain.tradehistory.repository.TradeHistoryRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ApplicationEventPublisher eventPublisher;

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

        eventPublisher.publishEvent(new TradeCompletedEvent(
                userId, account.getAccountId(), account.getParticipant().getSeasonId(),
                "BUY", stockItem.getAssetType().name(), normalizedStockCode, executedAt
        ));

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

        eventPublisher.publishEvent(new TradeCompletedEvent(
                userId, account.getAccountId(), account.getParticipant().getSeasonId(),
                "SELL", stockItem.getAssetType().name(), normalizedStockCode, executedAt
        ));

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