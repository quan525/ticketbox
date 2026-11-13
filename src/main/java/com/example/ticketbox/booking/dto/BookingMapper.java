package com.example.ticketbox.booking.dto;

import com.example.ticketbox.booking.dto.request.CreateBookingReq;
import com.example.ticketbox.booking.dto.request.UpdateBookingReq;
import com.example.ticketbox.booking.dto.response.BookingResponse;
import com.example.ticketbox.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(target = "seats", expression = "java(booking.getBookingSeats().stream().map(bs -> bs.getShowSeat().getSeat().getRowLabel() + bs.getShowSeat().getSeat().getSeatNumber()).toList())")
    BookingResponse toResponse(Booking booking);

    default Instant toCreatedInstant(LocalDateTime createdAt) {
        // Existing audit timestamps use the backend's local clock.
        return createdAt == null ? null : createdAt.atZone(ZoneId.systemDefault()).toInstant();
    }

    List<BookingResponse> toResponses(List<Booking> bookings);
}

