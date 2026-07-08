package com.sungjiduk.backend.payment.controller;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.payment.dto.request.PaymentOrderCreateRequest;
import com.sungjiduk.backend.payment.dto.response.PaymentOrderCreateResponse;
import com.sungjiduk.backend.payment.service.PaymentOrderService;
import com.sungjiduk.backend.user.entity.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-orders")
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;

    public PaymentOrderController(PaymentOrderService paymentOrderService) {
        this.paymentOrderService = paymentOrderService;
    }

    @PostMapping
    public ApiResponse<PaymentOrderCreateResponse> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody PaymentOrderCreateRequest request
    ) {
        return ApiResponse.ok(paymentOrderService.createOrder(requireUserId(currentUser), request));
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser.getId();
    }
}
