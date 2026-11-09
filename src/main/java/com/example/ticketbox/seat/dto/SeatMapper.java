package com.example.ticketbox.seat.dto;

import com.example.ticketbox.entity.Seats;
import com.example.ticketbox.seat.dto.request.CreateSeatRequest;
import com.example.ticketbox.seat.dto.request.UpdateSeatRequest;
import com.example.ticketbox.seat.dto.response.SeatResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface SeatMapper {
    @Mapping(target = "room.id", source = "roomId")
    Seats toEntity(CreateSeatRequest req, Integer roomId);

    SeatResponse toResponse(Seats seat);

    @Mapping(source = "room.id", target = "roomid")
    List<SeatResponse> toResponses(List<Seats> seats);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(
            UpdateSeatRequest req,
            @MappingTarget Seats seat
    );
}

