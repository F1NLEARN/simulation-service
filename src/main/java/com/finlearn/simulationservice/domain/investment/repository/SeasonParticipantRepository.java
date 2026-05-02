package com.finlearn.simulationservice.domain.investment.repository;

import com.finlearn.simulationservice.domain.investment.entity.SeasonParticipant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonParticipantRepository extends JpaRepository<SeasonParticipant, UUID> {

    Optional<SeasonParticipant> findBySeasonIdAndUserId(UUID seasonId, UUID userId);
}
