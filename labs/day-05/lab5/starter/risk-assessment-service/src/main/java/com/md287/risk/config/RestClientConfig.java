package com.md287.risk.config;

import com.md287.risk.model.ModelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient modelRestClient(RestClient.Builder builder, Md287Properties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(3));
        return builder
                .baseUrl(properties.model().baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Bean
    public ModelClient modelClient(RestClient modelRestClient, Md287Properties properties) {
        return new ModelClient(modelRestClient, properties);
    }
}
