package com.finlearn.simulationservice.domain.investment.repository;

import com.finlearn.simulationservice.domain.investment.entity.FavoriteStock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteStockRepository extends JpaRepository<FavoriteStock, UUID> {

    boolean existsByUserIdAndSymbol(UUID userId, String symbol);

    List<FavoriteStock> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<FavoriteStock> findByUserIdAndSymbol(UUID userId, String symbol);
}
