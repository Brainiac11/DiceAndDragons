package src.networking;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class GameClient {
    private Socket socket;

    public void connect(String ip, int port, Consumer<String> onConnected) throws IOException, ClassNotFoundException {
        socket = new Socket(ip, port);
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        String received = (String) in.readObject();
        out.writeObject("Connectada");
        out.flush();
        if (onConnected != null) {
            onConnected.accept(received);
        }
    }

    // theads
    public void connectAsync(String ip, int port, Consumer<String> onConnected, Consumer<String> onError) {
        Thread thread = new Thread(() -> {
            try {
                connect(ip, port, onConnected);
            } catch (Exception e) {
                if (onError != null) {
                    onError.accept(e.getMessage());
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}