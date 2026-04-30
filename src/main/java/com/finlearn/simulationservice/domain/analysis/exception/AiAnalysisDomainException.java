package com.finlearn.simulationservice.domain.analysis.exception;

import com.finlearn.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class AiAnalysisDomainException extends CustomException {

    public AiAnalysisDomainException(AiAnalysisErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getField(), errorCode.getMessage(), HttpStatus.BAD_REQUEST);
    }
}