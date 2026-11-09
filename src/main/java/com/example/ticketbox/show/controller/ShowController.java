package com.example.ticketbox.show.controller;

import com.example.ticketbox.entity.Show;
import com.example.ticketbox.show.dto.Request.CreateShowReq;
import com.example.ticketbox.show.dto.Request.GetShowsReq;
import com.example.ticketbox.show.dto.Request.UpdateShowReq;
import com.example.ticketbox.show.dto.Response.ShowResponse;
import com.example.ticketbox.show.dto.ShowMapper;
import com.example.ticketbox.show.service.ShowServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowServiceImpl showService;
    private final ShowMapper showMapper;

    @PostMapping
    public ResponseEntity<ShowResponse> createShow(
            @Valid @RequestBody CreateShowReq req
    ) {
        Show show = showService.createShow(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(showMapper.toResponse(show));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShow(@PathVariable Integer id) {
        Show show = showService.getShowById(id);
        return ResponseEntity.ok(showMapper.toResponse(show));
    }

    @GetMapping
    public ResponseEntity<List<ShowResponse>> getShows(
            @ModelAttribute GetShowsReq req
    ) {
        List<Show> shows = showService.getShows(req);
        return ResponseEntity.ok(showMapper.toResponses(shows));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShowResponse> updateShow(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateShowReq req
    ) {
        Show show = showService.updateShow(id, req);
        return ResponseEntity.ok(showMapper.toResponse(show));
    }
}

