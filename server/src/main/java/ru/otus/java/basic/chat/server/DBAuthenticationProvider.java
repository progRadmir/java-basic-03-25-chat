package ru.otus.java.basic.chat.server;


public class DBAuthenticationProvider implements AuthenticationProvider{

    private final Server server;
    private DBInteraction dbInteraction = null;

    public DBAuthenticationProvider(Server server) {
        this.server = server;
    }

    @Override
    public void initialize() {
        dbInteraction = new DBInteraction("jdbc:postgresql://localhost:8090/chatdb", "admin", "admin");
        System.out.println("AuthenticationProvider запущен в режиме DataBase");
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        if (dbInteraction == null) {
            clientHandler.sendMessage("Соединение с базой данных не установлено");
            return false;
        }
        if(!dbInteraction.checkAuthentication(login, password)) {
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
        if (dbInteraction == null) {
            clientHandler.sendMessage("Соединение с базой данных не установлено");
            return false;
        }
        if(!login.matches("[a-zа-яA-ZА-Я0-9@_-]{4,}")) {
            clientHandler.sendMessage("Логин должен быть больше 3 символов, не используйте спец. сиволы");
            return false;
        }
        if(dbInteraction.checkUser(login)) {
            clientHandler.sendMessage("Такой логин уже занят");
            return false;
        }
        if(!password.matches("(?=.*?[a-zа-я])(?=.*?[A-ZА-Я])(?=.*?[0-9]).{4,}")) {
            clientHandler.sendMessage("Пароль должен быть больше 3 символов, содержать строчные и прописные буквы, а также цифры");
            return false;
        }
        dbInteraction.addUser(login, password);
        clientHandler.setUsername(login);
        return true;
    }

    public void close() {
        dbInteraction.close();
        System.out.println("AuthenticationProvider завершил работу. Соединение с DataBase закрыто");
    }

    public boolean checkOnAdmin(String login) {
        return dbInteraction.checkOnAdmin(login);
    }

}


