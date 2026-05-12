package com.finlearn.simulationservice.application.analysis.dto.response;

import com.finlearn.simulationservice.domain.analysis.vo.ConcentrationLevel;
import com.finlearn.simulationservice.domain.analysis.vo.PortfolioDiagnosis;
import com.finlearn.simulationservice.domain.analysis.vo.RiskLevel;

import java.util.List;

public record PortfolioDiagnosisResponse(
        ConcentrationLevel concentrationLevel,
        RiskLevel riskLevel,
        String analysisSummary,
        List<String> warnings
) {
    public static PortfolioDiagnosisResponse from(PortfolioDiagnosis diagnosis) {
        return new PortfolioDiagnosisResponse(
                diagnosis.concentrationLevel(),
                diagnosis.riskLevel(),
                diagnosis.analysisSummary(),
                diagnosis.warnings()
        );
    }
}
