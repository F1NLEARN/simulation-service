package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seed_money_grant_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeedMoneyGrantHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID seedMoneyGrantHistoryId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID seasonId;

    @Column(nullable = false)
    private UUID investmentAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grantedAmount;

    @Column(nullable = false)
    private LocalDateTime grantedAt;

    private SeedMoneyGrantHistory(
            UUID userId,
            UUID seasonId,
            UUID investmentAccountId,
            BigDecimal grantedAmount,
            LocalDateTime grantedAt
    ) {
        this.userId = userId;
        this.seasonId = seasonId;
        this.investmentAccountId = investmentAccountId;
        this.grantedAmount = grantedAmount;
        this.grantedAt = grantedAt;
    }

    public static SeedMoneyGrantHistory grant(
            UUID userId,
            UUID seasonId,
            UUID investmentAccountId,
            BigDecimal grantedAmount,
            LocalDateTime grantedAt
    ) {
        return new SeedMoneyGrantHistory(
                userId,
                seasonId,
                investmentAccountId,
                grantedAmount,
                grantedAt
        );
    }
}
