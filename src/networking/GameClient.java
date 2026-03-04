package src.networking;

import src.screens.ConnectionScreen;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class GameClient {
    private Socket socket;
    private Object data;

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

    public void send(Object object) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(object);
    }



    // theads
    public void connectAsync(String ip, int port, Consumer<String> onConnected, Consumer<String> onError) {
        Thread thread = new Thread(() -> {
            try {
                connect(ip, port, onConnected);
                updateDataAsync();
            } catch (Exception e) {
                if (onError != null) {
                    onError.accept(e.getMessage());
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void updateDataAsync(){
        Thread thread = new Thread(() -> {
            try{
                ObjectInputStream inputStream = new ObjectInputStream((socket.getInputStream()));
                data =  inputStream.readObject();
                changeConnectionScreen();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void changeConnectionScreen(){
        ConnectionScreen.dataLabel().setText((String) data);
        updateDataAsync();
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