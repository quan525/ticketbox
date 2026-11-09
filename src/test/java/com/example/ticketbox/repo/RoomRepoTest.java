package com.example.ticketbox.repo;

import com.example.ticketbox.entity.Rooms;
import com.example.ticketbox.room.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
class RoomRepoTest {
    @Autowired
    private RoomRepository roomRepository;

    @Test
    void saveRoom_success() {
        Rooms room = Rooms.builder()
                .name("Room F")
                .seatRows(12)
                .capacity(20)
                .build();

        Rooms saved = roomRepository.save(room);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Room F");
    }

    @Test
    void findById_success() {

        Rooms roomBReq = Rooms.builder().name("Room N").seatRows(11).build();

        Rooms room = roomRepository.save(roomBReq);

        Rooms found = roomRepository.findById(room.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(room.getId());
        assertThat(found.getName()).isEqualTo("Room N");
    }

    @Test
    void existsByName_shouldReturnTrue() {
        roomRepository.save(
                Rooms.builder().name("Room C").capacity(20).seatRows(1).build()
        );

        boolean exists = roomRepository.existsByName("Room C");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByName_shouldReturnFalse() {
        boolean exists = roomRepository.existsByName("Not Exists");

        assertThat(exists).isFalse();
    }
}

