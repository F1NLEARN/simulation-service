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
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.trade.entity.TradeHistory;
import com.finlearn.simulationservice.domain.trade.repository.TradeHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
    private final HoldingRepository holdingRepository;
    private final TradeHistoryRepository tradeHistoryRepository;

    @Transactional
    public BuyStockResponse buy(UUID userId, BuyOrderRequest request) {
        if (request.quantity() <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ORDER_QUANTITY);
        }

        InvestmentAccount account = investmentAccountRepository.findByUserIdAndStatus(userId, InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        String normalizedStockCode = normalizeStockCode(request.stockCode());
        StockItem stockItem = stockItemRepository.findByStockCode(normalizedStockCode)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_ITEM_NOT_FOUND));

        BigDecimal currentPrice = stockItem.getCurrentPrice();
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvestmentException(InvestmentErrorCode.STOCK_NOT_TRADABLE);
        }

        long currentPriceAsLong = toLongAmount(currentPrice);
        long totalAmount = currentPriceAsLong * request.quantity();
        if (toLongAmount(account.getCashBalance()) < totalAmount) {
            throw new InvestmentException(InvestmentErrorCode.INSUFFICIENT_CASH);
        }

        account.buy(stockItem.getAssetType(), normalizedStockCode, request.quantity(), currentPrice, LocalDateTime.now());
        investmentAccountRepository.save(account);

        Holding holding = holdingRepository.findByAccountIdAndInstrumentCode(account.getInvestmentAccountId(), normalizedStockCode)
                .map(existing -> {
                    existing.updateCurrentPrice(currentPriceAsLong);
                    existing.addBuy(request.quantity(), currentPriceAsLong);
                    return existing;
                })
                .orElseGet(() -> Holding.create(new CreateHoldingCommand(
                        account.getInvestmentAccountId(),
                        stockItem.getStockName(),
                        MVP_DEFAULT_SEASON_ID,
                        MVP_DEFAULT_SEASON_NUMBER,
                        normalizedStockCode,
                        request.quantity(),
                        currentPriceAsLong,
                        currentPriceAsLong
                )));
        holdingRepository.save(holding);

        long cashBalanceAfterTrade = toLongAmount(account.getCashBalance());
        TradeHistory tradeHistory = TradeHistory.buy(
                account.getInvestmentAccountId(),
                MVP_DEFAULT_SEASON_ID,
                MVP_DEFAULT_SEASON_NUMBER,
                normalizedStockCode,
                request.quantity(),
                currentPriceAsLong,
                LocalDateTime.now(),
                cashBalanceAfterTrade
        );
        tradeHistoryRepository.save(tradeHistory);

        return new BuyStockResponse(
                account.getInvestmentAccountId(),
                normalizedStockCode,
                stockItem.getStockName(),
                "BUY",
                request.quantity(),
                currentPriceAsLong,
                totalAmount,
                cashBalanceAfterTrade
        );
    }

    @Transactional
    public SellStockResponse sell(UUID userId, SellOrderRequest request) {
        if (request.quantity() <= 0) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ORDER_QUANTITY);
        }

        InvestmentAccount account = investmentAccountRepository.findByUserIdAndStatus(userId, InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        String normalizedStockCode = normalizeStockCode(request.stockCode());
        StockItem stockItem = stockItemRepository.findByStockCode(normalizedStockCode)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_ITEM_NOT_FOUND));

        BigDecimal currentPrice = stockItem.getCurrentPrice();
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvestmentException(InvestmentErrorCode.STOCK_NOT_TRADABLE);
        }

        Holding holding = holdingRepository.findByAccountIdAndInstrumentCode(account.getInvestmentAccountId(), normalizedStockCode)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.HOLDING_STOCK_NOT_FOUND));

        if (holding.getQuantity() < request.quantity()) {
            throw new InvestmentException(InvestmentErrorCode.INSUFFICIENT_HOLDING_QUANTITY);
        }

        long currentPriceAsLong = toLongAmount(currentPrice);
        long totalSellAmount = currentPriceAsLong * request.quantity();
        account.addCashBalance(currentPrice.multiply(BigDecimal.valueOf(request.quantity())));
        investmentAccountRepository.save(account);

        holding.updateCurrentPrice(currentPriceAsLong);
        holding.sell(request.quantity());

        long remainingQuantity = holding.getQuantity();
        if (holding.isEmpty()) {
            holdingRepository.delete(holding);
        } else {
            holdingRepository.save(holding);
        }

        long cashBalanceAfterTrade = toLongAmount(account.getCashBalance());
        TradeHistory tradeHistory = TradeHistory.sell(
                account.getInvestmentAccountId(),
                MVP_DEFAULT_SEASON_ID,
                MVP_DEFAULT_SEASON_NUMBER,
                normalizedStockCode,
                request.quantity(),
                currentPriceAsLong,
                LocalDateTime.now(),
                cashBalanceAfterTrade
        );
        tradeHistoryRepository.save(tradeHistory);

        return new SellStockResponse(
                normalizedStockCode,
                stockItem.getStockName(),
                request.quantity(),
                currentPriceAsLong,
                totalSellAmount,
                remainingQuantity,
                cashBalanceAfterTrade
        );
    }

    private String normalizeStockCode(String stockCode) {
        return stockCode == null ? null : stockCode.trim().toUpperCase();
    }

    private long toLongAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
