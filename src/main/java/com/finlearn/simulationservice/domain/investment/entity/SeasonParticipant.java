package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "season_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeasonParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID seasonParticipantId;

    @Column(nullable = false)
    private UUID seasonId;

    @Column(nullable = false)
    private UUID userId;

    private SeasonParticipant(UUID seasonId, UUID userId) {
        this.seasonId = seasonId;
        this.userId = userId;
    }

    public static SeasonParticipant create(UUID seasonId, UUID userId) {
        return new SeasonParticipant(seasonId, userId);
    }
}
