package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.simulationservice.domain.investment.enums.SeedMoneyGrantType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seed_money_grant_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeedMoneyGrantHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "grant_history_id")
    private UUID grantHistoryId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Column(name = "season_number", nullable = false)
    private int seasonNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_type", nullable = false, length = 20)
    private SeedMoneyGrantType grantType;

    @Column(name = "grant_amount", nullable = false)
    private long grantAmount;

    @Column(name = "grant_reason", nullable = false, length = 255)
    private String grantReason;

    @Column(name = "grant_at", nullable = false)
    private LocalDateTime grantAt;

    @Builder
    private SeedMoneyGrantHistory(UUID accountId, UUID seasonId, int seasonNumber,
                                  SeedMoneyGrantType grantType, long grantAmount,
                                  String grantReason, LocalDateTime grantAt) {
        this.accountId = accountId;
        this.seasonId = seasonId;
        this.seasonNumber = seasonNumber;
        this.grantType = grantType;
        this.grantAmount = grantAmount;
        this.grantReason = grantReason;
        this.grantAt = grantAt;
    }

    public static SeedMoneyGrantHistory grant(UUID accountId, UUID seasonId, int seasonNumber,
                                              SeedMoneyGrantType grantType, long grantAmount,
                                              String grantReason, LocalDateTime grantAt) {
        return SeedMoneyGrantHistory.builder()
                .accountId(accountId)
                .seasonId(seasonId)
                .seasonNumber(seasonNumber)
                .grantType(grantType)
                .grantAmount(grantAmount)
                .grantReason(grantReason)
                .grantAt(grantAt)
                .build();
    }
}
