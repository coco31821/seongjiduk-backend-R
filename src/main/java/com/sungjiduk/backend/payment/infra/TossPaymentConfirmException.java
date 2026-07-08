package com.sungjiduk.backend.payment.infra;

public class TossPaymentConfirmException extends RuntimeException {

    private final String code;
    private final String rawPayload;

    public TossPaymentConfirmException(String code, String message, String rawPayload) {
        super(message);
        this.code = code;
        this.rawPayload = rawPayload;
    }

    public String getCode() {
        return code;
    }

    public String getRawPayload() {
        return rawPayload;
    }
}
