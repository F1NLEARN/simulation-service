package com.finlearn.simulationservice.domain.investment.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SeasonParticipant {

    @Column(name = "investor_id", nullable = false)
    private UUID investorId;

    @Column(name = "investor_name", nullable = false, length = 10)
    private String investorName;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Column(name = "season_number", nullable = false)
    private int seasonNumber;
}
