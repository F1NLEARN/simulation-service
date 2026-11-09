package com.finlearn.simulationservice.application.analysis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAnalysisResponse;
import com.finlearn.simulationservice.application.analysis.query.GetPortfolioAnalysisQuery;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisStatus;
import com.finlearn.simulationservice.domain.analysis.repository.AiAnalysisRepository;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAnalysisQueryService {

    private final AiAnalysisRepository aiAnalysisRepository;
    private final AiAnalysisService aiAnalysisService;
    private final ObjectMapper objectMapper;
    private final PortfolioAnalysisDataService portfolioAnalysisDataService;

    @Cacheable(value = "portfolioAnalysis", key = "#query.investorId")
    public PortfolioAnalysisResponse getPortfolioAnalysis(GetPortfolioAnalysisQuery query) {

        PortfolioAnalysisData data =
                portfolioAnalysisDataService.load(query.investorId());

        List<PortfolioRecommendation> resolvedRecommendations =
                resolveRecommendations(
                        data.account().getAccountId(),
                        data.diagnosis().recommendations()
                );

        return PortfolioAnalysisResponse.of(
                data.account(),
                data.holdings(),
                data.allocation(),
                data.diagnosis(),
                resolvedRecommendations,
                data.stockReturnRate(),
                data.etfReturnRate()
        );
    }

    /**
     * AI 분석을 우선 시도하고,
     * 실패하면 규칙 기반 추천으로 Fallback한다.
     *
     * AI 호출 중에는 활성 트랜잭션을 유지하지 않는다.
     */
    @CacheEvict(value = "portfolioAnalysis", key = "#investorId")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PortfolioAnalysisResponse refresh(UUID investorId) {

        // 읽기 전용 트랜잭션에서 조회 및 계산
        PortfolioAnalysisData data =
                portfolioAnalysisDataService.load(investorId);

        List<PortfolioRecommendation> recommendations;

        try {
            // 트랜잭션 없이 AI 호출
            recommendations = aiAnalysisService.callAndSave(
                    data.account(),
                    data.diagnosis(),
                    data.allocation(),
                    data.diagnosis().recommendations()
            );

        } catch (Exception e) {
            log.warn(
                    "[PortfolioAnalysis] AI 분석 실패 - 규칙 기반 결과로 대체",
                    e
            );

            recommendations = data.diagnosis().recommendations();

            try {
                // 별도의 쓰기 트랜잭션에서 실패 이력 저장
                aiAnalysisService.saveFailed(
                        data.account(),
                        data.diagnosis(),
                        data.allocation(),
                        data.diagnosis().recommendations(),
                        e.getMessage()
                );
            } catch (Exception saveException) {
                log.error(
                        "[PortfolioAnalysis] AI 분석 실패 이력 저장 실패: investorId={}",
                        investorId,
                        saveException
                );
            }
        }

        return PortfolioAnalysisResponse.of(
                data.account(),
                data.holdings(),
                data.allocation(),
                data.diagnosis(),
                recommendations,
                data.stockReturnRate(),
                data.etfReturnRate()
        );
    }

    @CacheEvict(value = "portfolioAnalysis", key = "#investorId")
    public void evictCache(UUID investorId) {
        // 캐시 무효화는 @CacheEvict에서 처리
    }

    /**
     * 가장 최근 COMPLETED AI 분석 결과를 조회한다.
     * 결과가 없거나 JSON 파싱에 실패하면 규칙 기반 추천을 반환한다.
     */
    private List<PortfolioRecommendation> resolveRecommendations(
            UUID accountId,
            List<PortfolioRecommendation> ruleBasedRecommendations
    ) {
        return aiAnalysisRepository
                .findTopByAccountIdAndAnalysisStatusOrderByAnalyzedAtDesc(
                        accountId,
                        AnalysisStatus.COMPLETED
                )
                .map(analysis -> {
                    try {
                        JsonNode node = objectMapper.readTree(
                                analysis.getAiFeedbackMessage()
                        );

                        return objectMapper.<List<PortfolioRecommendation>>convertValue(
                                node,
                                new TypeReference<List<PortfolioRecommendation>>() {}
                        );
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .orElse(ruleBasedRecommendations);
    }
}
