package com.sungjiduk.backend.payment.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
public class TossPaymentsClient {

    private static final String CONFIRM_URI = "/v1/payments/confirm";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String secretKey;

    public TossPaymentsClient(
            @Value("${seongjiduk.tosspayments.secret-key:}") String secretKey
    ) {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.secretKey = secretKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.tosspayments.com")
                .build();
    }

    public TossPaymentConfirmResult confirm(String paymentKey, String orderId, Long amount) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new TossPaymentConfirmException(
                    "TOSS_SECRET_KEY_MISSING",
                    "토스페이먼츠 시크릿 키가 설정되지 않았습니다.",
                    null
            );
        }

        try {
            String rawPayload = restClient.post()
                    .uri(CONFIRM_URI)
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "paymentKey", paymentKey,
                            "orderId", orderId,
                            "amount", amount
                    ))
                    .retrieve()
                    .body(String.class);
            TossPaymentConfirmResult result = objectMapper.readValue(rawPayload, TossPaymentConfirmResult.class);
            return new TossPaymentConfirmResult(
                    result.paymentKey(),
                    result.orderId(),
                    result.status(),
                    result.method(),
                    result.totalAmount(),
                    result.currency(),
                    result.approvedAt(),
                    result.receipt(),
                    rawPayload
            );
        } catch (RestClientResponseException exception) {
            TossErrorResponse error = parseError(exception.getResponseBodyAsString());
            throw new TossPaymentConfirmException(error.code(), error.message(), exception.getResponseBodyAsString());
        } catch (JsonProcessingException exception) {
            throw new TossPaymentConfirmException(
                    "TOSS_RESPONSE_PARSE_FAILED",
                    "토스페이먼츠 응답을 해석하지 못했습니다.",
                    exception.getOriginalMessage()
            );
        }
    }

    private String basicAuthHeader() {
        String token = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private TossErrorResponse parseError(String rawPayload) {
        try {
            TossErrorResponse error = objectMapper.readValue(rawPayload, TossErrorResponse.class);
            if (error.code() != null && error.message() != null) {
                return error;
            }
        } catch (JsonProcessingException ignored) {
        }
        return new TossErrorResponse("TOSS_CONFIRM_FAILED", "토스페이먼츠 결제 승인 요청이 실패했습니다.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TossErrorResponse(String code, String message) {
    }
}
