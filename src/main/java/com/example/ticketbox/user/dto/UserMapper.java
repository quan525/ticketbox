package com.example.ticketbox.user.dto;

import com.example.ticketbox.entity.Users;
import com.example.ticketbox.user.dto.request.CreateUserRequest;
import com.example.ticketbox.user.dto.request.UpdateUserRequest;
import com.example.ticketbox.user.dto.response.UserDetailResponse;
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
public interface UserMapper {

    Users toEntity(CreateUserRequest req);


    void updateEntity(
            UpdateUserRequest request,
            @MappingTarget Users user
    );

    UserDetailResponse toResponse(Users user);

    List<UserDetailResponse> toDetailResponses(List<Users> users);
}
