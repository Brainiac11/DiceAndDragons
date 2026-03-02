package src.networking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class AbstractNetworkManager implements Runnable {
    // Publishes its game data
    private ServerSocket gameConnectionSocket;
    // Recieves data from other connections -> allows both host and client
    // behaviors, where host has multiple socket connections, and client has 1
    private ArrayList<Socket> socketConnections;

    private ArrayList<ObjectInputStream> socketObjectInputs;

    public AbstractNetworkManager(int connectionSocketPort) {
        try {
            gameConnectionSocket = new ServerSocket(1024);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        socketConnections = new ArrayList<>();
    }

    public void setSocketPort(int port) throws IOException {
        gameConnectionSocket.close();
        gameConnectionSocket = new ServerSocket(port);
    }

    public Socket addSocketConnection(String ip, int port) throws UnknownHostException, IOException {
        Socket socket = new Socket(ip, port);
        socketConnections.add(socket);
        socketObjectInputs.add(new ObjectInputStream(socket.getInputStream()));
        return socket;
    }

    @Override
    public void run() {
        ArrayList<Boolean> connectedClients = new ArrayList<>(socketConnections.size());
        for (Socket socket : socketConnections) {
            connectedClients.add(socket.isConnected());
        }
    }

}
