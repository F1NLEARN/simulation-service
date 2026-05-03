package com.finlearn.simulationservice.domain.analysis.exception;

import com.finlearn.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class AiAnalysisNotFoundException extends CustomException {

    private static final String CODE = "AI_ANALYSIS_NOT_FOUND";
    private static final String FIELD = "aiAnalysisId";
    private static final String MESSAGE = "포트폴리오 분석 결과를 찾을 수 없습니다.";

    public AiAnalysisNotFoundException() {
        super(CODE, FIELD, MESSAGE, HttpStatus.NOT_FOUND);
    }
}