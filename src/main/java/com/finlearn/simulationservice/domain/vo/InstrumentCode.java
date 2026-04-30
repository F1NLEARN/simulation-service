package com.finlearn.simulationservice.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstrumentCode {

    private String value;

    // VO 내부 검증은 IllegalArgumentException을 사용한다.
    // 외부 진입점(Entity)의 validate()가 DomainException으로 먼저 검증하므로,
    // VO 예외는 직접 생성 시의 안전망 역할만 한다.
    private InstrumentCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("종목 코드는 blank일 수 없습니다.");
        }
        this.value = value;
    }

    public static InstrumentCode of(String value) {
        return new InstrumentCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}