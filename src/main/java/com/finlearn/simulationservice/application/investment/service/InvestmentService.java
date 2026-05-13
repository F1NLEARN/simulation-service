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
import com.finlearn.simulationservice.domain.investment.vo.SeasonParticipant;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.entity.StockTransaction;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.enums.SeedMoneyGrantType;
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
import com.finlearn.simulationservice.domain.investment.repository.SeedMoneyGrantHistoryRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockPriceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestmentService {

    private final FavoriteStockRepository favoriteStockRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final SeedMoneyGrantHistoryRepository seedMoneyGrantHistoryRepository;
    private final StockItemRepository stockItemRepository;
    private final StockPriceRepository stockPriceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void handlePointQuizPassed(PointQuizPassedEvent event) {
        boolean accountAlreadyExists = investmentAccountRepository
                .findByParticipant_InvestorIdAndParticipant_SeasonId(event.investorId(), event.seasonId())
                .isPresent();

        if (accountAlreadyExists) {
            return;
        }

        SeasonParticipant participant = new SeasonParticipant(
                event.investorId(), event.investorName(), event.seasonId(), event.seasonNumber());
        InvestmentAccount account = InvestmentAccount.open(participant, event.seedMoney());

        InvestmentAccount savedAccount;
        try {
            savedAccount = investmentAccountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            boolean createdByConcurrentRequest = investmentAccountRepository
                    .findByParticipant_InvestorIdAndParticipant_SeasonId(event.investorId(), event.seasonId())
                    .isPresent();
            if (createdByConcurrentRequest) {
                return;
            }
            throw e;
        }

        seedMoneyGrantHistoryRepository.save(
                SeedMoneyGrantHistory.grant(
                        savedAccount.getAccountId(),
                        event.seasonId(),
                        event.seasonNumber(),
                        SeedMoneyGrantType.INITIAL,
                        event.seedMoney(),
                        "포인트 퀴즈 통과 시드머니 지급",
                        LocalDateTime.now()
                )
        );

        eventPublisher.publishEvent(new SeasonInvestmentAccountOpenedEvent(event.seasonId(), event.investorId(), event.seedMoney()));
        eventPublisher.publishEvent(new SeedMoneyGrantedEvent(event.seasonId(), event.investorId(), event.seedMoney()));
    }

    @Transactional
    public void buyStock(BuyStockRequest request) {
        InvestmentAccount account = investmentAccountRepository.findById(request.accountId())
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        if (account.getStatus() != InvestmentAccountStatus.ACTIVE) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ACCOUNT_STATUS);
        }

        StockItem stockItem = stockItemRepository.findByStockCode(normalizeCode(request.instrumentCode()))
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_PRICE_NOT_FOUND));

        Long currentPrice = stockItem.getCurrentPrice();
        if (currentPrice == null || currentPrice <= 0) {
            throw new InvestmentException(InvestmentErrorCode.STOCK_PRICE_NOT_FOUND);
        }

        StockTransaction transaction = account.buy(
                stockItem.getStockCode(),
                stockItem.getStockName(),
                request.quantity(),
                currentPrice,
                LocalDateTime.now()
        );

        investmentAccountRepository.save(account);
        eventPublisher.publishEvent(new StockBoughtEvent(
                account.getAccountId(),
                account.getParticipant().getInvestorId(),
                account.getParticipant().getSeasonId(),
                account.getParticipant().getSeasonNumber(),
                transaction.getInstrumentCode(),
                transaction.getQuantity(),
                transaction.getTradePrice(),
                transaction.getTotalTradeAmount(),
                transaction.getTradeAt()
        ));
    }

    @Transactional
    public void sellStock(SellStockRequest request) {
        InvestmentAccount account = investmentAccountRepository.findById(request.accountId())
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        if (account.getStatus() != InvestmentAccountStatus.ACTIVE) {
            throw new InvestmentException(InvestmentErrorCode.INVALID_ACCOUNT_STATUS);
        }

        Long currentPrice = stockPriceRepository.findCurrentPrice(normalizeCode(request.instrumentCode()))
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_PRICE_NOT_FOUND));

        StockTransaction transaction = account.sell(
                normalizeCode(request.instrumentCode()),
                request.quantity(),
                currentPrice,
                LocalDateTime.now()
        );

        investmentAccountRepository.save(account);
        eventPublisher.publishEvent(new StockSoldEvent(
                account.getAccountId(),
                account.getParticipant().getInvestorId(),
                account.getParticipant().getSeasonId(),
                transaction.getInstrumentCode(),
                transaction.getQuantity(),
                transaction.getTradePrice(),
                transaction.getTotalTradeAmount(),
                transaction.getTradeAt()
        ));
    }

    @Transactional
    public UUID registerFavoriteStock(UUID userId, RegisterFavoriteStockRequest request) {
        String normalizedSymbol = normalizeCode(request.symbol());

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
        List<FavoriteStock> favoriteStocks = favoriteStockRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        if (favoriteStocks.isEmpty()) {
            return List.of();
        }

        List<String> stockCodes = favoriteStocks.stream()
                .map(FavoriteStock::getSymbol)
                .distinct()
                .toList();

        Map<String, String> stockNameByCode = stockItemRepository.findAllByStockCodeIn(stockCodes).stream()
                .collect(Collectors.toMap(StockItem::getStockCode, StockItem::getStockName, (first, ignored) -> first));

        return favoriteStocks.stream()
                .map(favoriteStock -> FavoriteStockResponse.from(
                        favoriteStock,
                        stockNameByCode.getOrDefault(favoriteStock.getSymbol(), favoriteStock.getSymbol())
                ))
                .toList();
    }

    @Transactional
    public void deleteFavoriteStock(UUID userId, String symbol) {
        String normalizedSymbol = normalizeCode(symbol);
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
        String normalizedStockCode = normalizeCode(stockCode);
        StockItem stockItem = stockItemRepository.findByStockCode(normalizedStockCode)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_ITEM_NOT_FOUND));
        return StockItemDetailResponse.from(stockItem);
    }

    public StockPriceResponse getCurrentStockPrice(String stockCode) {
        String normalized = normalizeCode(stockCode);
        long currentPrice = stockPriceRepository.findCurrentPrice(normalized)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.STOCK_PRICE_NOT_FOUND));
        return new StockPriceResponse(normalized, currentPrice);
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
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
