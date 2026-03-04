package src.networking;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class GameSubscriber {
    Socket socket;
    public GameSubscriber(Socket socket){
        this.socket  = socket;
    }
    public Socket getSocket(){
        return socket;
    }

    public void send(Object object) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(object);
    }

}
