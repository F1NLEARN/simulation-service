package com.finlearn.simulationservice.domain.analysis.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisScore {

    private BigDecimal value;

    private AnalysisScore(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("분석 점수는 0 이상이어야 합니다.");
        }
        this.value = value;
    }

    public static AnalysisScore of(BigDecimal value) {
        return new AnalysisScore(value);
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}