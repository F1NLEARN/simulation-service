package com.finlearn.simulationservice.application.tradehistory.service;

import com.finlearn.simulationservice.application.tradehistory.dto.response.TradeHistoryDetailResponse;
import com.finlearn.simulationservice.application.tradehistory.dto.response.TradeHistoryListResponse;
import com.finlearn.simulationservice.application.tradehistory.query.GetTradeHistoryListQuery;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeHistory;
import com.finlearn.simulationservice.domain.tradehistory.exception.TradeHistoryForbiddenException;
import com.finlearn.simulationservice.domain.tradehistory.exception.TradeHistoryNotFoundException;
import com.finlearn.simulationservice.domain.tradehistory.repository.TradeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeHistoryQueryService {

    private final TradeHistoryRepository tradeHistoryRepository;

    public Page<TradeHistoryListResponse> getTradeHistoryList(GetTradeHistoryListQuery query) {
        // TODO: ACTIVE 투자 계좌 검증 필요
        //  현재는 accountId 기준으로만 조회하며, 계좌 활성 상태(ACTIVE)는 검증하지 않음.
        //  InvestmentAccount 도메인이 완성된 이후,
        //  해당 accountId의 상태가 ACTIVE인지 확인하는 로직을 추가해야 함.
        //  관련 이슈: https://github.com/F1NLEARN/simulation-service/issues/20
        return tradeHistoryRepository.findAllWithFilters(
                query.accountId(),
                query.tradeType(),
                query.status(),
                query.instrumentCode(),
                query.pageable()
        ).map(TradeHistoryListResponse::from);
    }

    public TradeHistoryDetailResponse getTradeHistoryDetail(UUID accountId, UUID tradeHistoryId) {
        TradeHistory tradeHistory = tradeHistoryRepository.findById(tradeHistoryId)
                .orElseThrow(TradeHistoryNotFoundException::new);

        if (!tradeHistory.getAccountId().equals(accountId)) {
            throw new TradeHistoryForbiddenException();
        }

        return TradeHistoryDetailResponse.from(tradeHistory);
    }
}