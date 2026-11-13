package com.example.ticketbox.booking.dto;

import com.example.ticketbox.booking.dto.request.CreateBookingReq;
import com.example.ticketbox.booking.dto.request.UpdateBookingReq;
import com.example.ticketbox.booking.dto.response.BookingResponse;
import com.example.ticketbox.config.TicketboxTime;
import com.example.ticketbox.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingMapper {

    @Mapping(source = "userId", target = "user.id")
    @Mapping(source = "showId", target = "show.id")
    Booking toEntity(CreateBookingReq req);

    void update(UpdateBookingReq req, @MappingTarget Booking booking);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "show.id", target = "showId")
    @Mapping(source = "show.movie.title", target = "movieTitle")
    @Mapping(source = "show.room.name", target = "roomName")
    @Mapping(source = "show.startTime", target = "startTime")
    @Mapping(target = "seats", expression = "java(booking.getBookingSeats().stream().map(bs -> bs.getShowSeat().getSeat().getRowLabel() + bs.getShowSeat().getSeat().getSeatNumber()).toList())")
    BookingResponse toResponse(Booking booking);

    default java.time.Instant toReservationDeadline(java.time.LocalDateTime deadline) {
        return TicketboxTime.reservationDeadline(deadline);
    }

    List<BookingResponse> toResponses(List<Booking> bookings);
}

