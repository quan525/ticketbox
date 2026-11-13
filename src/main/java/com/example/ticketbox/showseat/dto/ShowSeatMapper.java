package com.example.ticketbox.showseat.dto;

import com.example.ticketbox.entity.ShowSeat;
import com.example.ticketbox.showseat.dto.request.CreateShowSeatReq;
import com.example.ticketbox.showseat.dto.request.UpdateShowSeatReq;
import com.example.ticketbox.showseat.dto.response.ShowSeatRes;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ShowSeatMapper {
    ShowSeat toEntity(CreateShowSeatReq req);

    void updateEntity(UpdateShowSeatReq req, @MappingTarget ShowSeat showSeat);

    @org.mapstruct.Mapping(target = "seat", expression = "java(toSeatResponse(showSeat))")
    ShowSeatRes toResponse(ShowSeat showSeat);

    default com.example.ticketbox.seat.dto.response.SeatResponse toSeatResponse(ShowSeat showSeat) {
        var seat = showSeat.getSeat();
        if (seat == null) return null;
        return com.example.ticketbox.seat.dto.response.SeatResponse.builder()
                .id(seat.getId())
                .roomid(seat.getRoom() == null ? null : seat.getRoom().getId())
                .rowLabel(seat.getRowLabel())
                .seatNumber(seat.getSeatNumber())
                .seatType(showSeat.getEffectiveSeatType())
                .build();
    }

    List<ShowSeatRes> toResponses(List<ShowSeat> showSeat);

}
