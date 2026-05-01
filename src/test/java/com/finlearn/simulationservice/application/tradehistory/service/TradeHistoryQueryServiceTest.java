package com.finlearn.simulationservice.application.tradehistory.service;

import com.finlearn.simulationservice.application.tradehistory.dto.response.TradeHistoryDetailResponse;
import com.finlearn.simulationservice.application.tradehistory.dto.response.TradeHistoryListResponse;
import com.finlearn.simulationservice.application.tradehistory.query.GetTradeHistoryListQuery;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeHistory;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeStatus;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeType;
import com.finlearn.simulationservice.domain.tradehistory.exception.TradeHistoryForbiddenException;
import com.finlearn.simulationservice.domain.tradehistory.exception.TradeHistoryNotFoundException;
import com.finlearn.simulationservice.domain.tradehistory.repository.TradeHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeHistoryQueryServiceTest {

    @Mock
    private TradeHistoryRepository tradeHistoryRepository;

    @InjectMocks
    private TradeHistoryQueryService tradeHistoryQueryService;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID ANOTHER_ACCOUNT_ID = UUID.randomUUID();
    private static final String INSTRUMENT_CODE = "005930";

    private TradeHistory buyHistory(UUID accountId, String instrumentCode, LocalDateTime tradeAt) {
        TradeHistory th = TradeHistory.buy(
                accountId, UUID.randomUUID(), 1, instrumentCode,
                5L, 75_000L, tradeAt, 625_000L
        );
        ReflectionTestUtils.setField(th, "tradeHistoryId", UUID.randomUUID());
        return th;
    }

    private TradeHistory sellHistory(UUID accountId, String instrumentCode, LocalDateTime tradeAt) {
        TradeHistory th = TradeHistory.sell(
                accountId, UUID.randomUUID(), 1, instrumentCode,
                5L, 75_000L, tradeAt, 625_000L
        );
        ReflectionTestUtils.setField(th, "tradeHistoryId", UUID.randomUUID());
        return th;
    }

    @Test
    @DisplayName("accountId 기준으로 거래내역이 조회된다.")
    void getTradeHistoryList_byAccountId() {
        TradeHistory history = buyHistory(ACCOUNT_ID, INSTRUMENT_CODE, LocalDateTime.now());
        Pageable pageable = PageRequest.of(0, 20);
        GetTradeHistoryListQuery query = new GetTradeHistoryListQuery(ACCOUNT_ID, null, null, null, pageable);

        when(tradeHistoryRepository.findAllWithFilters(eq(ACCOUNT_ID), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(history)));

        Page<TradeHistoryListResponse> result = tradeHistoryQueryService.getTradeHistoryList(query);

        assertThat(result.getContent()).hasSize(1);
        verify(tradeHistoryRepository).findAllWithFilters(eq(ACCOUNT_ID), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Pageable에 포함된 최신순 정렬 조건이 Repository에 그대로 전달된다.")
    void getTradeHistoryList_passesPageableWithSort_toRepository() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "tradeAt"));
        GetTradeHistoryListQuery query = new GetTradeHistoryListQuery(ACCOUNT_ID, null, null, null, pageable);

        when(tradeHistoryRepository.findAllWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        tradeHistoryQueryService.getTradeHistoryList(query);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(tradeHistoryRepository).findAllWithFilters(any(), any(), any(), any(), pageableCaptor.capture());

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("tradeAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("거래 타입 BUY 조건이 Repository에 전달된다.")
    void getTradeHistoryList_withBuyFilter_passesTradeTypeBuy() {
        Pageable pageable = PageRequest.of(0, 20);
        GetTradeHistoryListQuery query = new GetTradeHistoryListQuery(ACCOUNT_ID, TradeType.BUY, null, null, pageable);

        when(tradeHistoryRepository.findAllWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        tradeHistoryQueryService.getTradeHistoryList(query);

        verify(tradeHistoryRepository).findAllWithFilters(any(), eq(TradeType.BUY), any(), any(), any());
    }

    @Test
    @DisplayName("거래 타입 SELL 조건이 Repository에 전달된다.")
    void getTradeHistoryList_withSellFilter_passesTradeTypeSell() {
        Pageable pageable = PageRequest.of(0, 20);
        GetTradeHistoryListQuery query = new GetTradeHistoryListQuery(ACCOUNT_ID, TradeType.SELL, null, null, pageable);

        when(tradeHistoryRepository.findAllWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        tradeHistoryQueryService.getTradeHistoryList(query);

        verify(tradeHistoryRepository).findAllWithFilters(any(), eq(TradeType.SELL), any(), any(), any());
    }

    @Test
    @DisplayName("거래 상태 조건이 Repository에 전달된다.")
    void getTradeHistoryList_withStatusFilter_passesStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        GetTradeHistoryListQuery query = new GetTradeHistoryListQuery(ACCOUNT_ID, null, TradeStatus.COMPLETED, null, pageable);

        when(tradeHistoryRepository.findAllWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        tradeHistoryQueryService.getTradeHistoryList(query);

        verify(tradeHistoryRepository).findAllWithFilters(any(), any(), eq(TradeStatus.COMPLETED), any(), any());
    }

    @Test
    @DisplayName("종목 코드 조건이 Repository에 전달된다.")
    void getTradeHistoryList_withInstrumentCodeFilter_passesCode() {
        Pageable pageable = PageRequest.of(0, 20);
        GetTradeHistoryListQuery query = new GetTradeHistoryListQuery(ACCOUNT_ID, null, null, INSTRUMENT_CODE, pageable);

        when(tradeHistoryRepository.findAllWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        tradeHistoryQueryService.getTradeHistoryList(query);

        verify(tradeHistoryRepository).findAllWithFilters(any(), any(), any(), eq(INSTRUMENT_CODE), any());
    }

    @Test
    @DisplayName("페이징 조건이 Repository에 그대로 전달된다.")
    void getTradeHistoryList_withPaging_passesPageable() {
        Pageable pageable = PageRequest.of(2, 5);
        GetTradeHistoryListQuery query = new GetTradeHistoryListQuery(ACCOUNT_ID, null, null, null, pageable);

        when(tradeHistoryRepository.findAllWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        tradeHistoryQueryService.getTradeHistoryList(query);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(tradeHistoryRepository).findAllWithFilters(any(), any(), any(), any(), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("존재하지 않는 거래 ID 조회 시 TradeHistoryNotFoundException이 발생한다.")
    void getTradeHistoryDetail_withNotFoundId_throwsNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(tradeHistoryRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeHistoryQueryService.getTradeHistoryDetail(ACCOUNT_ID, unknownId))
                .isInstanceOf(TradeHistoryNotFoundException.class);
    }

    @Test
    @DisplayName("다른 사용자의 거래내역 조회 시 TradeHistoryForbiddenException이 발생한다.")
    void getTradeHistoryDetail_withDifferentAccountId_throwsForbiddenException() {
        TradeHistory history = buyHistory(ANOTHER_ACCOUNT_ID, INSTRUMENT_CODE, LocalDateTime.now());
        UUID tradeHistoryId = history.getTradeHistoryId();

        when(tradeHistoryRepository.findById(tradeHistoryId)).thenReturn(Optional.of(history));

        assertThatThrownBy(() -> tradeHistoryQueryService.getTradeHistoryDetail(ACCOUNT_ID, tradeHistoryId))
                .isInstanceOf(TradeHistoryForbiddenException.class);
    }
}