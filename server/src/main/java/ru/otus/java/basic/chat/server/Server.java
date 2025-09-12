package ru.otus.java.basic.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int port;
    private List<ClientHandler> listOfClient;

    public Server(int port) {
        this.port = port;
        listOfClient = new CopyOnWriteArrayList<>();
        start();
    }

    void start(){
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту: " + port);
            while(true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, this);
                System.out.println("Подключен пользователь: " + clientHandler.getUsername());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void subscribe(ClientHandler clientHandler) {
        listOfClient.add(clientHandler);
    }

    void unsubscribe(ClientHandler clientHandler) {
        listOfClient.remove(clientHandler);
        System.out.println("Пользователь " + clientHandler.getUsername() + " покинул чат");
    }

    void broadcastMessage(String msg) {
        for(ClientHandler clientHandler : listOfClient) {
            clientHandler.sendMessage(msg);
        }
    }

    void personalMessage(ClientHandler clientHandler, String msg) {
        String[] request = msg.split(" ", 3);
        String userTarget = request[1];
        String text = request[2];
        for(ClientHandler clientHandlerTarget : listOfClient) {
            if(clientHandlerTarget.getUsername().equals(userTarget)) {
                clientHandlerTarget.sendMessage(clientHandler.getUsername() + ": " + text);
                clientHandler.sendMessage(clientHandler.getUsername() + ": " + text);
                return;
            }
        }
        clientHandler.sendMessage("Такого пользователя не существует");
    }

}
