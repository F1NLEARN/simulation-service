package com.finlearn.simulationservice.presentation.tradehistory.controller;

import com.finlearn.common.exception.GlobalExceptionAdviceImpl;
import com.finlearn.simulationservice.application.tradehistory.dto.response.TradeHistoryDetailResponse;
import com.finlearn.simulationservice.application.tradehistory.dto.response.TradeHistoryListResponse;
import com.finlearn.simulationservice.application.tradehistory.query.GetTradeHistoryListQuery;
import com.finlearn.simulationservice.application.tradehistory.service.TradeHistoryQueryService;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeStatus;
import com.finlearn.simulationservice.domain.tradehistory.entity.TradeType;
import com.finlearn.simulationservice.domain.tradehistory.exception.TradeHistoryForbiddenException;
import com.finlearn.simulationservice.domain.tradehistory.exception.TradeHistoryNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TradeHistoryControllerTest {

    @Mock
    private TradeHistoryQueryService tradeHistoryQueryService;

    private MockMvc mockMvc;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID TRADE_HISTORY_ID = UUID.randomUUID();
    private static final String INSTRUMENT_CODE = "005930";

    @BeforeEach
    void setUp() {
        TradeHistoryController controller = new TradeHistoryController(tradeHistoryQueryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionAdviceImpl())
                .build();
    }

    @Test
    @DisplayName("거래내역 목록 조회 API는 쿼리 파라미터를 서비스에 올바르게 전달한다.")
    void getTradeHistoryList_requestParamsMappedCorrectly() throws Exception {
        when(tradeHistoryQueryService.getTradeHistoryList(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/trade-histories")
                        .header("X-Account-Id", ACCOUNT_ID.toString())
                        .param("tradeType", "BUY")
                        .param("status", "COMPLETED")
                        .param("instrumentCode", INSTRUMENT_CODE)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("거래 내역 목록 조회 성공"));

        ArgumentCaptor<GetTradeHistoryListQuery> queryCaptor = ArgumentCaptor.forClass(GetTradeHistoryListQuery.class);
        verify(tradeHistoryQueryService).getTradeHistoryList(queryCaptor.capture());

        GetTradeHistoryListQuery captured = queryCaptor.getValue();
        assertThat(captured.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(captured.tradeType()).isEqualTo(TradeType.BUY);
        assertThat(captured.status()).isEqualTo(TradeStatus.COMPLETED);
        assertThat(captured.instrumentCode()).isEqualTo(INSTRUMENT_CODE);
        assertThat(captured.pageable().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("정렬 파라미터 미지정 시 기본 정렬(tradeAt DESC)이 적용된다.")
    void getTradeHistoryList_defaultSort_isTradeAtDesc() throws Exception {
        when(tradeHistoryQueryService.getTradeHistoryList(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/trade-histories")
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isOk());

        ArgumentCaptor<GetTradeHistoryListQuery> queryCaptor = ArgumentCaptor.forClass(GetTradeHistoryListQuery.class);
        verify(tradeHistoryQueryService).getTradeHistoryList(queryCaptor.capture());

        Sort.Order order = queryCaptor.getValue().pageable().getSort().getOrderFor("tradeAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("거래내역 목록 Response DTO가 올바르게 매핑되어 반환된다.")
    void getTradeHistoryList_responseDto_mappedCorrectly() throws Exception {
        TradeHistoryListResponse response = new TradeHistoryListResponse(
                TRADE_HISTORY_ID, INSTRUMENT_CODE, TradeType.BUY, TradeStatus.COMPLETED,
                5L, 75_000L, 375_000L, LocalDateTime.of(2026, 4, 29, 10, 0)
        );
        when(tradeHistoryQueryService.getTradeHistoryList(any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/trade-histories")
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].tradeHistoryId").value(TRADE_HISTORY_ID.toString()))
                .andExpect(jsonPath("$.data.content[0].instrumentCode").value(INSTRUMENT_CODE))
                .andExpect(jsonPath("$.data.content[0].tradeType").value("BUY"))
                .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.content[0].quantity").value(5))
                .andExpect(jsonPath("$.data.content[0].tradePrice").value(75_000))
                .andExpect(jsonPath("$.data.content[0].totalTradeAmount").value(375_000));
    }

    @Test
    @DisplayName("거래내역 상세 Response DTO가 올바르게 매핑되어 반환된다.")
    void getTradeHistoryDetail_responseDto_mappedCorrectly() throws Exception {
        UUID seasonId = UUID.randomUUID();
        TradeHistoryDetailResponse response = new TradeHistoryDetailResponse(
                TRADE_HISTORY_ID, ACCOUNT_ID, seasonId, 1,
                INSTRUMENT_CODE, TradeType.BUY, TradeStatus.COMPLETED,
                5L, 75_000L, 375_000L, 625_000L,
                LocalDateTime.of(2026, 4, 29, 10, 0)
        );
        when(tradeHistoryQueryService.getTradeHistoryDetail(eq(ACCOUNT_ID), eq(TRADE_HISTORY_ID)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/trade-histories/{tradeHistoryId}", TRADE_HISTORY_ID)
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("거래 내역 상세 조회 성공"))
                .andExpect(jsonPath("$.data.tradeHistoryId").value(TRADE_HISTORY_ID.toString()))
                .andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.data.instrumentCode").value(INSTRUMENT_CODE))
                .andExpect(jsonPath("$.data.tradeType").value("BUY"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.cashBalanceAfterTrade").value(625_000));
    }

    @Test
    @DisplayName("존재하지 않는 거래 ID 조회 시 404를 반환한다.")
    void getTradeHistoryDetail_notFound_returns404() throws Exception {
        when(tradeHistoryQueryService.getTradeHistoryDetail(any(), any()))
                .thenThrow(new TradeHistoryNotFoundException());

        mockMvc.perform(get("/api/v1/trade-histories/{tradeHistoryId}", TRADE_HISTORY_ID)
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("다른 사용자의 거래내역 조회 시 403을 반환한다.")
    void getTradeHistoryDetail_forbidden_returns403() throws Exception {
        when(tradeHistoryQueryService.getTradeHistoryDetail(any(), any()))
                .thenThrow(new TradeHistoryForbiddenException());

        mockMvc.perform(get("/api/v1/trade-histories/{tradeHistoryId}", TRADE_HISTORY_ID)
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isForbidden());
    }
}