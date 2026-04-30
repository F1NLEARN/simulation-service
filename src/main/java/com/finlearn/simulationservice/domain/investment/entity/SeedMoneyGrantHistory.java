package com.finlearn.simulationservice.domain.investment.entity;

import com.finlearn.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "seed_money_grant_histories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_seed_money_grant_history_season_user", columnNames = {"season_id", "user_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeedMoneyGrantHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID seedMoneyGrantHistoryId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Builder
    private SeedMoneyGrantHistory(UUID userId, UUID seasonId, UUID accountId, BigDecimal amount, LocalDateTime grantedAt) {
        this.userId = userId;
        this.seasonId = seasonId;
        this.accountId = accountId;
        this.amount = amount;
        this.grantedAt = grantedAt;
    }

    public static SeedMoneyGrantHistory grant(UUID userId, UUID seasonId, UUID accountId, BigDecimal amount, LocalDateTime grantedAt) {
        return SeedMoneyGrantHistory.builder()
                .userId(userId)
                .seasonId(seasonId)
                .accountId(accountId)
                .amount(amount)
                .grantedAt(grantedAt)
                .build();
    }
}
