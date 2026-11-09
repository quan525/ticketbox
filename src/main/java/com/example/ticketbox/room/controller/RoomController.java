package com.example.ticketbox.room.controller;

import com.example.ticketbox.entity.Rooms;
import com.example.ticketbox.room.dto.RoomMapper;
import com.example.ticketbox.room.dto.request.CreateRoomRequest;
import com.example.ticketbox.room.dto.request.UpdateManyRoomReq;
import com.example.ticketbox.room.dto.request.UpdateRoomRequest;
import com.example.ticketbox.room.dto.response.RoomResponse;
import com.example.ticketbox.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomMapper roomMapper;

    @PostMapping
    public ResponseEntity<RoomResponse> create(
            @RequestBody @Valid CreateRoomRequest req
    ) {
        Rooms room = roomService.create(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(roomMapper.toResponse(room));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<RoomResponse>> createMany(
            @RequestBody @Valid List<CreateRoomRequest> reqs
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(roomMapper.toResponses(roomService.createMany(reqs)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                roomMapper.toResponse(roomService.getById(id))
        );
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAll() {
        return ResponseEntity.ok(
                roomMapper.toResponses(roomService.getAll())
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<RoomResponse>> getByNames(
            @RequestParam List<String> names) {
        return ResponseEntity.ok(
                roomMapper.toResponses(roomService.getManyById(names))
        );
    }

    @GetMapping("/exists")
    public ResponseEntity<Map<String, Boolean>> exists(@RequestParam String name) {
        return ResponseEntity.ok(Map.of("exists", roomService.checkExists(name)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> update(
            @PathVariable Integer id,
            @RequestBody @Valid UpdateRoomRequest req
    ) {
        return ResponseEntity.ok(
                roomMapper.toResponse(roomService.update(id, req))
        );
    }

    @PutMapping("/batch")
    public ResponseEntity<List<RoomResponse>> updateMany(
            @RequestBody @Valid List<UpdateManyRoomReq> req
    ) {
        return ResponseEntity.ok(
                roomMapper.toResponses(roomService.updateMany(req))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

