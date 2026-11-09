package com.example.ticketbox.room.dto;

import com.example.ticketbox.entity.Rooms;
import com.example.ticketbox.room.dto.request.CreateRoomRequest;
import com.example.ticketbox.room.dto.request.UpdateRoomRequest;
import com.example.ticketbox.room.dto.response.RoomResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface RoomMapper {

    @Mapping(source = "seatRows", target = "seatRows")
    Rooms toEntity(CreateRoomRequest req);

    List<Rooms> toEntities(List<CreateRoomRequest> req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(
            UpdateRoomRequest req,
            @MappingTarget Rooms room
    );

    RoomResponse toResponse(Rooms room);

    List<RoomResponse> toResponses(List<Rooms> rooms);
}
