package com.ticketfilms.ms_boletos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // URL base de ms-asientos
    @Value("${ms-asientos.base-url}")
    private String msAsientosBaseUrl;

    @Bean
    public RestClient asientosRestClient() {
        return RestClient.builder()
                .baseUrl(msAsientosBaseUrl)
                .build();
    }
}
