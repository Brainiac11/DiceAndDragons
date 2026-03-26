package src.networking;

import java.io.*;
import java.net.*;

public class GameSubscriber {
    public Socket socket;
    public ObjectInputStream input;
    public ObjectOutputStream output;
    public String handle;

    public GameSubscriber(Socket socket) throws IOException {
        this.socket = socket;
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());
    }

    public void send(Object obj) throws IOException {
        output.reset();
        output.writeObject(obj);
        output.flush();
    }

    public Object read() {
        try {
            return input.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}
