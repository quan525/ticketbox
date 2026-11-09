package com.example.ticketbox.user.controller;

import com.example.ticketbox.entity.Users;
import com.example.ticketbox.enums.UserRoles;
import com.example.ticketbox.user.dto.UserMapper;
import com.example.ticketbox.user.dto.request.CreateUserRequest;
import com.example.ticketbox.user.dto.request.FindManyRequest;
import com.example.ticketbox.user.dto.request.Pagination;
import com.example.ticketbox.user.dto.request.UpdateUserRequest;
import com.example.ticketbox.user.dto.response.UserDetailResponse;
import com.example.ticketbox.user.repository.UserRepository;
import com.example.ticketbox.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping
    public UserDetailResponse createUser(
            @RequestBody @Valid CreateUserRequest request
    ) {
        return userMapper.toResponse(
                userService.createUser(request)
        );
    }

    @GetMapping("/{id}")
    public UserDetailResponse getUser(@PathVariable Integer id) {
        return userMapper.toResponse(
                userService.getUserById(id)
        );
    }

    @GetMapping
    public List<UserDetailResponse> getUsers(
            @ModelAttribute FindManyRequest request,
            @ModelAttribute Pagination pagination
    ) {
        return userMapper.toDetailResponses(
                userService.getUsers(request, pagination)
        );
    }

    @PutMapping("/{id}")
    public UserDetailResponse updateUser(
            @PathVariable Integer id,
            @RequestBody @Valid UpdateUserRequest request
    ) {
        return userMapper.toResponse(
                userService.updateUser(id, request)
        );
    }

    @PatchMapping("/{id}/role")
    public UserDetailResponse updateUserRole(
            @PathVariable Integer id,
            @RequestParam UserRoles role
    ) {
        return userMapper.toResponse(
                userService.updateUserRole(id, role)
        );
    }
}
