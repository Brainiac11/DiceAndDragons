package src.networking;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class GameServer {
    private ServerSocket serverSocket;
    private Consumer<String> onConnected;

    public GameServer() throws IOException {
        serverSocket = new ServerSocket(0); // gonna bind the rando port but might work on getting this more polished
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public void setOnConnected(Consumer<String> callback) {
        this.onConnected = callback;
    }

    public void startListening() {
        Thread thread = new Thread(() -> {
            try {
                Socket clientSocket = serverSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                out.writeObject("Connected");
                out.flush();
                String received = (String) in.readObject();
                if (onConnected != null) {
                    onConnected.accept(received);
                }
            } catch (Exception e) {
                if (!serverSocket.isClosed()) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void close() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}