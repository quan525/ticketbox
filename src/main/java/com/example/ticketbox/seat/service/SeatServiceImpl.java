package com.example.ticketbox.seat.service;

import com.example.ticketbox.entity.Rooms;
import com.example.ticketbox.entity.Seats;
import com.example.ticketbox.room.service.RoomService;
import com.example.ticketbox.seat.dto.SeatMapper;
import com.example.ticketbox.seat.dto.request.AutoGenerateSeatReq;
import com.example.ticketbox.seat.dto.request.CreateSeatRequest;
import com.example.ticketbox.seat.dto.request.UpdateSeatRequest;
import com.example.ticketbox.seat.repository.SeatRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final RoomService roomService;
    private final SeatMapper seatMapper;
    private final com.example.ticketbox.showseat.repository.ShowSeatRepository showSeats;

    @Override
    @Transactional
    public Seats create(Integer roomId, CreateSeatRequest req) {

//        Rooms room = roomRepository.findById(roomId)
//                .orElseThrow(() -> new EntityNotFoundException("Room not found"));
        Rooms room = roomService.getById(roomId);

        if (seatRepository.existsByRoomIdAndRowLabelAndSeatNumber(
                roomId, req.getRowLabel(), req.getSeatNumber())) {
            throw new IllegalArgumentException("Seat already exists in room");
        }

        Seats seat = seatMapper.toEntity(req, room.getId());
        return seatRepository.save(seat);
    }

    @Override
    @Transactional
    public List<Seats> autoGenerateSeats(
            Integer roomId,
            AutoGenerateSeatReq req
    ) {
//        Rooms room = roomRepository.findById(roomId)
//                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        Rooms room = roomService.getById(roomId);

        List<Seats> toCreate = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < req.getRows(); rowIndex++) {

            String rowLabel = String.valueOf((char) ('A' + rowIndex));

            for (int seatNum = req.getStartSeatNumber();
                 seatNum < req.getStartSeatNumber() + req.getSeatsPerRow();
                 seatNum++) {

                boolean exists =
                        seatRepository.existsByRoomIdAndRowLabelAndSeatNumber(
                                roomId, rowLabel, seatNum
                        );

                if (exists) {
                    continue; // skip existing seats
                }

                Seats seat = Seats.builder()
                        .room(room)
                        .rowLabel(rowLabel)
                        .seatNumber(seatNum)
                        .seatType(req.getSeatType())
                        .build();

                toCreate.add(seat);
            }
        }

        if (toCreate.isEmpty()) {
            return List.of();
        }

        return seatRepository.saveAll(toCreate);
    }

    @Override
    public List<Seats> getSeatsByRoom(Integer roomId) {
        List<Seats> seats = seatRepository.findByRoomId(roomId);
        System.out.println("seats" + seats);
        return seats;
    }

    @Override
    @Transactional
    public Seats updateSeat(Integer seatId, UpdateSeatRequest req) {

        Seats seat = seatRepository.lockByIds(List.of(seatId)).stream()
                .findFirst().orElseThrow(() -> new EntityNotFoundException("Seat not found"));

        if (req.getSeatType() != null && req.getSeatType() != seat.getSeatType()) {
            for (var showSeat : showSeats.lockByPhysicalSeat(seatId)) {
                if (showSeat.getSeatTypeAtScheduling() == null) {
                    showSeat.setSeatTypeAtScheduling(seat.getSeatType());
                }
            }
            seatMapper.updateEntity(req, seat);
        }

        return seat;
    }

    @Override
    @Transactional
    public List<Seats> getSeatsByRoomForScheduling(Integer roomId) {
        return seatRepository.lockByRoom(roomId);
    }

    @Override
    @Transactional
    public List<Seats> getSeatInIds(List<Integer> ids) {
        return ids.isEmpty() ? List.of() : seatRepository.lockByIds(ids);
    }
}
