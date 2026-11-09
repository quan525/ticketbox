package com.example.ticketbox.service;

import com.example.ticketbox.entity.Rooms;
import com.example.ticketbox.room.dto.request.CreateRoomRequest;
import com.example.ticketbox.room.dto.request.UpdateRoomRequest;
import com.example.ticketbox.room.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
class RoomServiceTest {
    @Autowired
    private RoomService roomService;

    @Test
    void createRoomTest() {
        CreateRoomRequest req = CreateRoomRequest.builder().name("Okla").seatRows(2).build();

        Rooms response = roomService.create(req);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Okla");
    }

    @Test
    void getByIdTest() {
        CreateRoomRequest req = CreateRoomRequest.builder().name("room D").seatRows(5).build();

        Rooms roomA = roomService.create(req);

        Rooms foundRoom = roomService.getById(roomA.getId());

        assertThat(foundRoom.getId()).isEqualTo(roomA.getId());
        assertThat(foundRoom.getName()).isEqualTo("room D");
    }

    @Test
    void updateRoom_success_whenNameIsUnique() {
        // given
        CreateRoomRequest createReq = CreateRoomRequest.builder().name("Room V").seatRows(5).build();

        Rooms created = roomService.create(createReq);

        UpdateRoomRequest updateReq =
                new UpdateRoomRequest("Room V Updated");

        // when
        Rooms updated =
                roomService.update(created.getId(), updateReq);

        // then
        assertThat(updated.getName()).isEqualTo("Room V Updated");
    }

    @Test
    void searchAndExistenceApisHaveServiceSupport() {
        Rooms room = roomService.create(CreateRoomRequest.builder()
                .name("Searchable room")
                .seatRows(2)
                .build());

        assertThat(roomService.checkExists(room.getName())).isTrue();
        assertThat(roomService.getManyById(List.of(room.getName())))
                .extracting(Rooms::getId)
                .containsExactly(room.getId());
    }

}
