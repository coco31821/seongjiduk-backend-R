package com.sungjiduk.backend.payment.service;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.payment.constants.TravelOfferProvider;
import com.sungjiduk.backend.payment.constants.TravelOfferType;
import com.sungjiduk.backend.payment.dto.response.TravelOffersResponse;
import com.sungjiduk.backend.payment.repository.TravelOfferRepository;
import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.trip.entity.TripStatus;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import com.sungjiduk.backend.user.entity.User;
import com.sungjiduk.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;



@SpringBootTest
@Transactional
@DisplayName("TravelOfferServiceTest")
class TravelOfferServiceTests {

    @Autowired
    private TravelOfferService travelOfferService;

    @Autowired
    private TravelOfferRepository travelOfferRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private TripPlanRepository tripPlanRepository;

    private User user;
    private TripPlan savedTrip;

    @BeforeEach
    void setUp() {
        Content content = contentRepository.save(Content.create("봇치 더 록!", "ANIME", "JP", "설명"));
        user = userRepository.save(User.builder()
                .email("payment@example.com")
                .passwordHash("password1234")
                .nickname("botch")
                .build());
        savedTrip = tripPlanRepository.save(TripPlan.builder()
                .user(user)
                .content(content)
                .title("시모키타자와 성지순례")
                .durationDays(2)
                .status(TripStatus.SAVED)
                .build());
    }

    @Test
    @DisplayName("SAVED 상태의 일정이면 mock 항공권과 숙소 offer를 생성한다.")
    void 성공_createMockOfferForSavedTrip(){
        // given
        User user = userRepository.save(User.builder()
            .email("payment-user@example.com")
            .passwordHash("password1234")
            .nickname("paymenttest1")
            .build());

        Content content = contentRepository.save(Content.create(
            "봇치 더 록!",
            "ANIME",
            "JP",
            "설명"
        ));

        TripPlan tripPlan = tripPlanRepository.save(TripPlan.builder()
            .user(user)
            .content(content)
            .title("시모키타자와 성지순례")
            .durationDays(2)
            .status(TripStatus.SAVED)
            .build());


        // when
        TravelOffersResponse response = travelOfferService.findOrCreateMockOffers(user.getId(), tripPlan.getId());

        // then
        assertThat(response.flights()).hasSize(1);
        assertThat(response.hotels()).hasSize(1);

        assertThat(response.flights().get(0).provider()).isEqualTo(TravelOfferProvider.MOCK_FLIGHT);
        assertThat(response.flights().get(0).offerType()).isEqualTo(TravelOfferType.FLIGHT);

        assertThat(response.hotels().get(0).provider()).isEqualTo(TravelOfferProvider.MOCK_HOTEL);
        assertThat(response.hotels().get(0).offerType()).isEqualTo(TravelOfferType.HOTEL);

        assertThat(travelOfferRepository
            .findAll()) //  .findByTripPlanIdAndDeletedAtIsNullOrderByIdAsc(tripPlan.getId())) 해당 일정에 offer가 없으면 mock 항공권/숙소를 자동 생성하고, 있으면 기존 offer를 반환함.
            .hasSize(2);

    }

    @Test
    @DisplayName("이미 offer가 있으면 중복 생성하지 않고 기존 offer를 반환한다.")
    void 실패_기존offer반환(){
        // given
        User user = userRepository.save(User.builder()
            .email("payment-user@example.com")
            .passwordHash("password1234")
            .nickname("paymenttest1")
            .build());

        Content content = contentRepository.save(Content.create(
            "봇치 더 록!",
            "ANIME",
            "JP",
            "여고생 록밴드 애니메이션"
        ));

        TripPlan tripPlan = tripPlanRepository.save(TripPlan.builder()
            .user(user)
            .content(content)
            .title("시모키타자와 성지순례")
            .durationDays(2)
            .status(TripStatus.SAVED)
            .build());

        // when
        TravelOffersResponse first = travelOfferService.findOrCreateMockOffers(user.getId(), tripPlan.getId());

        TravelOffersResponse second = travelOfferService.findOrCreateMockOffers(user.getId(), tripPlan.getId());

        // then
        assertThat(travelOfferRepository.findAll()).hasSize(2);
        assertThat(second.flights().get(0).offerId()).isEqualTo(first.flights().get(0).offerId());
        assertThat(second.hotels().get(0).offerId()).isEqualTo(first.hotels().get(0).offerId());
    }

    @Test
    @DisplayName("DRAFT상태의 일정이면 offer 조회에 실패")
    void 실패_Draft일정(){
        // given
        User user = userRepository.save(User.builder()
            .email("draft-user@example.com")
            .passwordHash("password1234")
            .nickname("paymenttest1")
            .build());

        Content content = contentRepository.save(Content.create(
            "봇치 더 록!",
            "ANIME",
            "JP",
            "여고생 록밴드 애니메이션"
        ));

        TripPlan tripPlan = tripPlanRepository.save(TripPlan.builder()
            .user(user)
            .content(content)
            .title("시모키타자와 성지순례")
            .durationDays(2)
            .status(TripStatus.DRAFT)
            .build());

        // when
        assertThatThrownBy(() -> travelOfferService.findOrCreateMockOffers(user.getId(), tripPlan.getId()))
        // then
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.TRIP_NOT_READY_FOR_PAYMENT);

        assertThat(travelOfferRepository.findAll()).isEmpty();  // 일정이 실패하므로, offer 생성이 되지 않는지 확인하기!
    }

}
