package com.example.ticketbox.user.service;

import com.example.ticketbox.entity.Users;
import com.example.ticketbox.enums.UserRoles;
import com.example.ticketbox.user.dto.UserMapper;
import com.example.ticketbox.user.dto.request.CreateUserRequest;
import com.example.ticketbox.user.dto.request.FindManyRequest;
import com.example.ticketbox.user.dto.request.Pagination;
import com.example.ticketbox.user.dto.request.UpdateUserRequest;
import com.example.ticketbox.user.repository.UserRepository;
import com.example.ticketbox.utils.PageableConvert;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Users createUser(CreateUserRequest req) {
        Users user = userMapper.toEntity(req);
        user.setRole(UserRoles.USER);
        return userRepository.save(user);
    }

    @Override
    public Users getUserById(Integer id) {

        return userRepository.findById(id)
            .orElseThrow(() ->
                new EntityNotFoundException("User not found"));
    }

    @Override
    public Users getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<Users> getUserByName(String name) { return userRepository.findByUsername(name); }

    @Override
    public List<Users> getUsers(
            FindManyRequest request,
            Pagination pagination
    ) {
        Pageable pageable = PageableConvert.toPageable(pagination);

        return userRepository.findAllByFilter(
                request.getUsername(),
                request.getName(),
                request.getRole(),
                pageable
        );
    }

    @Override
    public Users updateUser(Integer id, UpdateUserRequest req) {
        Users user = getUserById(id);
        userMapper.updateEntity(req, user);
        return userRepository.save(user);
    }

    @Override
    public Users updateUserRole(Integer id, UserRoles role) {
        Users user = getUserById(id);
        user.setRole(role);
        return userRepository.save(user);
    }
}
