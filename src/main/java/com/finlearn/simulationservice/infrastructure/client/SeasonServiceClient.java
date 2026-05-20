package com.finlearn.simulationservice.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class SeasonServiceClient {

    private static final ParameterizedTypeReference<ApiResponse<SeasonData>> SEASON_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public SeasonServiceClient(@Qualifier("loadBalanced") RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://season-service").build();
    }

    public Optional<CurrentSeasonInfo> getCurrentSeason() {
        try {
            ApiResponse<SeasonData> response = restClient.get()
                    .uri("/api/v1/seasons/current")
                    .retrieve()
                    .body(SEASON_TYPE);

            if (response == null || response.data() == null || response.data().seasonId() == null) {
                return Optional.empty();
            }
            SeasonData data = response.data();
            return Optional.of(new CurrentSeasonInfo(data.seasonId(), data.seasonNumber()));
        } catch (RestClientException e) {
            log.warn("[SeasonServiceClient] 현재 시즌 조회 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record CurrentSeasonInfo(UUID seasonId, int seasonNumber) {}

    private record ApiResponse<T>(@JsonProperty("data") T data) {}

    private record SeasonData(
            @JsonProperty("seasonId") UUID seasonId,
            @JsonProperty("seasonNumber") Integer seasonNumber
    ) {}
}