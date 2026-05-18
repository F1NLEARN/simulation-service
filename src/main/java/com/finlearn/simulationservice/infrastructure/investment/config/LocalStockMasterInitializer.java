package com.finlearn.simulationservice.infrastructure.investment.config;

import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "docker"})
@RequiredArgsConstructor
public class LocalStockMasterInitializer implements ApplicationRunner {

    private final StockItemRepository stockItemRepository;

    @Value("${stock-master.seed.enabled:true}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        // ── KOSPI 대형주 ────────────────────────────────────────────────
        seedIfMissing("삼성전자",       "005930", StockAssetType.STOCK);
        seedIfMissing("SK하이닉스",     "000660", StockAssetType.STOCK);
        seedIfMissing("LG에너지솔루션", "373220", StockAssetType.STOCK);
        seedIfMissing("삼성바이오로직스","207940", StockAssetType.STOCK);
        seedIfMissing("현대차",         "005380", StockAssetType.STOCK);
        seedIfMissing("기아",           "000270", StockAssetType.STOCK);
        seedIfMissing("POSCO홀딩스",   "005490", StockAssetType.STOCK);
        seedIfMissing("삼성SDI",        "006400", StockAssetType.STOCK);
        seedIfMissing("LG화학",         "051910", StockAssetType.STOCK);
        seedIfMissing("NAVER",          "035420", StockAssetType.STOCK);
        seedIfMissing("카카오",         "035720", StockAssetType.STOCK);
        seedIfMissing("셀트리온",       "068270", StockAssetType.STOCK);
        seedIfMissing("현대모비스",     "012330", StockAssetType.STOCK);
        seedIfMissing("KB금융",         "105560", StockAssetType.STOCK);
        seedIfMissing("신한지주",       "055550", StockAssetType.STOCK);
        seedIfMissing("하나금융지주",   "086790", StockAssetType.STOCK);
        seedIfMissing("우리금융지주",   "316140", StockAssetType.STOCK);
        seedIfMissing("LG전자",         "066570", StockAssetType.STOCK);
        seedIfMissing("SK텔레콤",       "017670", StockAssetType.STOCK);
        seedIfMissing("KT",             "030200", StockAssetType.STOCK);
        seedIfMissing("삼성물산",       "028260", StockAssetType.STOCK);
        seedIfMissing("두산에너빌리티", "034020", StockAssetType.STOCK);
        seedIfMissing("고려아연",       "010130", StockAssetType.STOCK);
        seedIfMissing("한국전력",       "015760", StockAssetType.STOCK);
        seedIfMissing("크래프톤",       "259960", StockAssetType.STOCK);

        // ── KOSDAQ 대형주 ───────────────────────────────────────────────
        seedIfMissing("에코프로비엠",   "247540", StockAssetType.STOCK);
        seedIfMissing("에코프로",       "086520", StockAssetType.STOCK);
        seedIfMissing("HLB",            "028300", StockAssetType.STOCK);
        seedIfMissing("알테오젠",       "196170", StockAssetType.STOCK);
        seedIfMissing("리가켐바이오",   "141080", StockAssetType.STOCK);

        // ── ETF ─────────────────────────────────────────────────────────
        seedIfMissing("KODEX 200",          "069500", StockAssetType.ETF);
        seedIfMissing("TIGER 200",          "102110", StockAssetType.ETF);
        seedIfMissing("KODEX 레버리지",     "122630", StockAssetType.ETF);
        seedIfMissing("KODEX 인버스",       "114800", StockAssetType.ETF);
        seedIfMissing("TIGER 미국S&P500",   "360750", StockAssetType.ETF);
        seedIfMissing("KODEX 미국S&P500TR", "379800", StockAssetType.ETF);
        seedIfMissing("TIGER 나스닥100",    "133690", StockAssetType.ETF);
        seedIfMissing("KODEX 코스닥150",    "229200", StockAssetType.ETF);
    }

    private void seedIfMissing(String stockName, String stockCode, StockAssetType assetType) {
        if (stockItemRepository.findByStockCode(stockCode).isPresent()) {
            return;
        }
        stockItemRepository.save(StockItem.create(stockName, stockCode, assetType));
    }
}
