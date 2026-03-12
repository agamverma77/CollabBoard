package com.example.collabboard.service;

import com.example.collabboard.model.Room;
import com.example.collabboard.model.User;
import com.example.collabboard.repository.RoomRepository;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     
     * @param owner 
     * @return
     */
    public Room createRoom(User owner) {
        Room newRoom = new Room();
        newRoom.setOwner(owner);
        return roomRepository.save(newRoom);
    }

    /**
     * @param roomCode 
     * @return
     */
    public Room findRoomByCode(String roomCode) {
        return roomRepository.findByRoomCode(roomCode).orElse(null);
    }
}