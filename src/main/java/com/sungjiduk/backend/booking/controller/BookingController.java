package com.sungjiduk.backend.booking.controller;

import com.sungjiduk.backend.booking.dto.response.BookingLinkResponse;
import com.sungjiduk.backend.booking.service.BookingService;
import com.sungjiduk.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/booking-links")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ApiResponse<List<BookingLinkResponse>> bookingLinks(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return ApiResponse.ok(bookingService.findBookingLinks(city, startDate, endDate));
    }
}
