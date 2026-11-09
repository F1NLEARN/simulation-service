package com.finlearn.simulationservice.application.analysis.service;

import com.finlearn.simulationservice.domain.analysis.command.CreateAiAnalysisCommand;
import com.finlearn.simulationservice.domain.analysis.entity.AiAnalysis;
import com.finlearn.simulationservice.domain.analysis.repository.AiAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiAnalysisPersistenceService {

    private final AiAnalysisRepository aiAnalysisRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCompleted(CreateAiAnalysisCommand command) {
        AiAnalysis analysis = AiAnalysis.create(command);
        analysis.complete();
        aiAnalysisRepository.save(analysis);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailed(CreateAiAnalysisCommand command, String errorMessage) {
        AiAnalysis analysis = AiAnalysis.create(command);
        analysis.fail(errorMessage);
        aiAnalysisRepository.save(analysis);
    }
}
