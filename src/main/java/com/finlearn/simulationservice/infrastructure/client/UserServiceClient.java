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
public class UserServiceClient {

    private static final ParameterizedTypeReference<ApiResponse<UserData>> USER_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public UserServiceClient(@Qualifier("loadBalanced") RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://user-service").build();
    }

    public Optional<String> getNickname(UUID userId) {
        try {
            ApiResponse<UserData> response = restClient.get()
                    .uri("/api/internal/users/{userId}", userId)
                    .retrieve()
                    .body(USER_TYPE);

            if (response == null || response.data() == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(response.data().nickname());
        } catch (RestClientException e) {
            log.warn("[UserServiceClient] 사용자 닉네임 조회 실패 - userId={}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    private record ApiResponse<T>(@JsonProperty("data") T data) {}

    private record UserData(@JsonProperty("nickname") String nickname) {}
}