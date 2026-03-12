package com.example.collabboard.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Data 
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String roomCode;

   
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

  
    @PrePersist
    protected void onCreate() {
       
        if (roomCode == null) {
            roomCode = UUID.randomUUID().toString().substring(0, 8);
        }
    }
}