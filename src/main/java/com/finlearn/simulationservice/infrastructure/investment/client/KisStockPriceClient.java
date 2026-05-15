package com.finlearn.simulationservice.infrastructure.investment.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finlearn.simulationservice.infrastructure.investment.config.KisApiProperties;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KisStockPriceClient {

    private static final String PRICE_TR_ID = "FHKST01010100";
    private static final long TOKEN_EXPIRY_MARGIN_SECONDS = 60;

    private final KisApiProperties properties;
    private final RestClient restClient;

    private final Object tokenRefreshLock = new Object();
    private volatile String cachedAccessToken;
    private volatile Instant cachedAccessTokenExpiresAt = Instant.EPOCH;

    public KisStockPriceClient(KisApiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public Optional<Long> findCurrentPrice(String stockCode) {
        if (!properties.isReady() || stockCode == null || stockCode.isBlank()) {
            return Optional.empty();
        }

        String normalizedCode = stockCode.trim().toUpperCase();

        try {
            String accessToken = getAccessToken();
            KisPriceResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(properties.getPricePath())
                            .queryParam("FID_COND_MRKT_DIV_CODE", properties.getMarketDivCode())
                            .queryParam("FID_INPUT_ISCD", normalizedCode)
                            .build())
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.set("appkey", properties.getAppKey());
                        headers.set("appsecret", properties.getAppSecret());
                        headers.set("tr_id", PRICE_TR_ID);
                    })
                    .retrieve()
                    .body(KisPriceResponse.class);

            return parseCurrentPrice(response);
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("[KIS] 현재가 조회 실패 - stockCode={}, fallback=DB, reason={}", normalizedCode, e.getMessage());
            return Optional.empty();
        }
    }

    private String getAccessToken() {
        Instant now = Instant.now();
        if (cachedAccessToken != null && now.isBefore(cachedAccessTokenExpiresAt)) {
            return cachedAccessToken;
        }

        synchronized (tokenRefreshLock) {
            now = Instant.now();
            if (cachedAccessToken != null && now.isBefore(cachedAccessTokenExpiresAt)) {
                return cachedAccessToken;
            }

            KisTokenResponse response = restClient.post()
                    .uri(properties.getTokenPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new KisTokenRequest("client_credentials", properties.getAppKey(), properties.getAppSecret()))
                    .retrieve()
                    .body(KisTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new IllegalArgumentException("KIS access token response is empty");
            }

            long expiresIn = response.expiresIn() == null ? 0L : response.expiresIn();
            long cacheSeconds = Math.max(0L, expiresIn - TOKEN_EXPIRY_MARGIN_SECONDS);
            cachedAccessToken = response.accessToken();
            cachedAccessTokenExpiresAt = now.plusSeconds(cacheSeconds);
            return cachedAccessToken;
        }
    }

    private Optional<Long> parseCurrentPrice(KisPriceResponse response) {
        if (response == null || response.output() == null || response.output().currentPrice() == null) {
            return Optional.empty();
        }

        String rawPrice = response.output().currentPrice().trim();
        if (rawPrice.isBlank()) {
            return Optional.empty();
        }

        long price = Long.parseLong(rawPrice.replace(",", ""));
        return price > 0 ? Optional.of(price) : Optional.empty();
    }

    private record KisTokenRequest(
            @JsonProperty("grant_type") String grantType,
            @JsonProperty("appkey") String appKey,
            @JsonProperty("appsecret") String appSecret
    ) {
    }

    private record KisTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn
    ) {
    }

    private record KisPriceResponse(
            @JsonProperty("output") KisPriceOutput output
    ) {
    }

    private record KisPriceOutput(
            @JsonProperty("stck_prpr") String currentPrice
    ) {
    }
}
