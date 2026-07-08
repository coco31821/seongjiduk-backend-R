package com.sungjiduk.backend.payment.service;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.payment.constants.PaymentOrderStatus;
import com.sungjiduk.backend.payment.dto.request.PaymentConfirmRequest;
import com.sungjiduk.backend.payment.dto.request.PaymentOrderCreateRequest;
import com.sungjiduk.backend.payment.dto.response.PaymentConfirmResponse;
import com.sungjiduk.backend.payment.dto.response.PaymentOrderCreateResponse;
import com.sungjiduk.backend.payment.dto.response.TravelOffersResponse;
import com.sungjiduk.backend.payment.entity.PaymentOrder;
import com.sungjiduk.backend.payment.infra.TossPaymentConfirmException;
import com.sungjiduk.backend.payment.infra.TossPaymentConfirmResult;
import com.sungjiduk.backend.payment.infra.TossPaymentsClient;
import com.sungjiduk.backend.payment.repository.PaymentOrderRepository;
import com.sungjiduk.backend.payment.repository.PaymentTransactionRepository;
import com.sungjiduk.backend.payment.repository.SupplierBookingRepository;
import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.trip.entity.TripStatus;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import com.sungjiduk.backend.user.entity.User;
import com.sungjiduk.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;

import java.time.OffsetDateTime;
import java.util.List;

@SpringBootTest
@Transactional
@DisplayName("PaymentOrderService 테스트")
class PaymentOrderServiceTests {

    @MockitoBean
    private TossPaymentsClient tossPaymentsClient;

    @Nested
    @DisplayName("createOrder 테스트")
    class CreateOrder {
        @Autowired
        private PaymentOrderService paymentOrderService;

        @Autowired
        private TravelOfferService travelOfferService;

        @Autowired
        private PaymentOrderRepository paymentOrderRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ContentRepository contentRepository;

        @Autowired
        private TripPlanRepository tripPlanRepository;

        @Nested
        @DisplayName("주문")
        class offers{
            @Test
            @DisplayName("READY 상태의 결제 주문을 생성")
            void 성공_createPaymentOrder() {
                // given
                User user = userRepository.save(User.builder()
                    .email("order-user@example.com")
                    .passwordHash("password1234")
                    .nickname("paymenttest2")
                    .build());

                Content content = contentRepository.save(Content.create(
                    "봇치 더 록!",
                    "ANIME",
                    "JP",
                    "우리 분홍머리가 귀여워요"
                ));

                TripPlan tripPlan = tripPlanRepository.save(TripPlan.builder()
                    .user(user)
                    .content(content)
                    .title("시모키타자와 성지순례")
                    .durationDays(2)
                    .status(TripStatus.SAVED)
                    .build());

                TravelOffersResponse offers =
                    travelOfferService.findOrCreateMockOffers(user.getId(), tripPlan.getId());

                Long flightOfferId = offers.flights().get(0).offerId();
                Long hotelOfferId = offers.hotels().get(0).offerId();

                // when
                PaymentOrderCreateResponse response = paymentOrderService.createOrder(
                    user.getId(), new PaymentOrderCreateRequest(tripPlan.getId(), List.of(flightOfferId, hotelOfferId)));

                // then
                PaymentOrder order = paymentOrderRepository.findByOrderId(response.orderId()).orElseThrow();

                assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.READY);
                assertThat(order.getItems()).hasSize(2);
                assertThat(order.getTotalAmount()).isEqualTo(offers.flights().get(0).price() + offers.hotels().get(0).price());

                assertThat(response.orderId()).isEqualTo(order.getOrderId());
                assertThat(response.orderName()).isEqualTo(order.getOrderName());
                assertThat(response.amount()).isEqualTo(offers.flights().get(0).price() + offers.hotels().get(0).price());
                assertThat(response.currency()).isEqualTo("KRW");
            }

            @Test
            @DisplayName("존재하지 않는 offerId가 포함되면 주문 생성에 실패한다")
            void 실패_notExistedOfferId() {
                // given
                Long notExistsOfferId = -1L;

                User user = userRepository.save(User.builder()
                    .email("not-order-user@example.com")
                    .passwordHash("password1234")
                    .nickname("paymenttest2")
                    .build());

                Content content = contentRepository.save(Content.create(
                    "봇치 더 록!",
                    "ANIME",
                    "JP",
                    "락밴드에요"
                ));

                TripPlan tripPlan = tripPlanRepository.save(TripPlan.builder()
                    .user(user)
                    .content(content)
                    .title("시모키타자와 성지순례")
                    .durationDays(2)
                    .status(TripStatus.SAVED)
                    .build());

                // when
                assertThatThrownBy(() ->
                        paymentOrderService.createOrder(user.getId(), new PaymentOrderCreateRequest(tripPlan.getId(), List.of(notExistsOfferId)))
                    // then
                )
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TRAVEL_OFFER_NOT_FOUND);

                assertThat(paymentOrderRepository.findAll()).isEmpty();
            }
        }
    }


}
