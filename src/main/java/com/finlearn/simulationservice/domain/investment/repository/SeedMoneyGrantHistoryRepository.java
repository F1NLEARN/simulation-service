package com.finlearn.simulationservice.domain.investment.repository;

import com.finlearn.simulationservice.domain.investment.entity.SeedMoneyGrantHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeedMoneyGrantHistoryRepository extends JpaRepository<SeedMoneyGrantHistory, UUID> {
}
