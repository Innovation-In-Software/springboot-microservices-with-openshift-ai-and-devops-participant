package com.md287.risk.model;

import com.md287.risk.config.Md287Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Map;

public class ModelClient {

    private static final Logger log = LoggerFactory.getLogger(ModelClient.class);

    private final RestClient restClient;
    private final Md287Properties.Model model;

    public ModelClient(RestClient restClient, Md287Properties properties) {
        this.restClient = restClient;
        this.model = properties.model();
    }

    public ModelScore score(String transactionId, String accountId, BigDecimal amount, String currency, String type) {
        try {
            ModelApiResponse body = restClient.post()
                    .uri(model.scorePath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + model.apiKey())
                    .body(Map.of(
                            "transactionId", transactionId,
                            "accountId", accountId,
                            "amount", amount,
                            "currency", currency,
                            "type", type
                    ))
                    .retrieve()
                    .body(ModelApiResponse.class);
            if (body == null || body.score() == null) {
                log.warn("Model returned an empty body transactionId={}", transactionId);
                return ModelScore.unavailable();
            }
            log.info("Model scored transactionId={} score={} modelName={} modelVersion={}",
                    transactionId, body.score(), body.modelName(), body.modelVersion());
            return ModelScore.ok(body.score(), body.modelName(), body.modelVersion());
        } catch (ResourceAccessException ex) {
            log.warn("Model timeout or connection failure transactionId={}", transactionId);
            return ModelScore.timeout();
        } catch (RestClientException ex) {
            log.warn("Model unavailable transactionId={}", transactionId);
            return ModelScore.unavailable();
        }
    }

    public record ModelApiResponse(Integer score, String modelName, String modelVersion) {
    }
}
