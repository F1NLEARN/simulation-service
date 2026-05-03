package com.finlearn.simulationservice.domain.analysis.exception;

import com.finlearn.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class AiAnalysisForbiddenException extends CustomException {

    private static final String CODE = "AI_ANALYSIS_FORBIDDEN_ACCESS";
    private static final String FIELD = "accountId";
    private static final String MESSAGE = "해당 포트폴리오 분석 결과에 접근할 권한이 없습니다.";

    public AiAnalysisForbiddenException() {
        super(CODE, FIELD, MESSAGE, HttpStatus.FORBIDDEN);
    }
}