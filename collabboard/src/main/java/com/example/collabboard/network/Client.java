package com.example.collabboard.network;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client implements Runnable {
    private final String hostIp;
    private final int port;
    private final String username; // NEW: Store the username
    private Socket socket;
    private PrintWriter writer;
    private final Consumer<String> onDataReceived;
    private final Runnable onSuccess;
    private final Consumer<Exception> onFailure;

    // UPDATED: Constructor now requires the username
    public Client(String hostIp, int port, String username, Consumer<String> onDataReceived, Runnable onSuccess, Consumer<Exception> onFailure) {
        this.hostIp = hostIp;
        this.port = port;
        this.username = username;
        this.onDataReceived = onDataReceived;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(hostIp, port);
            writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // THE FIX: Immediately tell the Host who we are so it doesn't drop the connection
            writer.println("IDENTIFY:" + username);

            onSuccess.run();

            String message;
            while ((message = reader.readLine()) != null) {
                onDataReceived.accept(message);
            }
        } catch (IOException e) {
            System.err.println("Failed to connect to host: " + e.getMessage());
            onFailure.accept(e);
        }
    }

    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    public void shutdown() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}