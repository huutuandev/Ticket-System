package com.example.ticket.service;

import com.example.ticket.dto.request.CreateConcertRequest;
import com.example.ticket.dto.response.ConcertResponse;
import com.example.ticket.entity.Concert;
import com.example.ticket.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import com.example.ticket.dto.request.UpdateConcertRequest;
import com.example.ticket.exception.ConcertNotFoundException;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepo;

    public ConcertResponse createConcert(CreateConcertRequest concertRequest){
        Concert concert  = toEntity(concertRequest);
        concert.setImageUrl(concertRequest.getImageUrl());
        concertRepo.save(concert);
        return toResponse(concert);
    }

    @Transactional
    public ConcertResponse updateConcert(Long id, UpdateConcertRequest request) {
        Concert concert = concertRepo.findById(id)
                .orElseThrow(() -> new ConcertNotFoundException(id));

        if (request.getName() != null) {
            concert.setName(request.getName());
        }
        if (request.getLocation() != null) {
            concert.setLocation(request.getLocation());
        }
        if (request.getEventTime() != null) {
            concert.setEventTime(request.getEventTime());
        }
        if (request.getDescription() != null) {
            concert.setDescription(request.getDescription());
        }
        if (request.getImageUrl() != null) {
            concert.setImageUrl(request.getImageUrl());
        }

        Concert saved = concertRepo.save(concert);
        return toResponse(saved);
    }
    public Page<ConcertResponse> getAllConcerts(Pageable pageable){
        Page<Concert> page = concertRepo.findAll(pageable);
        return page.map(this::toResponse);
    }

    public ConcertResponse getConcertById(Long id){
        Concert concert = concertRepo.findById(id)
                .orElseThrow(()-> new RuntimeException("không tìm thấy concert "+id));
        return toResponse(concert);
    }


    private Concert toEntity(CreateConcertRequest concertRequest){
        Concert concert = new Concert();
        concert.setName(concertRequest.getName());
        concert.setLocation(concertRequest.getLocation());
        concert.setEventTime(concertRequest.getEventTime());
        concert.setDescription(concertRequest.getDescription());
        return concert;
    }

    private ConcertResponse toResponse(Concert concert){
        ConcertResponse concertResponse = new ConcertResponse();
        concertResponse.setId(concert.getId());
        concertResponse.setName(concert.getName());
        concertResponse.setLocation(concert.getLocation());
        concertResponse.setDescription(concert.getDescription());
        concertResponse.setImageUrl(concert.getImageUrl());
        concertResponse.setEventTime(concert.getEventTime());
        concertResponse.setCreatAt(concert.getCreatedAt());
        return concertResponse;
    }
}
