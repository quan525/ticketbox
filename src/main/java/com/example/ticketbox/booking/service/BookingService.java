package com.example.ticketbox.booking.service;

import com.example.ticketbox.booking.dto.request.CreateBookingReq;
import com.example.ticketbox.entity.Booking;

import java.util.List;

public interface BookingService {

    Booking createBooking(CreateBookingReq req);

    Booking getBooking(Long id);

    List<Booking> getBookings();

    List<Booking> getBookingsByUser(Integer userId);

    Booking cancelBooking(Long id);
}
