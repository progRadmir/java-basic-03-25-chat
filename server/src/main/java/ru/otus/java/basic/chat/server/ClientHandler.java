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
    private volatile String username;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
        start();
    }

    public Socket getSocket() {
        return socket;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    void start() {
        new Thread(() -> {
            try {
                sendMessage("Добрый день! Для перехода в чат авторизуйтесь/зарегистрируйтесь:");
                sendMessage("Для авторизации введите следующий запрос: '/auth login password'");
                sendMessage("Для регистрации введите следующий запрос: '/reg login password'");

                while(true) {
                    String msg = input.readUTF();
                    if(msg.startsWith("/reg") && server.checkMsgRegister(this, msg)) {
                        break;
                    }
                    if(msg.startsWith("/auth") && server.checkMsgAuthenticate(this, msg)) {
                        break;
                    }
                    if(msg.equals("/exit")) {
                        sendMessage("До свидания!");
                        sendMessage(msg);
                        return;
                    }
                    sendMessage("Для перехода в чат авторизуйтесь/зарегистрируйтесь");
                }

                server.subscribe(this);
                sendMessage("Вы вошли в чат под ником: " + username);
                while(true) {
                    String msg = input.readUTF();
                    if(!server.checkActiveUsers(username) && !msg.equals("/exit")) {
                        continue;
                    }
                    if(msg.startsWith("/")) {
                        if(msg.equals("/exit")) {
                            sendMessage("Вы покинули чат");
                            sendMessage(msg);
                            break;
                        }
                        if(msg.startsWith("/w")) {
                            server.personalMessage(this, msg);
                        }
                        if(msg.startsWith("/kick")) {
                            server.checkBeforeKick(this, msg);
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


    private synchronized void disconnect() {
        if(username == null) {
            System.out.println("Пользователь user_" + this.socket.getPort() + " отключился");
        } else if(server.checkActiveUsers(username)) {
            server.unsubscribe(this);
            System.out.println("Пользователь user_" + socket.getPort() + " (" + username + ") покинул чат");
            server.broadcastMessage("Пользователь " + username + " покинул чат");
        } else {
            System.out.println("Пользователь user_" + socket.getPort() + " (" + username + ") покинул чат");
            server.broadcastMessage("Пользователь " + username + " покинул чат");
        }

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
