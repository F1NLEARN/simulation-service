package com.finlearn.simulationservice.application.holding.service;

import com.finlearn.simulationservice.application.holding.dto.response.HoldingDetailResponse;
import com.finlearn.simulationservice.application.holding.dto.response.HoldingListResponse;
import com.finlearn.simulationservice.application.holding.query.GetHoldingListQuery;
import com.finlearn.simulationservice.domain.holding.command.CreateHoldingCommand;
import com.finlearn.simulationservice.domain.holding.entity.Holding;
import com.finlearn.simulationservice.domain.holding.exception.HoldingForbiddenException;
import com.finlearn.simulationservice.domain.holding.exception.HoldingNotFoundException;
import com.finlearn.simulationservice.domain.holding.repository.HoldingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldingQueryServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @InjectMocks
    private HoldingQueryService holdingQueryService;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();
    private static final UUID HOLDING_ID = UUID.randomUUID();

    private Holding createHolding(UUID accountId, String instrumentCode) {
        Holding holding = Holding.create(new CreateHoldingCommand(
                accountId, "삼성전자", SEASON_ID, 1,
                instrumentCode, 10L, 75_000L, 80_000L
        ));
        ReflectionTestUtils.setField(holding, "holdingId", HOLDING_ID);
        return holding;
    }

    @Test
    @DisplayName("보유 종목 목록 조회 시 필터 없이 전체 목록을 반환한다.")
    void getHoldingList_withoutFilter_returnsAll() {
        Holding holding = createHolding(ACCOUNT_ID, "005930");
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, null)).thenReturn(List.of(holding));

        List<HoldingListResponse> result = holdingQueryService.getHoldingList(
                new GetHoldingListQuery(ACCOUNT_ID, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).instrumentCode()).isEqualTo("005930");
        assertThat(result.get(0).quantity()).isEqualTo(10L);
        assertThat(result.get(0).currentPrice()).isEqualTo(80_000L);
    }

    @Test
    @DisplayName("종목 코드 필터를 전달하면 해당 종목만 반환한다.")
    void getHoldingList_withInstrumentCodeFilter_returnsFiltered() {
        Holding holding = createHolding(ACCOUNT_ID, "005930");
        when(holdingRepository.findAllWithFilter(ACCOUNT_ID, "005930")).thenReturn(List.of(holding));

        List<HoldingListResponse> result = holdingQueryService.getHoldingList(
                new GetHoldingListQuery(ACCOUNT_ID, "005930"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).instrumentCode()).isEqualTo("005930");
    }

    @Test
    @DisplayName("보유 종목 상세 조회 시 holdingId가 없으면 HoldingNotFoundException을 던진다.")
    void getHoldingDetail_whenNotFound_throwsHoldingNotFoundException() {
        when(holdingRepository.findById(HOLDING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> holdingQueryService.getHoldingDetail(ACCOUNT_ID, HOLDING_ID))
                .isInstanceOf(HoldingNotFoundException.class);
    }

    @Test
    @DisplayName("보유 종목 상세 조회 시 accountId가 일치하지 않으면 HoldingForbiddenException을 던진다.")
    void getHoldingDetail_whenAccountIdMismatch_throwsHoldingForbiddenException() {
        UUID otherAccountId = UUID.randomUUID();
        Holding holding = createHolding(ACCOUNT_ID, "005930");
        when(holdingRepository.findById(HOLDING_ID)).thenReturn(Optional.of(holding));

        assertThatThrownBy(() -> holdingQueryService.getHoldingDetail(otherAccountId, HOLDING_ID))
                .isInstanceOf(HoldingForbiddenException.class);
    }

    @Test
    @DisplayName("보유 종목 상세 조회 성공 시 모든 필드를 반환한다.")
    void getHoldingDetail_success_returnsAllFields() {
        Holding holding = createHolding(ACCOUNT_ID, "005930");
        when(holdingRepository.findById(HOLDING_ID)).thenReturn(Optional.of(holding));

        HoldingDetailResponse result = holdingQueryService.getHoldingDetail(ACCOUNT_ID, HOLDING_ID);

        assertThat(result.holdingId()).isEqualTo(HOLDING_ID);
        assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.instrumentCode()).isEqualTo("005930");
        assertThat(result.quantity()).isEqualTo(10L);
        assertThat(result.averageBuyPrice()).isEqualTo(75_000L);
        assertThat(result.currentPrice()).isEqualTo(80_000L);
        assertThat(result.totalBuyAmount()).isEqualTo(750_000L);
        assertThat(result.valuationAmount()).isEqualTo(800_000L);
        assertThat(result.unrealizedProfitLoss()).isEqualTo(50_000L);
    }
}
