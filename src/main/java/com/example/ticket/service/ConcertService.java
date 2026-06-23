package com.example.ticket.service;

import com.example.ticket.dto.request.CreateConcertRequest;
import com.example.ticket.dto.response.ConcertResponse;
import com.example.ticket.entity.Concert;
import com.example.ticket.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepo;

    public ConcertResponse createConcert(CreateConcertRequest concertRequest){
        Concert concert  = toEntity(concertRequest);
        concertRepo.save(concert);
        return toResponse(concert);
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
        concertResponse.setEventTime(concert.getEventTime());
        concertResponse.setCreatAt(concert.getCreatedAt());
        return concertResponse;
    }
}
