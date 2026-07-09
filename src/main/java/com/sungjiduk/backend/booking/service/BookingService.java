package com.sungjiduk.backend.booking.service;

import com.sungjiduk.backend.booking.dto.response.BookingLinkResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 항공권·숙소 외부 링크 (F-6). Travelpayouts marker가 있으면 어필리에이트 딥링크
 * (Hotellook 호텔·Aviasales 항공, 예약 시 수수료), 없으면 구글 트래블로 폴백.
 * 우리는 예약을 처리하지 않고 파트너 사이트로 보낸다(어필리에이트 모델 — VISION ⑨).
 */
@Service
public class BookingService {

    private final String marker;

    public BookingService(@Value("${seongjiduk.travelpayouts.marker:}") String marker) {
        this.marker = marker;
    }

    public List<BookingLinkResponse> findBookingLinks(String city, String startDate, String endDate) {
        if (marker == null || marker.isBlank()) {
            return List.of(
                    new BookingLinkResponse("FLIGHT", "항공권 검색", "https://www.google.com/travel/flights"),
                    new BookingLinkResponse("HOTEL", "숙소 검색", "https://www.google.com/travel/hotels")
            );
        }
        String dest = city == null ? "" : city.trim();
        String enc = URLEncoder.encode(dest, StandardCharsets.UTF_8).replace("+", "%20");
        String prefix = dest.isBlank() ? "" : dest + " ";

        // Hotellook: 목적지·체크인/아웃으로 실제 호텔 검색 (marker로 어필리에이트 트래킹)
        StringBuilder hotel = new StringBuilder("https://search.hotellook.com/?marker=").append(marker);
        if (!enc.isBlank()) {
            hotel.append("&destination=").append(enc);
        }
        if (startDate != null && !startDate.isBlank()) {
            hotel.append("&checkIn=").append(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            hotel.append("&checkOut=").append(endDate);
        }

        // Aviasales: 목적지 항공권 (marker 트래킹). 출발지 IATA 미보유라 목적지 힌트로 검색을 연다.
        StringBuilder flight = new StringBuilder("https://www.aviasales.com/?marker=").append(marker);
        if (!enc.isBlank()) {
            flight.append("&destination=").append(enc);
        }

        return List.of(
                new BookingLinkResponse("FLIGHT", prefix + "항공권 검색", flight.toString()),
                new BookingLinkResponse("HOTEL", prefix + "숙소 검색", hotel.toString())
        );
    }
}
