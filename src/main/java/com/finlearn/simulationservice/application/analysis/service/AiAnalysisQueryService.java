package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.application.analysis.dto.response.AiAnalysisDetailResponse;
import com.finlearn.simulationservice.application.analysis.dto.response.AiAnalysisListResponse;
import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.exception.AiAnalysisForbiddenException;
import com.finlearn.simulationservice.domain.analysis.exception.AiAnalysisNotFoundException;
import com.finlearn.simulationservice.domain.analysis.repository.AiAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiAnalysisQueryService {

    private final AiAnalysisRepository aiAnalysisRepository;

    public List<AiAnalysisListResponse> getAnalysisList(UUID accountId) {
        return aiAnalysisRepository.findAllByAccountIdOrderByAnalyzedAtDesc(accountId)
                .stream()
                .map(AiAnalysisListResponse::from)
                .toList();
    }

    public AiAnalysisDetailResponse getLatestAnalysis(UUID accountId) {
        AiAnalysis aiAnalysis = aiAnalysisRepository.findTopByAccountIdOrderByAnalyzedAtDesc(accountId)
                .orElseThrow(AiAnalysisNotFoundException::new);
        return AiAnalysisDetailResponse.from(aiAnalysis);
    }

    public AiAnalysisDetailResponse getAnalysisDetail(UUID accountId, UUID aiAnalysisId) {
        AiAnalysis aiAnalysis = aiAnalysisRepository.findById(aiAnalysisId)
                .orElseThrow(AiAnalysisNotFoundException::new);

        if (!aiAnalysis.getAccountId().equals(accountId)) {
            throw new AiAnalysisForbiddenException();
        }

        return AiAnalysisDetailResponse.from(aiAnalysis);
    }
}