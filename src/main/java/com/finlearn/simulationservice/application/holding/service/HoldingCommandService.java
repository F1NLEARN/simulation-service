package com.finlearn.simulationservice.application.holding.service;

import com.finlearn.simulationservice.domain.holding.command.CreateHoldingCommand;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.exception.HoldingNotFoundException;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import com.finlearn.simulationservice.domain.investment.entity.StockItem;
import com.finlearn.simulationservice.domain.investment.repository.StockItemRepository;
import com.finlearn.simulationservice.domain.tradehistory.event.StockBoughtEvent;
import com.finlearn.simulationservice.domain.tradehistory.event.StockSoldEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class HoldingCommandService {

    private final HoldingRepository holdingRepository;
    private final StockItemRepository stockItemRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyBuyResult(StockBoughtEvent event) {
        holdingRepository.findByAccountIdAndInstrumentCode(event.accountId(), event.instrumentCode())
                .ifPresentOrElse(
                        holding -> {
                            holding.addBuy(event.quantity(), event.tradePrice());
                            holdingRepository.save(holding);
                        },
                        () -> {
                            String holdingName = stockItemRepository.findByStockCode(event.instrumentCode())
                                    .map(StockItem::getStockName)
                                    .orElse(event.instrumentCode());
                            holdingRepository.save(Holding.create(new CreateHoldingCommand(
                                    event.accountId(),
                                    holdingName,
                                    event.seasonId(),
                                    event.seasonNumber(),
                                    event.instrumentCode(),
                                    event.quantity(),
                                    event.tradePrice(),
                                    event.tradePrice()
                            )));
                        }
                );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applySellResult(StockSoldEvent event) {
        Holding holding = holdingRepository.findByAccountIdAndInstrumentCode(
                        event.accountId(), event.instrumentCode())
                .orElseThrow(HoldingNotFoundException::new);
        holding.sell(event.quantity());
        holdingRepository.save(holding);
    }
}