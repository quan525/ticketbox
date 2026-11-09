package com.example.ticketbox.show.dto;

import com.example.ticketbox.entity.Show;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.show.dto.Request.UpdateShowReq;
import com.example.ticketbox.show.dto.Response.ShowResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ShowMapper {
    //    @Mapping(source = "movieId", target = "movie")
//    @Mapping(source = "roomId", target = "room")
    Show toEntity(CreateShowReq req);

    void update(UpdateShowReq req, @MappingTarget Show show);

    ShowResponse toResponse(Show show);

    List<ShowResponse> toResponses(List<Show> show);
}
