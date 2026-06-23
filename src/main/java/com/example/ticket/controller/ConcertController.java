package com.example.ticket.controller;

import com.example.ticket.dto.request.CreateConcertRequest;
import com.example.ticket.dto.response.ConcertResponse;
import com.example.ticket.service.ConcertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;


    @PostMapping
    public ResponseEntity<ConcertResponse> createConcert(@RequestBody @Valid CreateConcertRequest concertRequest){
        return ResponseEntity.ok(concertService.createConcert(concertRequest));
    }


    @GetMapping
    public ResponseEntity<Page<ConcertResponse>> getAll(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size)
    {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(concertService.getAllConcerts(pageable));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ConcertResponse> getById(@PathVariable Long id){
        return ResponseEntity.ok(concertService.getConcertById(id));
    }
}
