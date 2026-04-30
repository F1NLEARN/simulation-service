package com.finlearn.simulationservice.application.investment.service;

import com.finlearn.simulationservice.application.investment.dto.request.BuyStockRequest;
import com.finlearn.simulationservice.application.investment.dto.request.RegisterFavoriteStockRequest;
import com.finlearn.simulationservice.application.investment.dto.request.SellStockRequest;
import com.finlearn.simulationservice.application.investment.dto.response.FavoriteStockResponse;
import com.finlearn.simulationservice.application.investment.dto.response.StockItemDetailResponse;
import com.finlearn.simulationservice.application.investment.dto.response.StockItemResponse;
import com.finlearn.simulationservice.application.investment.dto.response.StockPriceResponse;
import com.finlearn.simulationservice.domain.investment.entity.FavoriteStock;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.entity.SeedMoneyGrantHistory;
import com.finlearn.simulationservice.domain.investment.entity.SeasonParticipant;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.entity.StockTransaction;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.event.PointQuizPassedEvent;
import com.finlearn.simulationservice.domain.investment.event.SeasonInvestmentAccountOpenedEvent;
import com.finlearn.simulationservice.domain.investment.event.SeedMoneyGrantedEvent;
import com.finlearn.simulationservice.domain.investment.event.StockBoughtEvent;
import com.finlearn.simulationservice.domain.investment.event.StockSoldEvent;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.FavoriteStockRepository;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.SeasonParticipantRepository;
import com.finlearn.simulationservice.domain.investment.repository.SeedMoneyGrantHistoryRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestmentService {

    private final FavoriteStockRepository favoriteStockRepository;
    private final SeasonParticipantRepository seasonParticipantRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final SeedMoneyGrantHistoryRepository seedMoneyGrantHistoryRepository;
    private final StockItemRepository stockItemRepository;
    private final StockPriceRepository stockPriceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void handlePointQuizPassed(PointQuizPassedEvent event) {
        SeasonParticipant participant = seasonParticipantRepository.findBySeasonIdAndUserId(event.seasonId(), event.userId())
                .orElseGet(() -> seasonParticipantRepository.save(SeasonParticipant.create(event.seasonId(), event.userId())));

        boolean accountAlreadyExists = investmentAccountRepository.findBySeasonParticipantId(participant.getSeasonParticipantId())
                .isPresent();

        if (accountAlreadyExists) {
            return;
        }

        InvestmentAccount account = InvestmentAccount.open(participant.getSeasonParticipantId(), event.seedMoney());
        InvestmentAccount savedAccount = investmentAccountRepository.save(account);
        seedMoneyGrantHistoryRepository.save(
                SeedMoneyGrantHistory.grant(
                        event.userId(),
                        event.seasonId(),
                        savedAccount.getInvestmentAccountId(),
                        event.seedMoney(),
                        LocalDateTime.now()
                )
        );

        eventPublisher.publishEvent(new SeasonInvestmentAccountOpenedEvent(event.seasonId(), event.userId(), event.seedMoney()));
        eventPublisher.publishEvent(new SeedMoneyGrantedEvent(event.seasonId(), event.userId(), event.seedMoney()));
    }

    @Transactional
    public void buyStock(BuyStockRequest request) {
        InvestmentAccount account = investmentAccountRepository.findById(request.investmentAccountId())
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        if (account.getStatus() != InvestmentAccountStatus.ACTIVE) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ACCOUNT_STATUS);
        }

        BigDecimal currentPrice = stockPriceRepository.findCurrentPrice(request.assetType(), request.symbol())
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_PRICE_NOT_FOUND));

        StockTransaction transaction = account.buy(
                request.assetType(),
                request.symbol(),
                request.quantity(),
                currentPrice,
                LocalDateTime.now()
        );

        investmentAccountRepository.save(account);
        eventPublisher.publishEvent(new StockBoughtEvent(
                account.getInvestmentAccountId(),
                account.getSeasonParticipantId(),
                request.assetType(),
                request.symbol(),
                transaction.getQuantity(),
                transaction.getUnitPrice(),
                transaction.getTotalAmount(),
                transaction.getExecutedAt()
        ));
    }

    @Transactional
    public void sellStock(SellStockRequest request) {
        InvestmentAccount account = investmentAccountRepository.findById(request.investmentAccountId())
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        if (account.getStatus() != InvestmentAccountStatus.ACTIVE) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ACCOUNT_STATUS);
        }

        BigDecimal currentPrice = stockPriceRepository.findCurrentPrice(request.assetType(), request.symbol())
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_PRICE_NOT_FOUND));

        StockTransaction transaction = account.sell(
                request.assetType(),
                request.symbol(),
                request.quantity(),
                currentPrice,
                LocalDateTime.now()
        );

        investmentAccountRepository.save(account);
        eventPublisher.publishEvent(new StockSoldEvent(
                account.getInvestmentAccountId(),
                account.getSeasonParticipantId(),
                request.assetType(),
                request.symbol(),
                transaction.getQuantity(),
                transaction.getUnitPrice(),
                transaction.getTotalAmount(),
                transaction.getExecutedAt()
        ));
    }

    @Transactional
    public UUID registerFavoriteStock(UUID userId, RegisterFavoriteStockRequest request) {
        String normalizedSymbol = normalizeSymbol(request.symbol());

        if (favoriteStockRepository.existsByUserIdAndSymbol(userId, normalizedSymbol)) {
            throw new InvestmentException(InvestmentErrorCode.FAVORITE_STOCK_ALREADY_EXISTS);
        }

        FavoriteStock favoriteStock = FavoriteStock.register(userId, request.assetType(), normalizedSymbol);
        try {
            FavoriteStock saved = favoriteStockRepository.save(favoriteStock);
            return saved.getFavoriteStockId();
        } catch (DataIntegrityViolationException e) {
            throw new InvestmentException(InvestmentErrorCode.FAVORITE_STOCK_ALREADY_EXISTS);
        }
    }

    public List<FavoriteStockResponse> getFavoriteStocks(UUID userId) {
        return favoriteStockRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(FavoriteStockResponse::from)
                .toList();
    }

    @Transactional
    public void deleteFavoriteStock(UUID userId, String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        FavoriteStock favoriteStock = favoriteStockRepository.findByUserIdAndSymbol(userId, normalizedSymbol)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.FAVORITE_STOCK_NOT_FOUND));
        favoriteStockRepository.delete(favoriteStock);
    }

    public List<StockItemResponse> getStockItems(String assetType) {
        StockAssetType filter = parseAssetType(assetType);
        List<StockItem> stockItems = filter == null
                ? stockItemRepository.findAllByCurrentPriceIsNotNullOrderByStockCodeAsc()
                : stockItemRepository.findAllByAssetTypeAndCurrentPriceIsNotNullOrderByStockCodeAsc(filter);

        return stockItems.stream()
                .map(StockItemResponse::from)
                .toList();
    }

    public StockItemDetailResponse getStockItemDetail(String stockCode) {
        String normalizedStockCode = normalizeSymbol(stockCode);
        StockItem stockItem = stockItemRepository.findByStockCode(normalizedStockCode)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_ITEM_NOT_FOUND));
        return StockItemDetailResponse.from(stockItem);
    }

    public StockPriceResponse getCurrentStockPrice(String stockCode) {
        String normalizedStockCode = normalizeSymbol(stockCode);
        BigDecimal currentPrice = stockPriceRepository.findCurrentPrice(null, normalizedStockCode)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_PRICE_NOT_FOUND));
        return new StockPriceResponse(normalizedStockCode, currentPrice);
    }

    private String normalizeSymbol(String symbol) {
        return symbol == null ? null : symbol.trim().toUpperCase();
    }

    private StockAssetType parseAssetType(String assetType) {
        if (assetType == null || assetType.isBlank()) {
            return null;
        }

        try {
            return StockAssetType.valueOf(assetType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ASSET_TYPE);
        }
    }
}
