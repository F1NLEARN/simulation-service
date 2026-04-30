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