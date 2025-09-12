package ru.otus.java.basic.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private final Socket socket;
    private final Server server;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final String username;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
        this.username = "user_" + socket.getPort();
        start();
        sendMessage("Вы подключились под ником: " + username);
    }

    public String getUsername() {
        return username;
    }

    void start() {
        new Thread(() -> {
            try {
                server.subscribe(this);
                while(true) {
                    String msg = input.readUTF();
                    if(msg.startsWith("/")) {
                        if(msg.equals("/exit")) {
                            sendMessage("Вы вышли из чата. До свидания!");
                            sendMessage(msg);
                            break;
                        }
                        if(msg.startsWith("/w")) {
                            server.personalMessage(this, msg);
                        }
                    }
                    else {
                        server.broadcastMessage(username + ": " + msg);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                disconnect();
            }
        }).start();
    }

    void sendMessage(String msg) {
        try {
            output.writeUTF(msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void disconnect() {
        server.unsubscribe(this);
        server.broadcastMessage("Пользователь " + username + " покинул чат");
        if(input != null) {
            try {
                input.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(output != null) {
            try {
                output.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
