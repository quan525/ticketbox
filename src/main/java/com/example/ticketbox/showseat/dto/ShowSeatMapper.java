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

    @org.mapstruct.Mapping(source = "seat.room.id", target = "seat.roomid")
    ShowSeatRes toResponse(ShowSeat showSeat);

    List<ShowSeatRes> toResponses(List<ShowSeat> showSeat);

}
