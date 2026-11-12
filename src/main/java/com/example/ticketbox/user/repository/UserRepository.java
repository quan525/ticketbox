package com.example.ticketbox.user.repository;

import com.example.ticketbox.entity.Users;
import com.example.ticketbox.enums.UserRoles;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Integer> {

    Optional<Users> findByUsername(String username);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Users u where u.role = com.example.ticketbox.enums.UserRoles.ADMIN order by u.id")
    List<Users> lockAdministrators();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Users u where u.id = :id")
    Optional<Users> findByIdForUpdate(@Param("id") Integer id);
    Optional<Users> findByGoogleSubject(String googleSubject);


    @Query("""
            SELECT u FROM Users u
            WHERE u.username ILIKE CONCAT('%', COALESCE(:username, ''), '%')
              AND (
                   COALESCE(:name, '') = ''
                   OR u.firstName ILIKE CONCAT('%', :name, '%')
                   OR u.lastName  ILIKE CONCAT('%', :name, '%')
                   OR CONCAT(u.firstName, ' ', u.lastName) ILIKE CONCAT('%', :name, '%')
                   OR CONCAT(u.lastName, ' ', u.firstName) ILIKE CONCAT('%', :name, '%')
                  )
              AND (:role IS NULL OR u.role = :role)
            """)
    List<Users> findAllByFilter(
            @Param("username") String username,
            @Param("name") String name,
            @Param("role") UserRoles role,
            Pageable pageable
    );

    Users findByEmail(String email);
}
