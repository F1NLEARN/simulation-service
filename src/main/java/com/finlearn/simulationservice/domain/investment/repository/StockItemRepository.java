package com.finlearn.simulationservice.domain.investment.repository;

import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockItemRepository extends JpaRepository<StockItem, UUID> {

    List<StockItem> findAllByOrderByStockCodeAsc();

    List<StockItem> findAllByAssetTypeOrderByStockCodeAsc(StockAssetType assetType);

    Optional<StockItem> findByStockCode(String stockCode);
}
