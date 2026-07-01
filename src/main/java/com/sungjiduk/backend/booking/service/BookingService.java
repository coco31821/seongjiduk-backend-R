package com.sungjiduk.backend.booking.service;

import com.sungjiduk.backend.booking.dto.response.BookingLinkResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingService {

    public List<BookingLinkResponse> findBookingLinks(String city, String startDate, String endDate) {
        return List.of(
                new BookingLinkResponse("FLIGHT", "항공권 검색", "https://www.google.com/travel/flights"),
                new BookingLinkResponse("HOTEL", "숙소 검색", "https://www.google.com/travel/hotels")
        );
    }
}
