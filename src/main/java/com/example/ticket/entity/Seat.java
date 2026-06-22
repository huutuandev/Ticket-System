package com.example.ticket.entity;


import com.example.ticket.enums.SeatStatus;
import com.example.ticket.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

import javax.print.attribute.standard.MediaSize;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Seat {


    @Id   @GeneratedValue
    private Long id;

    @Column(name = "row_name")
    private String rowName;

    @Column(name = "seat_number")
    private Long seatNumber;


    @Enumerated(EnumType.STRING)
    private SeatType type;

    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    private BigDecimal price;

    @Version
    private Long version;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @OneToMany(mappedBy = "seat")
    private Set<BookingSeat> bookingSeats = new HashSet<>();
}
