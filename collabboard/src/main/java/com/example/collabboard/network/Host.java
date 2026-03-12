package com.example.collabboard.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Represents the Host (server) in a LAN peer-to-peer session.
 * This version contains the corrected logic for message broadcasting and forwarding.
 */
public class Host implements Runnable {
    private final int port;
    private ServerSocket serverSocket;
    private final Map<PrintWriter, String> clients = new ConcurrentHashMap<>();
    private final Consumer<String> onDataReceived;

    public Host(int port, Consumer<String> onDataReceived) {
        this.port = port;
        this.onDataReceived = onDataReceived;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, this)).start();
            }
        } catch (IOException e) {
            System.out.println("Host server shut down.");
        }
    }

    /**
     * THIS METHOD IS FIXED.
     * Sends a message originating from the Host's UI to all connected clients.
     * It no longer echoes the message back to the host's own UI.
     */
    public void broadcast(String message) {
        System.out.println("[HOST - BROADCASTING]: " + message);
        clients.keySet().forEach(writer -> writer.println(message));
    }

    /**
     * DEFINITIVE FIX 2: This method correctly handles a message received from a client.
     * It updates the host's own UI and forwards the message to all OTHER clients.
     */
    public void forwardMessage(String message, PrintWriter sender) {
        System.out.println("[HOST - FORWARDING]: " + message);
        // 1. Update the host's UI with the client's action.
        onDataReceived.accept(message);
        
        // 2. Forward the message to all other connected clients.
        clients.forEach((writer, username) -> {
            if (writer != sender) { // Do not send back to the original sender
                writer.println(message);
            }
        });
    }

    // --- All other methods for session management are correct ---

    public void addClient(PrintWriter writer, String username) {
        clients.put(writer, username);
        broadcastUserList();
    }

    private void broadcastUserList() {
        String userList = String.join(",", clients.values());
        // The host must be included in the user list for everyone to see.
        String hostUsername = "Host"; // This could be improved to get the host's actual username from SessionManager
        
        // Create the full message
        String fullUserListMessage = "USER_LIST:" + hostUsername + "," + userList;

        // Update the host's own UI first
        onDataReceived.accept(fullUserListMessage);
        
        // Then, broadcast only to the clients
        clients.keySet().forEach(writer -> writer.println(fullUserListMessage));
    }

    public void kickUser(String usernameToKick) {
        clients.entrySet().stream()
            .filter(entry -> entry.getValue().equals(usernameToKick))
            .findFirst()
            .ifPresent(entry -> {
                PrintWriter writerToKick = entry.getKey();
                writerToKick.println("YOU_WERE_KICKED");
                removeClient(writerToKick);
            });
    }

    public void removeClient(PrintWriter writer) {
        clients.remove(writer);
        broadcastUserList();
    }

    public void shutdown() {
        try {
            clients.clear();
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final Host host;
        private PrintWriter writer;

        public ClientHandler(Socket socket, Host host) {
            this.clientSocket = socket;
            this.host = host;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                this.writer = new PrintWriter(clientSocket.getOutputStream(), true);

                String identifyMessage = reader.readLine();
                if (identifyMessage != null && identifyMessage.startsWith("IDENTIFY:")) {
                    String username = identifyMessage.substring(9);
                    host.addClient(this.writer, username);
                } else {
                    clientSocket.close();
                    return; // Invalid connection
                }

                String message;
                while ((message = reader.readLine()) != null) {
                    host.forwardMessage(message, this.writer);
                }
            } catch (IOException e) {
                // Client disconnected
            } finally {
                if (writer != null) {
                    host.removeClient(writer);
                }
            }
        }
    }
}

