package com.example.ticketbox.room.service;

import com.example.ticketbox.entity.Rooms;
import com.example.ticketbox.room.dto.request.CreateRoomRequest;
import com.example.ticketbox.room.dto.request.UpdateManyRoomReq;
import com.example.ticketbox.room.dto.request.UpdateRoomRequest;

import java.util.List;

public interface RoomService {

    Rooms create(CreateRoomRequest req);

    List<Rooms> createMany(List<CreateRoomRequest> reqs);

    Rooms getById(Integer id);

    List<Rooms> getManyById(List<String> names);

    Boolean checkExists(String name);

    List<Rooms> getAll();

    Rooms update(Integer id, UpdateRoomRequest req);

    List<Rooms> updateMany(List<UpdateManyRoomReq> req);

    void delete(Integer id);
}
