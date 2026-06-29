package com.example.ticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "concerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Concert {

    @Id  @GeneratedValue
    private Long id;

    private String name;

    private String location;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(name = "image_url")
    private String imageUrl;


    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "concert", cascade = {CascadeType.MERGE, CascadeType.PERSIST}, orphanRemoval = true)
    private Set<Seat> seats = new HashSet<>();
}
