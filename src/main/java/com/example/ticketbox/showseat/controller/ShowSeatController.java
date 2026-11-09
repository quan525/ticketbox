package com.example.ticketbox.showseat.controller;

import com.example.ticketbox.entity.ShowSeat;
import com.example.ticketbox.showseat.dto.ShowSeatMapper;
import com.example.ticketbox.showseat.dto.request.CreateShowSeatReq;
import com.example.ticketbox.showseat.dto.request.UpdateShowSeatReq;
import com.example.ticketbox.showseat.dto.response.ShowSeatRes;
import com.example.ticketbox.showseat.service.ShowSeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/show-seats")
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class ShowSeatController {

    private final ShowSeatService showSeatService;
    private final ShowSeatMapper showSeatMapper;

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public List<ShowSeatRes> createShowSeats(
            @RequestBody @Valid CreateShowSeatReq req
    ) {
        List<ShowSeat> seats = showSeatService.createShowSeats(req);
        return showSeatMapper.toResponses(seats);
    }

    @GetMapping("/show/{showId}")
    public List<ShowSeatRes> getByShow(@PathVariable Integer showId) {
        return showSeatMapper.toResponses(
                showSeatService.getShowSeatsByShowId(showId)
        );
    }

    @PutMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ShowSeatRes update(
            @PathVariable Integer id,
            @RequestBody @Valid UpdateShowSeatReq req
    ) {
        return showSeatMapper.toResponse(
                showSeatService.updateShowSeat(id, req)
        );
    }
}
