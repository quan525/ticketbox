package com.example.ticketbox.show.service;

import com.example.ticketbox.entity.Show;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.show.dto.Request.UpdateShowReq;

public interface ShowService {
    Show createShow(CreateShowReq req);

    Show getShowById(Integer id);

//    List<Show> getShows(GetShowsReq req);

    Show updateShow(Integer showId, UpdateShowReq req);
}
