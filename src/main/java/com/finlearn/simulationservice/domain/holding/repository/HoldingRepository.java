package com.finlearn.simulationservice.domain.holding.repository;

import com.finlearn.simulationservice.domain.holding.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {

    Optional<Holding> findByAccountIdAndInstrumentCode(UUID accountId, String instrumentCode);

    List<Holding> findAllByAccountId(UUID accountId);
}
