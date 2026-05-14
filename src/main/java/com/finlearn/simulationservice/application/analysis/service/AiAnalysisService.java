package com.finlearn.simulationservice.application.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.simulationservice.application.analysis.dto.response.PortfolioAllocationResponse;
import com.finlearn.simulationservice.domain.analysis.command.CreateAiAnalysisCommand;
import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.entity.AnalysisType;
import com.finlearn.simulationservice.domain.analysis.repository.AiAnalysisRepository;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioDiagnosis;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioRecommendation;
import com.finlearn.simulationservice.domain.analysis.vo.RecommendationType;
import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.infrastructure.openai.OpenAiClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final AiAnalysisRepository aiAnalysisRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    @Value("classpath:/prompts/ai-analysis-system.txt")
    private Resource systemPromptResource;

    @Value("classpath:/prompts/ai-analysis-user.txt")
    private Resource userPromptTemplateResource;

    private String systemPrompt;
    private String userPromptTemplate;

    @PostConstruct
    void loadPrompts() throws IOException {
        systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        userPromptTemplate = userPromptTemplateResource.getContentAsString(StandardCharsets.UTF_8);
    }

    @Async("aiAnalysisExecutor")
    @Transactional
    public void createAsync(InvestmentAccount account,
                            PortfolioDiagnosis diagnosis,
                            PortfolioAllocationResponse allocation,
                            List<PortfolioRecommendation> ruleBasedRecommendations) {
        String prompt = buildUserPrompt(diagnosis, ruleBasedRecommendations);
        UUID investorId = account.getParticipant().getInvestorId();

        try {
            String rawResponse = openAiClient.call(systemPrompt, prompt);

            List<PortfolioRecommendation> aiParsed = parseRecommendations(rawResponse);

            if (aiParsed.size() != ruleBasedRecommendations.size()) {
                throw new IllegalStateException("AI 응답 항목 수 불일치");
            }

            List<PortfolioRecommendation> merged = mergeRecommendations(ruleBasedRecommendations, aiParsed);
            String aiFeedbackJson = objectMapper.writeValueAsString(merged);

            AiAnalysis analysis = AiAnalysis.create(
                    buildCommand(account, diagnosis, allocation, aiFeedbackJson, rawResponse, prompt));
            analysis.complete();
            aiAnalysisRepository.save(analysis);

            cacheManager.getCache("portfolioAnalysis").evict(investorId);

        } catch (Exception e) {
            AiAnalysis analysis = AiAnalysis.create(
                    buildCommand(account, diagnosis, allocation, "N/A", null, prompt));
            analysis.fail(e.getMessage());
            aiAnalysisRepository.save(analysis);
        }
    }

    private List<PortfolioRecommendation> mergeRecommendations(
            List<PortfolioRecommendation> ruleBased,
            List<PortfolioRecommendation> aiParsed) {
        List<PortfolioRecommendation> merged = new ArrayList<>();
        for (int i = 0; i < ruleBased.size(); i++) {
            PortfolioRecommendation original = ruleBased.get(i);
            PortfolioRecommendation ai = aiParsed.get(i);
            merged.add(new PortfolioRecommendation(
                    original.recommendationType(),
                    original.targetCategory(),
                    ai.reason(),
                    ai.message()
            ));
        }
        return merged;
    }

    private List<PortfolioRecommendation> parseRecommendations(String rawResponse) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode recommendationsNode = root.path("recommendations");
        return objectMapper.convertValue(
                recommendationsNode,
                new TypeReference<List<PortfolioRecommendation>>() {}
        );
    }

    private CreateAiAnalysisCommand buildCommand(InvestmentAccount account,
                                                  PortfolioDiagnosis diagnosis,
                                                  PortfolioAllocationResponse allocation,
                                                  String aiFeedbackMessage,
                                                  String modelResponse,
                                                  String prompt) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();

        BigDecimal riskScore = switch (diagnosis.riskLevel()) {
            case STABLE -> BigDecimal.valueOf(30);
            case NORMAL -> BigDecimal.valueOf(60);
            case AGGRESSIVE -> BigDecimal.valueOf(90);
        };

        String recommendedLearningTopic = diagnosis.recommendations().stream()
                .filter(r -> r.recommendationType() == RecommendationType.QUIZ)
                .findFirst()
                .map(r -> r.targetCategory() != null ? r.targetCategory() : "없음")
                .orElse("없음");

        return new CreateAiAnalysisCommand(
                account.getAccountId(),
                account.getParticipant().getInvestorId(),
                account.getParticipant().getInvestorName(),
                account.getParticipant().getSeasonId(),
                account.getParticipant().getSeasonNumber(),
                AnalysisType.PORTFOLIO,
                riskScore,
                allocation.topHoldingWeight(),
                recommendedLearningTopic,
                diagnosis.analysisSummary(),
                aiFeedbackMessage,
                startOfDay,
                now,
                now,
                prompt,
                modelResponse
        );
    }

    private String buildUserPrompt(PortfolioDiagnosis diagnosis, List<PortfolioRecommendation> ruleBasedRecommendations) {
        try {
            String recommendationsJson = objectMapper.writeValueAsString(ruleBasedRecommendations);
            return userPromptTemplate
                    .replace("{concentrationLevel}", diagnosis.concentrationLevel().name())
                    .replace("{riskLevel}", diagnosis.riskLevel().name())
                    .replace("{analysisSummary}", diagnosis.analysisSummary())
                    .replace("{warnings}", diagnosis.warnings().toString())
                    .replace("{recommendations}", recommendationsJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build user prompt", e);
        }
    }
}