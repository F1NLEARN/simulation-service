package com.finlearn.simulationservice.domain.holding.repository;

import com.finlearn.simulationservice.domain.holding.entity.Holding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldingRepository {

    Holding save(Holding holding);

    Optional<Holding> findById(UUID holdingId);

    Optional<Holding> findByAccountIdAndInstrumentCode(UUID accountId, String instrumentCode);

    List<Holding> findAllByAccountId(UUID accountId);
}