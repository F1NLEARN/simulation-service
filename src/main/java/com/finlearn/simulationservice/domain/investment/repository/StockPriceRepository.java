package com.finlearn.simulationservice.domain.investment.repository;

import java.util.Optional;

public interface StockPriceRepository {

    Optional<Long> findCurrentPrice(String instrumentCode);
}
