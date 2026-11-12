package com.example.ticketbox.room.repository;

import com.example.ticketbox.entity.Rooms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Rooms, Integer> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Rooms r where r.id = :id")
    java.util.Optional<Rooms> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Integer id);

    List<Rooms> findByIdIn(List<Integer> ids);

    List<Rooms> findByNameIn(List<String> names);

}
