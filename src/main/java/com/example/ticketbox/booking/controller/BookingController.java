package com.example.ticketbox.booking.controller;

import com.example.ticketbox.auth.dto.request.LoginReq;
import com.example.ticketbox.booking.dto.BookingMapper;
import com.example.ticketbox.booking.dto.request.CreateBookingReq;
import com.example.ticketbox.booking.dto.response.BookingResponse;
import com.example.ticketbox.booking.service.BookingService;
import com.example.ticketbox.entity.Booking;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class BookingController {
    private final com.example.ticketbox.user.repository.UserRepository users;

    private void requireOwner(Integer userId, java.security.Principal principal) {
        var user = users.findByUsername(principal.getName()).orElseThrow();
        if (!user.getId().equals(userId) && user.getRole() == com.example.ticketbox.enums.UserRoles.USER) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot access this booking");
        }
    }

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    @PostMapping("/newbook")
    public  ResponseEntity<BookingResponse> create(
            @RequestBody @Valid CreateBookingReq request, java.security.Principal principal
//            @RequestBody Map<String, Object> claims
    ) {
        requireOwner(request.getUserId(), principal);
//        System.out.println("req2: " + claims);
//
        Booking booking = bookingService.createBooking(request);

        System.out.println("Finish");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookingMapper.toResponse(booking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getById(
            @PathVariable Long id, java.security.Principal principal
    ) {
        requireOwner(bookingService.getBooking(id).getUser().getId(), principal);
        return ResponseEntity.ok(
                bookingMapper.toResponse(
                        bookingService.getBooking(id)
                )
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getByUser(
            @PathVariable Integer userId, java.security.Principal principal
    ) {
        requireOwner(userId, principal);
        return ResponseEntity.ok(
                bookingMapper.toResponses(
                        bookingService.getBookingsByUser(userId)
                )
        );
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(
            @PathVariable Long id, java.security.Principal principal
    ) {
        requireOwner(bookingService.getBooking(id).getUser().getId(), principal);
        return ResponseEntity.ok(
                bookingMapper.toResponse(
                        bookingService.cancelBooking(id)
                )
        );
    }
}

