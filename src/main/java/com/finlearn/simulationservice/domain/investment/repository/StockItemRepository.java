package com.finlearn.simulationservice.domain.investment.repository;

import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.enums.StockAssetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockItemRepository extends JpaRepository<StockItem, UUID> {

    List<StockItem> findAllByOrderByStockCodeAsc();

    List<StockItem> findAllByAssetTypeOrderByStockCodeAsc(StockAssetType assetType);

    List<StockItem> findAllByCurrentPriceIsNotNullOrderByStockCodeAsc();

    List<StockItem> findAllByAssetTypeAndCurrentPriceIsNotNullOrderByStockCodeAsc(StockAssetType assetType);

    List<StockItem> findAllByStockCodeIn(List<String> stockCodes);

    Optional<StockItem> findByStockCode(String stockCode);

    Page<StockItem> findByAssetType(StockAssetType assetType, Pageable pageable);

    @Query("""
            SELECT s
            FROM StockItem s
            WHERE (
                    LOWER(s.stockName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(s.stockCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<StockItem> searchStocksByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            SELECT s
            FROM StockItem s
            WHERE s.assetType = :assetType
              AND (
                    LOWER(s.stockName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(s.stockCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<StockItem> searchStocksByAssetTypeAndKeyword(
            @Param("assetType") StockAssetType assetType,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
