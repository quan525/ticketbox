package com.example.ticketbox.showseat.service;

import com.example.ticketbox.entity.ShowSeat;
import com.example.ticketbox.showseat.dto.request.CreateShowSeatReq;
import com.example.ticketbox.showseat.dto.request.UpdateShowSeatReq;

import java.util.List;

public interface ShowSeatService {

    List<ShowSeat> createShowSeats(CreateShowSeatReq req);

    List<ShowSeat> getShowSeatsByShowId(Integer showId);

    ShowSeat updateShowSeat(Integer ssId, UpdateShowSeatReq req);

    void generateForShow(Integer showId);
}
