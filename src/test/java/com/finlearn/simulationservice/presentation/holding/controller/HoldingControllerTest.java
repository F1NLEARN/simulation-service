package com.finlearn.simulationservice.presentation.holding.controller;

import com.finlearn.common.exception.GlobalExceptionAdviceImpl;
import com.finlearn.simulationservice.application.holding.dto.response.HoldingDetailResponse;
import com.finlearn.simulationservice.application.holding.dto.response.HoldingListResponse;
import com.finlearn.simulationservice.application.holding.service.HoldingQueryService;
import com.finlearn.simulationservice.domain.holding.exception.HoldingForbiddenException;
import com.finlearn.simulationservice.domain.holding.exception.HoldingNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HoldingControllerTest {

    @Mock
    private HoldingQueryService holdingQueryService;

    private MockMvc mockMvc;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID HOLDING_ID = UUID.randomUUID();
    private static final UUID SEASON_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        HoldingController controller = new HoldingController(holdingQueryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionAdviceImpl())
                .build();
    }

    @Test
    @DisplayName("보유 종목 목록 조회 API는 종목 목록을 반환한다.")
    void getHoldingList_returnsHoldingList() throws Exception {
        HoldingListResponse response = new HoldingListResponse(
                HOLDING_ID, "삼성전자", "005930",
                10L, 75_000L, 80_000L, 800_000L, 50_000L, new BigDecimal("6.67")
        );
        when(holdingQueryService.getHoldingList(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/holdings")
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("보유 종목 목록 조회 성공"))
                .andExpect(jsonPath("$.data[0].holdingId").value(HOLDING_ID.toString()))
                .andExpect(jsonPath("$.data[0].holdingName").value("삼성전자"))
                .andExpect(jsonPath("$.data[0].instrumentCode").value("005930"))
                .andExpect(jsonPath("$.data[0].quantity").value(10))
                .andExpect(jsonPath("$.data[0].currentPrice").value(80_000));
    }

    @Test
    @DisplayName("보유 종목 상세 조회 API는 모든 필드를 반환한다.")
    void getHoldingDetail_returnsAllFields() throws Exception {
        HoldingDetailResponse response = new HoldingDetailResponse(
                HOLDING_ID, ACCOUNT_ID, SEASON_ID, 1,
                "삼성전자", "005930",
                10L, 75_000L, 80_000L, 750_000L, 800_000L, 50_000L, new BigDecimal("6.67")
        );
        when(holdingQueryService.getHoldingDetail(ACCOUNT_ID, HOLDING_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/holdings/{holdingId}", HOLDING_ID)
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("보유 종목 상세 조회 성공"))
                .andExpect(jsonPath("$.data.holdingId").value(HOLDING_ID.toString()))
                .andExpect(jsonPath("$.data.instrumentCode").value("005930"))
                .andExpect(jsonPath("$.data.totalBuyAmount").value(750_000))
                .andExpect(jsonPath("$.data.valuationAmount").value(800_000))
                .andExpect(jsonPath("$.data.unrealizedProfitLoss").value(50_000));
    }

    @Test
    @DisplayName("존재하지 않는 holdingId로 상세 조회하면 404를 반환한다.")
    void getHoldingDetail_whenNotFound_returns404() throws Exception {
        when(holdingQueryService.getHoldingDetail(ACCOUNT_ID, HOLDING_ID))
                .thenThrow(new HoldingNotFoundException());

        mockMvc.perform(get("/api/v1/holdings/{holdingId}", HOLDING_ID)
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("보유 종목을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("다른 계좌의 보유 종목을 조회하면 403을 반환한다.")
    void getHoldingDetail_whenForbidden_returns403() throws Exception {
        when(holdingQueryService.getHoldingDetail(ACCOUNT_ID, HOLDING_ID))
                .thenThrow(new HoldingForbiddenException());

        mockMvc.perform(get("/api/v1/holdings/{holdingId}", HOLDING_ID)
                        .header("X-Account-Id", ACCOUNT_ID.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("해당 보유 종목에 접근할 권한이 없습니다."));
    }
}
