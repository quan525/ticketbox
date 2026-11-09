package com.example.ticketbox.user.service;

import com.example.ticketbox.entity.Users;
import com.example.ticketbox.enums.UserRoles;
import com.example.ticketbox.user.dto.request.CreateUserRequest;
import com.example.ticketbox.user.dto.request.FindManyRequest;
import com.example.ticketbox.user.dto.request.Pagination;
import com.example.ticketbox.user.dto.request.UpdateUserRequest;

import java.util.List;
import java.util.Optional;


public interface UserService {
    Users createUser(CreateUserRequest req);

    Users getUserById(Integer id);

    Users getUserByEmail(String email);

    Optional<Users> getUserByName(String name);

    List<Users> getUsers(FindManyRequest request, Pagination pagination);

    Users updateUser(Integer id, UpdateUserRequest req);

    Users updateUserRole(Integer id, UserRoles role);
}
