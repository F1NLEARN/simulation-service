package com.finlearn.simulationservice.infrastructure.investment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "external.kis")
public class KisApiProperties {

    private boolean enabled = false;
    private String baseUrl = "https://openapi.koreainvestment.com:9443";
    private String tokenPath = "/oauth2/tokenP";
    private String pricePath = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private String appKey;
    private String appSecret;
    private String marketDivCode = "J";

    public boolean isReady() {
        return enabled
                && appKey != null && !appKey.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }
}
