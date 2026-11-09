package com.example.ticketbox.showseat.service;

import com.example.ticketbox.entity.Seats;
import com.example.ticketbox.entity.Show;
import com.example.ticketbox.entity.ShowSeat;
import com.example.ticketbox.enums.SeatStatus;
import com.example.ticketbox.seat.service.SeatService;
import com.example.ticketbox.show.service.ShowService;
import com.example.ticketbox.showseat.dto.request.CreateShowSeatReq;
import com.example.ticketbox.showseat.dto.request.UpdateShowSeatReq;
import com.example.ticketbox.showseat.repository.ShowSeatRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShowSeatServiceImpl implements ShowSeatService {

    private final ShowSeatRepository showSeatRepository;
    private final ShowService showService;
    private final SeatService seatService;

    @Override
    public List<ShowSeat> createShowSeats(CreateShowSeatReq req) {

        Show show = showService.getShowById(req.getShowId());

        List<Integer> seatIds = req.getSeatIds();
        List<Seats> seats = seatService.getSeatInIds(seatIds);

        if (seats.size() != seatIds.size()) {
            throw new EntityNotFoundException("One or more seats not found");
        }

        List<ShowSeat> existing =
                showSeatRepository.findByShowIdAndSeatIdIn(show.getId(), seatIds);

        Set<Integer> existingSeatIds = existing.stream()
                .map(ss -> ss.getSeat().getId())
                .collect(Collectors.toSet());

        List<ShowSeat> toCreate = seats.stream()
                .filter(seat -> !existingSeatIds.contains(seat.getId()))
                .map(seat -> ShowSeat.builder()
                        .show(show)
                        .seat(seat)
                        .status(SeatStatus.AVAILABLE)
                        .price(req.getPrice())
                        .build())
                .toList();

        if (toCreate.isEmpty()) {
            return List.of();
        }

        return showSeatRepository.saveAll(toCreate);
    }

    @Override
    public List<ShowSeat> getShowSeatsByShowId(Integer showId) {

        var seats = showSeatRepository.findByShowId(showId);
        var now = java.time.LocalDateTime.now();
        seats.forEach(seat -> {
            if (seat.getStatus() == SeatStatus.PENDING && seat.getHeldUntil() != null && !seat.getHeldUntil().isAfter(now)) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldUntil(null);
            }
        });
        return seats;
    }

    @Override
    public ShowSeat updateShowSeat(Integer ssId, UpdateShowSeatReq req) {

        ShowSeat showSeat = showSeatRepository.findById(ssId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Show seat not found with id " + ssId
                        )
                );

        if (req.getSeatStatus() != null) {
            if (showSeat.getStatus() == SeatStatus.BOOKED &&
                    req.getSeatStatus() == SeatStatus.AVAILABLE) {
                throw new IllegalStateException("Cannot revert a booked seat");
            }
        }

        if (req.getSeatStatus() != null) {
            showSeat.setStatus(req.getSeatStatus());
        }

        if (req.getPrice() != null) {
            showSeat.setPrice(req.getPrice());
        }

        return showSeat;
    }

    @Override
    public void generateForShow(Integer showId) {
        Show show = showService.getShowById(showId);
        Integer roomId = show.getRoom().getId();

        List<Seats> seats = seatService.getSeatsByRoom(roomId);

        if (seats.isEmpty()) {
            throw new IllegalStateException("Room has no seats");
        }

        List<ShowSeat> showSeats = seats.stream()
                .map(seat -> ShowSeat.builder()
                        .show(show)
                        .seat(seat)
                        .status(SeatStatus.AVAILABLE)
                        .price(calculatePrice(seat))
                        .build())
                .toList();

        showSeatRepository.saveAll(showSeats);
    }

    private BigDecimal calculatePrice(Seats seat) {
        return switch (seat.getSeatType()) {
            case VIP -> new BigDecimal("150000");
            case STANDARD -> new BigDecimal("100000");
            default -> new BigDecimal("80000");
        };
    }
}
