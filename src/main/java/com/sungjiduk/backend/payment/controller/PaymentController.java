package com.sungjiduk.backend.payment.controller;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.payment.dto.request.PaymentConfirmRequest;
import com.sungjiduk.backend.payment.dto.response.PaymentConfirmResponse;
import com.sungjiduk.backend.payment.service.PaymentOrderService;
import com.sungjiduk.backend.user.entity.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentOrderService paymentOrderService;

    public PaymentController(PaymentOrderService paymentOrderService) {
        this.paymentOrderService = paymentOrderService;
    }

    @PostMapping("/confirm")
    public ApiResponse<PaymentConfirmResponse> confirm(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        return ApiResponse.ok(paymentOrderService.confirm(requireUserId(currentUser), request));
    }

    private Long requireUserId(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser.getId();
    }
}
