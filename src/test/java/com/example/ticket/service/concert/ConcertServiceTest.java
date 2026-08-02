package com.example.ticket.service.concert;

import com.example.ticket.dto.request.CreateConcertRequest;
import com.example.ticket.dto.request.UpdateConcertRequest;
import com.example.ticket.dto.response.ConcertResponse;
import com.example.ticket.entity.Concert;
import com.example.ticket.exception.ConcertNotFoundException;
import com.example.ticket.repository.ConcertRepository;
import com.example.ticket.service.ConcertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConcertServiceTest {

    @Mock
    private ConcertRepository concertRepo;

    @InjectMocks
    private ConcertService concertService;

    private Concert concert;

    @BeforeEach
    void setUp() {
        // Concert không có @Builder nên sử dụng setter
        concert = new Concert();
        concert.setId(1L);
        concert.setName("Test Concert");
        concert.setLocation("Stadium");
        concert.setEventTime(LocalDateTime.now().plusDays(10));
        concert.setDescription("Awesome concert");
        concert.setImageUrl("http://image.url");
        concert.setCreatedAt(LocalDateTime.now());
    }

    // Kiểm tra tạo mới concert thành công
    @Test
    void createConcert_success() {
        // Arrange
        // CreateConcertRequest không có @Builder nên sử dụng setter
        CreateConcertRequest req = new CreateConcertRequest();
        req.setName("Test Concert");
        req.setLocation("Stadium");
        req.setEventTime(LocalDateTime.now().plusDays(10));
        req.setDescription("Awesome concert");
        req.setImageUrl("http://image.url");

        when(concertRepo.save(any(Concert.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ConcertResponse response = concertService.createConcert(req);

        // Assert
        assertNotNull(response);
        assertEquals("Test Concert", response.getName());
        assertEquals("http://image.url", response.getImageUrl());
        verify(concertRepo, times(1)).save(any(Concert.class));
    }

    // Kiểm tra cập nhật concert thành công, chỉ cập nhật các trường có trong request
    @Test
    void updateConcert_success() {
        // Arrange
        // UpdateConcertRequest không có @Builder nên sử dụng setter
        UpdateConcertRequest req = new UpdateConcertRequest();
        req.setName("Updated Name");
        req.setLocation("New Stadium");

        when(concertRepo.findById(1L)).thenReturn(Optional.of(concert));
        when(concertRepo.save(any(Concert.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ConcertResponse response = concertService.updateConcert(1L, req);

        // Assert
        assertEquals("Updated Name", response.getName());
        assertEquals("New Stadium", response.getLocation());
        assertEquals("Awesome concert", response.getDescription()); // Không bị đổi
        verify(concertRepo, times(1)).save(concert);
    }

    // Kiểm tra cập nhật thất bại khi concert không tồn tại
    @Test
    void updateConcert_notFound() {
        // Arrange
        UpdateConcertRequest req = new UpdateConcertRequest();
        when(concertRepo.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ConcertNotFoundException.class, () -> concertService.updateConcert(1L, req));
        verify(concertRepo, never()).save(any(Concert.class));
    }

    // Kiểm tra lấy danh sách concert có phân trang thành công
    @Test
    void getAllConcerts_success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Concert> page = new PageImpl<>(List.of(concert));
        when(concertRepo.findAll(pageable)).thenReturn(page);

        // Act
        Page<ConcertResponse> result = concertService.getAllConcerts(pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Concert", result.getContent().get(0).getName());
        verify(concertRepo, times(1)).findAll(pageable);
    }

    // Kiểm tra lấy concert theo id thành công
    @Test
    void getConcertById_success() {
        // Arrange
        when(concertRepo.findById(1L)).thenReturn(Optional.of(concert));

        // Act
        ConcertResponse response = concertService.getConcertById(1L);

        // Assert
        assertEquals("Test Concert", response.getName());
        assertEquals(1L, response.getId());
        verify(concertRepo, times(1)).findById(1L);
    }

    // Kiểm tra lấy concert theo id thất bại (không tồn tại)
    @Test
    void getConcertById_notFound() {
        // Arrange
        when(concertRepo.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> concertService.getConcertById(1L));
        assertTrue(ex.getMessage().contains("không tìm thấy concert"));
    }
}
