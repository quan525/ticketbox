package com.example.ticketbox.room.service;

import com.example.ticketbox.entity.Rooms;
import com.example.ticketbox.room.dto.RoomMapper;
import com.example.ticketbox.room.dto.request.CreateRoomRequest;
import com.example.ticketbox.room.dto.request.UpdateManyRoomReq;
import com.example.ticketbox.room.dto.request.UpdateRoomRequest;
import com.example.ticketbox.room.repository.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    @Override
    @Transactional
    public Rooms create(CreateRoomRequest req) {
        if (roomRepository.existsByName(req.getName())) {
            System.out.println(req.getName());
            throw new IllegalArgumentException("Room name already exists");
        }

        Rooms room = roomMapper.toEntity(req);
        return roomRepository.save(room);
    }

    @Override
    @Transactional
    public List<Rooms> createMany(List<CreateRoomRequest> requests) {

        List<String> names = requests.stream()
                .map(CreateRoomRequest::getName)
                .toList();

        List<String> existingNames = roomRepository.findByNameIn(names)
                .stream()
                .map(Rooms::getName)
                .toList();

        if (!existingNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "Rooms already exist: " + existingNames
            );
        }

        List<Rooms> rooms = roomMapper.toEntities(requests);
        return roomRepository.saveAll(rooms);
    }

    @Override
    public Rooms getById(Integer id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));
    }

    @Override
    public List<Rooms> getManyById(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return roomRepository.findByNameIn(names.stream().distinct().toList());
    }

    @Override
    public Boolean checkExists(String name) {
        return roomRepository.existsByName(name);
    }

    @Override
    public List<Rooms> getAll() {
        return roomRepository.findAll();
    }

    @Override
    @Transactional
    public Rooms update(Integer id, UpdateRoomRequest req) {

        Rooms room = getById(id);

        if (req.getName() != null &&
                roomRepository.existsByNameAndIdNot(req.getName(), id)) {
            throw new IllegalArgumentException("Room name already exists");
        }

        roomMapper.updateEntity(req, room);
        return room;
    }

    @Override
    @Transactional
    public List<Rooms> updateMany(List<UpdateManyRoomReq> req) {

        if (req == null || req.isEmpty()) {
            throw new IllegalArgumentException("Room list must not be empty");
        }

        Set<Integer> ids = req.stream()
                .map(UpdateManyRoomReq::getId)
                .collect(Collectors.toSet());

        if (ids.size() != req.size()) {
            throw new IllegalArgumentException("Duplicate room id in request");
        }

        List<Rooms> rooms = roomRepository.findByIdIn(new ArrayList<>(ids));
        if (rooms.size() != ids.size()) {
            throw new EntityNotFoundException("One or more rooms not found");
        }

        Map<Integer, Rooms> roomMap = rooms.stream()
                .collect(Collectors.toMap(Rooms::getId, r -> r));

        Set<String> requestedNames = req.stream()
                .map(UpdateManyRoomReq::getName)
                .collect(Collectors.toSet());
        if (requestedNames.size() != req.size()) {
            throw new IllegalArgumentException("Duplicate room name in request");
        }

        for (UpdateManyRoomReq item : req) {
            Rooms room = roomMap.get(item.getId());
            if (roomRepository.existsByNameAndIdNot(item.getName(), item.getId())) {
                throw new IllegalArgumentException("Room name already exists: " + item.getName());
            }
            roomMapper.updateEntity(
                    new UpdateRoomRequest(item.getName()),
                    room
            );
        }

        return rooms;
    }

    @Override
    public void delete(Integer id) {
        if (!roomRepository.existsById(id)) {
            throw new EntityNotFoundException("Room not found");
        }
        roomRepository.deleteById(id);
    }
}
