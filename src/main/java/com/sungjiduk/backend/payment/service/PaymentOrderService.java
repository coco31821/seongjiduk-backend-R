package com.sungjiduk.backend.payment.service;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.payment.dto.request.PaymentConfirmRequest;
import com.sungjiduk.backend.payment.dto.request.PaymentOrderCreateRequest;
import com.sungjiduk.backend.payment.dto.response.PaymentConfirmResponse;
import com.sungjiduk.backend.payment.dto.response.PaymentOrderCreateResponse;
import com.sungjiduk.backend.payment.dto.response.SupplierBookingResponse;
import com.sungjiduk.backend.payment.entity.PaymentOrder;
import com.sungjiduk.backend.payment.entity.PaymentOrderItem;
import com.sungjiduk.backend.payment.constants.PaymentOrderStatus;
import com.sungjiduk.backend.payment.entity.PaymentTransaction;
import com.sungjiduk.backend.payment.entity.SupplierBooking;
import com.sungjiduk.backend.payment.entity.TravelOffer;
import com.sungjiduk.backend.payment.infra.TossPaymentConfirmException;
import com.sungjiduk.backend.payment.infra.TossPaymentConfirmResult;
import com.sungjiduk.backend.payment.infra.TossPaymentsClient;
import com.sungjiduk.backend.payment.repository.PaymentOrderRepository;
import com.sungjiduk.backend.payment.repository.TravelOfferRepository;
import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.trip.entity.TripStatus;
import com.sungjiduk.backend.trip.exception.TripNotFoundException;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import com.sungjiduk.backend.user.entity.User;
import com.sungjiduk.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentOrderService {

    private final UserRepository userRepository;
    private final TripPlanRepository tripPlanRepository;
    private final TravelOfferRepository travelOfferRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final String tossClientKey;

    public PaymentOrderService(
            UserRepository userRepository,
            TripPlanRepository tripPlanRepository,
            TravelOfferRepository travelOfferRepository,
            PaymentOrderRepository paymentOrderRepository,
            TossPaymentsClient tossPaymentsClient,
            @Value("${seongjiduk.tosspayments.client-key:}") String tossClientKey
    ) {
        this.userRepository = userRepository;
        this.tripPlanRepository = tripPlanRepository;
        this.travelOfferRepository = travelOfferRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.tossPaymentsClient = tossPaymentsClient;
        this.tossClientKey = tossClientKey;
    }

    @Transactional
    public PaymentOrderCreateResponse createOrder(Long userId, PaymentOrderCreateRequest request) {
        User user = userRepository.findByIdOrThrow(userId);
        TripPlan tripPlan = tripPlanRepository.findById(request.tripPlanId())
                .orElseThrow(() -> new TripNotFoundException(request.tripPlanId()));
        requirePaymentReadyTrip(tripPlan, userId);

        List<Long> offerIds = new LinkedHashSet<>(request.offerIds()).stream().toList();
        if (offerIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "결제할 여행 상품을 선택해주세요.");
        }

        List<TravelOffer> offers = travelOfferRepository.findAllById(offerIds);
        validateOffers(tripPlan.getId(), offerIds, offers);

        long totalAmount = offers.stream().mapToLong(TravelOffer::getPrice).sum();
        String orderName = orderNameOf(tripPlan, offers);

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .user(user)
                .tripPlan(tripPlan)
                .orderId(generateOrderId())
                .orderName(orderName)
                .totalAmount(totalAmount)
                .currency("KRW")
                .status(PaymentOrderStatus.READY)
                .build();
        offers.stream()
                .map(PaymentOrderItem::from)
                .forEach(paymentOrder::addItem);

        PaymentOrder saved = paymentOrderRepository.save(paymentOrder);
        return new PaymentOrderCreateResponse(
                saved.getOrderId(),
                saved.getOrderName(),
                saved.getTotalAmount(),
                saved.getCurrency(),
                tossClientKey
        );
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public PaymentConfirmResponse confirm(Long userId, PaymentConfirmRequest request) {
        PaymentOrder paymentOrder = paymentOrderRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
        if (!paymentOrder.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (paymentOrder.getStatus() != PaymentOrderStatus.READY) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_INVALID_STATUS);
        }
        if (!paymentOrder.getTotalAmount().equals(request.amount())) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        TossPaymentConfirmResult result;
        try {
            result = tossPaymentsClient.confirm(request.paymentKey(), request.orderId(), paymentOrder.getTotalAmount());
        } catch (TossPaymentConfirmException exception) {
            paymentOrder.addTransaction(PaymentTransaction.failed(
                    request.paymentKey(),
                    paymentOrder.getTotalAmount(),
                    paymentOrder.getCurrency(),
                    exception.getCode(),
                    exception.getMessage(),
                    exception.getRawPayload()
            ));
            paymentOrder.markFailed();
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED, exception.getMessage());
        }

        if (!"DONE".equals(result.status()) || !paymentOrder.getTotalAmount().equals(result.totalAmount())) {
            paymentOrder.addTransaction(PaymentTransaction.failed(
                    result.paymentKey(),
                    paymentOrder.getTotalAmount(),
                    paymentOrder.getCurrency(),
                    "INVALID_TOSS_CONFIRM_RESULT",
                    "토스페이먼츠 승인 결과가 주문 정보와 일치하지 않습니다.",
                    result.rawPayload()
            ));
            paymentOrder.markFailed();
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED, "토스페이먼츠 승인 결과가 주문 정보와 일치하지 않습니다.");
        }

        paymentOrder.addTransaction(PaymentTransaction.succeeded(
                result.paymentKey(),
                result.method(),
                result.totalAmount(),
                result.currency(),
                result.receiptUrl(),
                result.approvedAt() == null ? LocalDateTime.now() : result.approvedAt().toLocalDateTime(),
                result.rawPayload()
        ));
        paymentOrder.markPaymentDone();
        confirmMockSupplierBookings(paymentOrder);
        paymentOrder.markBooked();

        return toConfirmResponse(paymentOrder, result);
    }

    private void validateOffers(Long tripPlanId, List<Long> requestedOfferIds, List<TravelOffer> offers) {
        if (offers.size() != requestedOfferIds.size()) {
            throw new BusinessException(ErrorCode.TRAVEL_OFFER_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        boolean invalidOffer = offers.stream().anyMatch(offer ->
                !offer.getTripPlan().getId().equals(tripPlanId) || offer.isExpired(now)
        );
        if (invalidOffer) {
            throw new BusinessException(ErrorCode.TRAVEL_OFFER_NOT_FOUND);
        }
        boolean mixedCurrency = offers.stream().map(TravelOffer::getCurrency).distinct().count() > 1;
        if (mixedCurrency) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "서로 다른 통화의 상품은 함께 결제할 수 없습니다.");
        }
    }

    private void requirePaymentReadyTrip(TripPlan tripPlan, Long userId) {
        if (tripPlan.isUnowned() || !tripPlan.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (tripPlan.getStatus() != TripStatus.SAVED) {
            throw new BusinessException(ErrorCode.TRIP_NOT_READY_FOR_PAYMENT);
        }
    }

    private String orderNameOf(TripPlan tripPlan, List<TravelOffer> offers) {
        if (offers.size() == 1) {
            return offers.get(0).getName();
        }
        String title = tripPlan.getTitle() == null ? "성지순례 일정" : tripPlan.getTitle();
        return title + " 여행 상품 " + offers.size() + "건";
    }

    private String generateOrderId() {
        return "ORDER-" + UUID.randomUUID().toString().replace("-", "");
    }

    private void confirmMockSupplierBookings(PaymentOrder paymentOrder) {
        paymentOrder.markBookingRequested();
        for (PaymentOrderItem item : paymentOrder.getItems()) {
            TravelOffer offer = item.getTravelOffer();
            String reservationId = "MOCK-" + offer.getOfferType().name() + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            paymentOrder.addSupplierBooking(SupplierBooking.mockConfirmed(
                    offer.getProvider(),
                    reservationId,
                    item.getAmount(),
                    item.getCurrency(),
                    "{\"reservationId\":\"" + reservationId + "\"}"
            ));
        }
    }

    private PaymentConfirmResponse toConfirmResponse(PaymentOrder paymentOrder, TossPaymentConfirmResult result) {
        List<SupplierBookingResponse> bookings = paymentOrder.getSupplierBookings().stream()
                .map(SupplierBookingResponse::from)
                .toList();
        return new PaymentConfirmResponse(
                paymentOrder.getOrderId(),
                paymentOrder.getStatus(),
                result.paymentKey(),
                result.method(),
                paymentOrder.getTotalAmount(),
                paymentOrder.getCurrency(),
                result.receiptUrl(),
                bookings
        );
    }
}
