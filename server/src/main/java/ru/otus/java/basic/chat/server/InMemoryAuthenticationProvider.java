package ru.otus.java.basic.chat.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAuthenticationProvider implements AuthenticationProvider{

    private final Map<String, User> users;
    private final Server server;

    public InMemoryAuthenticationProvider(Server server) {
        this.users = new ConcurrentHashMap<>();
        this.server = server;
        users.put("admin", new User("admin", "admin", Role.ADMIN));
    }

    private class User{
        private final String login;
        private final String password;
        private final Role role;

        public User(String login, String password, Role role) {
            this.login = login;
            this.password = password;
            this.role = role;
        }
    }

    private String getUsername(String login, String password) {
        if(users.containsKey(login)) {
            if(users.get(login).password.equals(password)) {
                return login;
            }
        }
        return null;
    }

    public Role getRole(String login) {
        if(users.containsKey(login)) {
            return users.get(login).role;
        }
        return null;
    }

    @Override
    public void initialize() {
        System.out.println("AuthenticationProvider запущен в режиме InMemory");
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        if(getUsername(login, password) == null) {
            clientHandler.sendMessage("Неверный логин/пароль");
            return false;
        }
        if(server.checkActiveUsers(login)) {
            clientHandler.sendMessage("Данная учетная запись уже занята");
            return false;
        }
        clientHandler.setUsername(login);
        return true;
    }

    @Override
    public boolean register(ClientHandler clientHandler, String login, String password) {
        if(!login.matches("[a-zа-яA-ZА-Я0-9@_-]{4,}")) {
            clientHandler.sendMessage("Логин должен быть больше 3 символов, не используйте спец. сиволы");
            return false;
        }
        if(users.containsKey(login)) {
            clientHandler.sendMessage("Такой логин уже занят");
            return false;
        }
        if(!password.matches("(?=.*?[a-zа-я])(?=.*?[A-ZА-Я])(?=.*?[0-9]).{4,}")) {
            clientHandler.sendMessage("Пароль должен быть больше 3 символов, содержать строчные и прописные буквы, а также цифры");
            return false;
        }
        users.put(login, new User(login, password, Role.USER));
        clientHandler.setUsername(login);
        return true;
    }
}
