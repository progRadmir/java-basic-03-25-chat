package ru.otus.java.basic.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final int port;
    private final Map<String, ClientHandler> activeClients;
    private final DBAuthenticationProvider authenticationProvider;

    public Server(int port) {
        this.port = port;
        activeClients = new ConcurrentHashMap<>();
        authenticationProvider = new DBAuthenticationProvider(this);
        start();
    }

    void start(){
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту: " + port);
            authenticationProvider.initialize();
            while(true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, this);
                System.out.println("Подключен пользователь: user_" + socket.getPort());
            }
        } catch (IOException e) {
            authenticationProvider.close();
            throw new RuntimeException(e);
        }
    }

    void subscribe(ClientHandler clientHandler) {
        activeClients.put(clientHandler.getUsername(), clientHandler);
    }

    void unsubscribe(ClientHandler clientHandler) {
        activeClients.remove(clientHandler.getUsername());
    }

    void broadcastMessage(String msg) {
        for(ClientHandler clientHandler : activeClients.values()) {
            clientHandler.sendMessage(msg);
        }
    }

    void personalMessage(ClientHandler clientHandler, String msg) {
        String[] request = msg.split(" ", 3);
        String userTarget = request[1];
        String text = request[2];
        if (activeClients.containsKey(userTarget)) {
            activeClients.get(userTarget).sendMessage("Персональное сообщение от " + clientHandler.getUsername() + ": " + text);
            clientHandler.sendMessage("Персональное сообщение для " + activeClients.get(userTarget).getUsername() + ": " + text);
            return;
        }
        clientHandler.sendMessage("Такого пользователя сейчас нет в чате");
    }

    boolean checkMsgAuthenticate(ClientHandler clientHandler, String msg) {
        String[] text = msg.split(" ");
        if(text.length != 3) {
            clientHandler.sendMessage("Неверный формат запроса '/auth login password'");
            return false;
        }
        return authenticationProvider.authenticate(clientHandler, text[1], text[2]);
    }

    boolean checkMsgRegister(ClientHandler clientHandler, String msg) {
        String[] text = msg.split(" ");
        if(text.length != 3) {
            clientHandler.sendMessage("Неверный формат запроса '/reg login password'");
            return false;
        }
        return authenticationProvider.register(clientHandler,  text[1], text[2]);

    }

    boolean checkActiveUsers(String username) {
        return activeClients.containsKey(username);
    }

    private void kick(ClientHandler clientHandler, String login) {
        ClientHandler bannedUser = activeClients.get(login);
        activeClients.get(login).sendMessage("Вы попали в бан на 20 секунд");
        bannedUser.sendMessage("/kick");
        activeClients.remove(login);
        broadcastMessage("Пользователь " + bannedUser.getUsername() + " попал в бан на 20 секунд");
        new Thread(() -> {
            try {
                Thread.sleep(20000);
                if(bannedUser.getSocket().isClosed()) {
                    return;
                }
                subscribeAfterKick(bannedUser);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    void checkBeforeKick(ClientHandler clientHandler, String msg) {
        String[] text = msg.split(" ");
        if(text.length != 2) {
            clientHandler.sendMessage("Неверный формат запроса '/kick username'");
            return;
        }
        if(!authenticationProvider.checkOnAdmin(clientHandler.getUsername())) {
            clientHandler.sendMessage("Вы не можете банить других пользователей");
            return;
        }
        if(!activeClients.containsKey(text[1])) {
            clientHandler.sendMessage("Вы не можете банить неактивных пользователей");
            return;
        }
        kick(clientHandler, text[1]);
    }

    private void subscribeAfterKick(ClientHandler bannedUser) {
        synchronized (bannedUser) {
            if(bannedUser.getSocket().isClosed()) {
                return;
            }
            subscribe(bannedUser);
            bannedUser.sendMessage("/kickCancel");
            bannedUser.sendMessage("Время бана закончилось. Вы снова можете писать в чат");
        }
    }



}
