package com.finlearn.simulationservice.infrastructure.client.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class LoadBalancedClientConfig {

    /**
     * Spring AI(OpenAI 클라이언트)가 qualifier 없이 주입받는 기본 빌더.
     * LoadBalancer 없이 외부 API를 직접 호출한다.
     */
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * 내부 마이크로서비스 호출 전용 (Eureka + LoadBalancer 사용).
     * UserServiceClient, SeasonServiceClient 등이 @Qualifier("loadBalanced")로 주입받는다.
     */
    @Bean
    @LoadBalanced
    @Qualifier("loadBalanced")
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}