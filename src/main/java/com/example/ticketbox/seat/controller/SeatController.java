package com.example.ticketbox.seat.controller;

import com.example.ticketbox.entity.Seats;
import com.example.ticketbox.seat.dto.SeatMapper;
import com.example.ticketbox.seat.dto.request.AutoGenerateSeatReq;
import com.example.ticketbox.seat.dto.request.CreateSeatRequest;
import com.example.ticketbox.seat.dto.request.UpdateSeatRequest;
import com.example.ticketbox.seat.dto.response.SeatResponse;
import com.example.ticketbox.seat.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;
    private final SeatMapper seatMapper;

    @PostMapping("/room/{roomId}")
    public ResponseEntity<SeatResponse> createRoom(
            @PathVariable Integer roomId,
            @RequestBody @Valid CreateSeatRequest createReq
    ) {
        return ResponseEntity.ok(
                seatMapper.toResponse(
                        seatService.create(roomId, createReq)
                )
        );
    }

    @PostMapping("/rooms/{roomId}/auto-generate")
    public ResponseEntity<List<SeatResponse>> autoGenerateSeats(
            @PathVariable Integer roomId,
            @Valid @RequestBody AutoGenerateSeatReq req
    ) {
        List<Seats> seats = seatService.autoGenerateSeats(roomId, req);
        return ResponseEntity.ok(seatMapper.toResponses(seats));
    }

    @GetMapping("/room/{roomId}")
    public List<SeatResponse> getSeatsByRoom(
            @PathVariable Integer roomId
    ) {
//        return ResponseEntity.ok(
                return seatMapper.toResponses(
                    seatService.getSeatsByRoom(roomId)
                );
//        );
    }

    @PutMapping("/{seatId}")
    public ResponseEntity<SeatResponse> updateType(
            @PathVariable Integer seatId,
            @RequestBody @Valid UpdateSeatRequest req
    ) {
        return ResponseEntity.ok(
                seatMapper.toResponse(
                        seatService.updateSeat(seatId, req)
                )
        );
    }
}

