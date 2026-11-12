package com.example.ticketbox.security;

import com.example.ticketbox.entity.Users;
import com.example.ticketbox.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        var authorities = new java.util.ArrayList<String>();
        authorities.add(user.getRole().name());
        if (user.hasPermission(com.example.ticketbox.enums.UserPermission.VIEW_OPERATIONS))
            user.getEffectivePermissions().forEach(permission -> authorities.add(permission.name()));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities.toArray(String[]::new))
                .build();
    }
}