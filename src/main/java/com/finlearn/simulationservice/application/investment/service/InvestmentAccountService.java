package com.finlearn.simulationservice.application.investment.service;

import com.finlearn.simulationservice.application.investment.dto.response.InvestmentAccountResponse;
import com.finlearn.simulationservice.domain.investment.entity.HoldingStock;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentErrorCode;
import com.finlearn.simulationservice.domain.investment.exception.InvestmentException;
import com.finlearn.simulationservice.domain.investment.repository.InvestmentAccountRepository;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.investment.vo.SeasonParticipant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestmentAccountService {

    // TODO: Season 도메인 연동 후 실제 seasonId/seasonNumber/investorName를 조회하도록 변경
    private static final UUID MVP_DEFAULT_SEASON_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int MVP_DEFAULT_SEASON_NUMBER = 1;
    private static final long DEFAULT_INITIAL_SEED_MONEY = 10_000_000L;

    private final InvestmentAccountRepository investmentAccountRepository;
    private final StockItemRepository stockItemRepository;

    @Transactional
    public InvestmentAccountResponse createAccount(UUID userId) {
        investmentAccountRepository.findByParticipant_InvestorIdAndStatus(userId, InvestmentAccountStatus.ACTIVE)
                .ifPresent(account -> {
                    throw new InvestmentException(InvestmentErrorCode.ACTIVE_INVESTMENT_ACCOUNT_ALREADY_EXISTS);
                });

        SeasonParticipant participant = new SeasonParticipant(
                userId, "투자자", MVP_DEFAULT_SEASON_ID, MVP_DEFAULT_SEASON_NUMBER);
        InvestmentAccount account = InvestmentAccount.open(participant, DEFAULT_INITIAL_SEED_MONEY);
        InvestmentAccount saved = investmentAccountRepository.save(account);
        return InvestmentAccountResponse.from(saved);
    }

    public InvestmentAccountResponse getMyAccount(UUID userId) {
        InvestmentAccount account = investmentAccountRepository.findByParticipant_InvestorIdAndStatus(userId, InvestmentAccountStatus.ACTIVE)
                .orElseThrow(() -> new InvestmentException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));
        return buildLiveAccountResponse(account);
    }

    private InvestmentAccountResponse buildLiveAccountResponse(InvestmentAccount account) {
        List<HoldingStock> holdingStocks = account.getHoldingStocks();
        if (holdingStocks.isEmpty()) {
            return InvestmentAccountResponse.from(account);
        }

        List<String> codes = holdingStocks.stream()
                .map(HoldingStock::getInstrumentCode)
                .toList();
        Map<String, Long> priceMap = stockItemRepository.findAllByStockCodeIn(codes).stream()
                .filter(s -> s.getCurrentPrice() != null && s.getCurrentPrice() > 0)
                .collect(Collectors.toMap(
                        s -> s.getStockCode(),
                        s -> s.getCurrentPrice()));

        long liveValuationAmount = holdingStocks.stream()
                .mapToLong(h -> priceMap.getOrDefault(h.getInstrumentCode(), h.getAverageBuyPrice()) * h.getQuantity())
                .sum();
        long liveAssetAmount = account.getCurrentCashBalance() + liveValuationAmount;
        long liveProfitLoss = liveAssetAmount - account.getInitialSeedMoney();
        BigDecimal liveProfitRate = account.getInitialSeedMoney() == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(liveProfitLoss)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(account.getInitialSeedMoney()), 2, RoundingMode.HALF_UP);

        return InvestmentAccountResponse.withLiveValues(
                account, liveValuationAmount, liveProfitLoss, liveProfitRate);
    }
}
