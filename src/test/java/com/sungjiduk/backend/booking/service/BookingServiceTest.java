package com.sungjiduk.backend.booking.service;

import com.sungjiduk.backend.booking.dto.response.BookingLinkResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BookingService")
class BookingServiceTest {

    private Map<String, BookingLinkResponse> byType(List<BookingLinkResponse> links) {
        return links.stream().collect(java.util.stream.Collectors.toMap(BookingLinkResponse::type, l -> l));
    }

    @Nested
    @DisplayName("findBookingLinks는")
    class FindBookingLinks {

        @Test
        @DisplayName("marker가 있으면 도시·날짜로 Travelpayouts 어필리에이트 딥링크(호텔·항공)를 만든다")
        void buildsAffiliateDeeplinks() {
            // given
            BookingService service = new BookingService("748641");

            // when
            var links = byType(service.findBookingLinks("Tokyo", "2026-08-01", "2026-08-03"));

            // then — 호텔은 Hotellook 검색 + marker + 목적지 + 체크인/아웃
            BookingLinkResponse hotel = links.get("HOTEL");
            assertThat(hotel.url()).contains("hotellook.com");
            assertThat(hotel.url()).contains("marker=748641");
            assertThat(hotel.url()).contains("Tokyo");
            assertThat(hotel.url()).contains("2026-08-01").contains("2026-08-03");
            // 항공은 Aviasales + marker
            BookingLinkResponse flight = links.get("FLIGHT");
            assertThat(flight.url()).contains("aviasales.com");
            assertThat(flight.url()).contains("marker=748641");
        }

        @Test
        @DisplayName("도시에 공백/특수문자가 있어도 URL 인코딩한다")
        void encodesCity() {
            // given
            BookingService service = new BookingService("748641");

            // when
            var links = byType(service.findBookingLinks("New York", null, null));

            // then — 공백이 인코딩되고 raw 공백은 없다
            assertThat(links.get("HOTEL").url()).contains("New%20York");
            assertThat(links.get("HOTEL").url()).doesNotContain("New York");
        }

        @Test
        @DisplayName("marker가 비어 있으면 구글 트래블 링크로 폴백한다")
        void fallsBackToGoogleWhenNoMarker() {
            // given
            BookingService service = new BookingService("");

            // when
            var links = byType(service.findBookingLinks("Tokyo", null, null));

            // then
            assertThat(links.get("FLIGHT").url()).contains("google.com/travel/flights");
            assertThat(links.get("HOTEL").url()).contains("google.com/travel/hotels");
        }
    }
}
