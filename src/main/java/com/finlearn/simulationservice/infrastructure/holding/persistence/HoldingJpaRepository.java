package com.finlearn.simulationservice.infrastructure.holding.persistence;

import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldingJpaRepository extends JpaRepository<Holding, UUID>, HoldingRepository {

    @Override
    @Query("SELECT h FROM Holding h WHERE h.accountId = :accountId AND h.instrumentCode.value = :instrumentCode")
    Optional<Holding> findByAccountIdAndInstrumentCode(@Param("accountId") UUID accountId,
                                                       @Param("instrumentCode") String instrumentCode);

    @Override
    List<Holding> findAllByAccountId(UUID accountId);
}