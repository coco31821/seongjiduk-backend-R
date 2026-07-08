package com.sungjiduk.backend.payment.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PaymentOrderCreateRequest(
        @NotNull(message = "tripPlanId는 필수입니다.")
        Long tripPlanId,

        @NotEmpty(message = "결제할 여행 상품을 선택해주세요.")
        List<Long> offerIds
) {
}
