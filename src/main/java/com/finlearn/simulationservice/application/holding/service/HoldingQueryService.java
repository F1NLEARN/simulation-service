package com.finlearn.simulationservice.application.holding.service;

import com.finlearn.simulationservice.application.holding.dto.response.HoldingDetailResponse;
import com.finlearn.simulationservice.application.holding.dto.response.HoldingListResponse;
import com.finlearn.simulationservice.application.holding.query.GetHoldingListQuery;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.exception.HoldingForbiddenException;
import com.finlearn.simulationservice.domain.holding.exception.HoldingNotFoundException;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoldingQueryService {

    private final HoldingRepository holdingRepository;

    public List<HoldingListResponse> getHoldingList(GetHoldingListQuery query) {
        // TODO: ACTIVE 투자 계좌 검증 필요
        //  현재는 accountId 기준으로만 조회하며, 계좌 활성 상태(ACTIVE)는 검증하지 않음.
        //  InvestmentAccount 도메인이 완성된 이후,
        //  해당 accountId의 상태가 ACTIVE인지 확인하는 로직을 추가해야 함.
        return holdingRepository.findAllWithFilter(query.accountId(), query.instrumentCode())
                .stream()
                .map(HoldingListResponse::from)
                .toList();
    }

    public HoldingDetailResponse getHoldingDetail(UUID accountId, UUID holdingId) {
        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(HoldingNotFoundException::new);

        if (!holding.getAccountId().equals(accountId)) {
            throw new HoldingForbiddenException();
        }

        return HoldingDetailResponse.from(holding);
    }
}
