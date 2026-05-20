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

        // ── KOSPI 추가 ──────────────────────────────────────────────────
        seedIfMissing("삼성생명",           "032830", StockAssetType.STOCK);
        seedIfMissing("삼성화재",           "000810", StockAssetType.STOCK);
        seedIfMissing("한화에어로스페이스", "012450", StockAssetType.STOCK);
        seedIfMissing("HD현대중공업",       "329180", StockAssetType.STOCK);
        seedIfMissing("한국조선해양",       "009540", StockAssetType.STOCK);
        seedIfMissing("현대글로비스",       "086280", StockAssetType.STOCK);
        seedIfMissing("이마트",             "139480", StockAssetType.STOCK);
        seedIfMissing("CJ제일제당",         "097950", StockAssetType.STOCK);
        seedIfMissing("아모레퍼시픽",       "090430", StockAssetType.STOCK);
        seedIfMissing("LG생활건강",         "051900", StockAssetType.STOCK);
        seedIfMissing("KT&G",               "033780", StockAssetType.STOCK);
        seedIfMissing("유한양행",           "000100", StockAssetType.STOCK);
        seedIfMissing("한미약품",           "128940", StockAssetType.STOCK);
        seedIfMissing("한국항공우주",       "047810", StockAssetType.STOCK);
        seedIfMissing("현대건설",           "000720", StockAssetType.STOCK);
        seedIfMissing("SK이노베이션",       "096770", StockAssetType.STOCK);
        seedIfMissing("HD현대",             "267250", StockAssetType.STOCK);
        seedIfMissing("롯데케미칼",         "011170", StockAssetType.STOCK);
        seedIfMissing("카카오뱅크",         "323410", StockAssetType.STOCK);
        seedIfMissing("GS건설",             "006360", StockAssetType.STOCK);

        // ── KOSDAQ 대형주 ───────────────────────────────────────────────
        seedIfMissing("에코프로비엠",       "247540", StockAssetType.STOCK);
        seedIfMissing("에코프로",           "086520", StockAssetType.STOCK);
        seedIfMissing("HLB",                "028300", StockAssetType.STOCK);
        seedIfMissing("알테오젠",           "196170", StockAssetType.STOCK);
        seedIfMissing("리가켐바이오",       "141080", StockAssetType.STOCK);

        // ── KOSDAQ 추가 ──────────────────────────────────────────────────
        seedIfMissing("카카오게임즈",       "293490", StockAssetType.STOCK);
        seedIfMissing("펄어비스",           "263750", StockAssetType.STOCK);
        seedIfMissing("SM엔터테인먼트",     "041510", StockAssetType.STOCK);
        seedIfMissing("JYP엔터테인먼트",    "035900", StockAssetType.STOCK);
        seedIfMissing("셀트리온제약",       "068760", StockAssetType.STOCK);
        seedIfMissing("씨젠",               "096530", StockAssetType.STOCK);
        seedIfMissing("포스코DX",           "022100", StockAssetType.STOCK);
        seedIfMissing("레인보우로보틱스",   "277810", StockAssetType.STOCK);
        seedIfMissing("원익IPS",            "240810", StockAssetType.STOCK);
        seedIfMissing("에코프로머티리얼즈", "450080", StockAssetType.STOCK);
        seedIfMissing("솔브레인",           "357780", StockAssetType.STOCK);
        seedIfMissing("HPSP",               "403870", StockAssetType.STOCK);

        // ── ETF ─────────────────────────────────────────────────────────
        seedIfMissing("KODEX 200",                     "069500", StockAssetType.ETF);
        seedIfMissing("TIGER 200",                     "102110", StockAssetType.ETF);
        seedIfMissing("KODEX 레버리지",                "122630", StockAssetType.ETF);
        seedIfMissing("KODEX 인버스",                  "114800", StockAssetType.ETF);
        seedIfMissing("TIGER 미국S&P500",              "360750", StockAssetType.ETF);
        seedIfMissing("KODEX 미국S&P500TR",            "379800", StockAssetType.ETF);
        seedIfMissing("TIGER 나스닥100",               "133690", StockAssetType.ETF);
        seedIfMissing("KODEX 코스닥150",               "229200", StockAssetType.ETF);
        seedIfMissing("TIGER 반도체",                  "091230", StockAssetType.ETF);
        seedIfMissing("KODEX 반도체",                  "091160", StockAssetType.ETF);
        seedIfMissing("TIGER 2차전지테마",             "305540", StockAssetType.ETF);
        seedIfMissing("KODEX 2차전지산업",             "305720", StockAssetType.ETF);
        seedIfMissing("KODEX 건강관리",                "266410", StockAssetType.ETF);
        seedIfMissing("TIGER 헬스케어",                "143460", StockAssetType.ETF);
        seedIfMissing("KODEX 에너지화학",              "117460", StockAssetType.ETF);
        seedIfMissing("TIGER 금융",                    "139270", StockAssetType.ETF);
        seedIfMissing("KODEX 자동차",                  "091180", StockAssetType.ETF);
        seedIfMissing("KODEX 삼성그룹",                "156080", StockAssetType.ETF);
        seedIfMissing("KODEX 배당성장",                "211560", StockAssetType.ETF);
        seedIfMissing("TIGER 단기통안채",              "157490", StockAssetType.ETF);
        seedIfMissing("KODEX MSCI Korea TR",           "278540", StockAssetType.ETF);
        seedIfMissing("KODEX 200선물인버스2X",         "252670", StockAssetType.ETF);
        seedIfMissing("TIGER 코스닥150레버리지",       "233740", StockAssetType.ETF);
        seedIfMissing("KODEX KOSPI100",                "270800", StockAssetType.ETF);
        seedIfMissing("KODEX 단기채권PLUS",            "214980", StockAssetType.ETF);
        seedIfMissing("TIGER 미국S&P500선물(H)",       "143850", StockAssetType.ETF);
        seedIfMissing("KODEX 미국S&P500(H)",           "219480", StockAssetType.ETF);
        seedIfMissing("TIGER 미국테크TOP10INDXX",      "381170", StockAssetType.ETF);
        seedIfMissing("KODEX 미국빅테크10",            "438330", StockAssetType.ETF);
        seedIfMissing("TIGER 일본니케이225",           "241180", StockAssetType.ETF);
        seedIfMissing("KODEX 일본TOPIX100",            "243880", StockAssetType.ETF);
        seedIfMissing("TIGER 차이나CSI300",            "192090", StockAssetType.ETF);
        seedIfMissing("KODEX 차이나H",                 "099140", StockAssetType.ETF);
        seedIfMissing("TIGER 부동산인프라고배당",      "182480", StockAssetType.ETF);
        seedIfMissing("TIGER 미국채10년선물",          "305080", StockAssetType.ETF);
        seedIfMissing("KODEX 미국채울트라30년선물(H)", "304660", StockAssetType.ETF);
        seedIfMissing("KODEX K-방산",                  "449170", StockAssetType.ETF);
        seedIfMissing("KODEX IT",                      "091220", StockAssetType.ETF);
    }

    private void seedIfMissing(String stockName, String stockCode, StockAssetType assetType) {
        if (stockItemRepository.findByStockCode(stockCode).isPresent()) {
            return;
        }
        stockItemRepository.save(StockItem.create(stockName, stockCode, assetType));
    }
}
