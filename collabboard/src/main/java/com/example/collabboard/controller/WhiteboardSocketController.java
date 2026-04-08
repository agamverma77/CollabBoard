package com.example.collabboard.controller;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the corrected, stateful controller for the cloud server.
 * It intelligently manages sessions and relays messages for all cloud rooms.
 */
@Controller
public class WhiteboardSocketController {

    // In-memory storage for session management
    private final Map<String, Set<String>> roomParticipants = new ConcurrentHashMap<>(); // Maps Room Code -> Set of Usernames
    private final Map<String, String> sessionUsernames = new ConcurrentHashMap<>();     // Maps Session ID -> Username
    private final Map<String, String> sessionRooms = new ConcurrentHashMap<>();         // Maps Session ID -> Room Code

    private final SimpMessagingTemplate messagingTemplate;

    public WhiteboardSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * This method is now the central hub for all incoming messages for a room.
     * It parses the message to determine if it's a session command or data to be relayed.
     */
    @MessageMapping("/board/{roomCode}")
    public void handleAction(@DestinationVariable String roomCode, @Payload String data, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        if (data.startsWith("IDENTIFY:")) {
            // A user is joining the room and announcing who they are.
            String username = data.substring(9);
            sessionUsernames.put(sessionId, username);
            sessionRooms.put(sessionId, roomCode);
            roomParticipants.computeIfAbsent(roomCode, k -> ConcurrentHashMap.newKeySet()).add(username);
            System.out.println("User '" + username + "' joined cloud room '" + roomCode + "'");
            broadcastUserList(roomCode);
        } else {
            // For all other messages (DRAW, ERASE, CHAT, SCREEN_SHARE, etc.),
            // simply relay them to everyone subscribed to the room's topic.
            messagingTemplate.convertAndSend("/topic/board/" + roomCode, data);
        }
    }

    /**
     * This event listener is automatically triggered by Spring when a user disconnects.
     * It cleans up their session data and notifies the remaining users.
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String username = sessionUsernames.remove(sessionId);
        String roomCode = sessionRooms.remove(sessionId);

        if (username != null && roomCode != null) {
            Set<String> participants = roomParticipants.get(roomCode);
            if (participants != null) {
                participants.remove(username);
                System.out.println("User '" + username + "' left room '" + roomCode + "'");
                if (participants.isEmpty()) {
                    roomParticipants.remove(roomCode); // Clean up the empty room
                } else {
                    broadcastUserList(roomCode); // Notify remaining users of the change
                }
            }
        }
    }

    /**
     * Constructs and broadcasts the current list of participants to everyone in the room.
     */
    private void broadcastUserList(String roomCode) {
        Set<String> participants = roomParticipants.get(roomCode);
        if (participants != null) {
            String userListMessage = "USER_LIST:" + String.join(",", participants);
            messagingTemplate.convertAndSend("/topic/board/" + roomCode, userListMessage);
        }
    }
}

