package ru.otus.java.basic.chat.server;

import java.sql.*;

public class DBInteraction {
    private final Connection connection;

    public DBInteraction(String DBURL, String username, String password) {
        try {
            connection = DriverManager.getConnection(DBURL, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addUser(String login, String password) {
        try (PreparedStatement pr = connection.prepareStatement("INSERT INTO users (user_login, user_password) " +
                "VALUES (?, ?)")) {
            pr.setString(1, login);
            pr.setString(2, password);
            pr.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkUser(String login) {
        try (PreparedStatement pr = connection.prepareStatement("SELECT user_login FROM users WHERE user_login = ?")) {
            pr.setString(1, login);
            ResultSet loginSet = pr.executeQuery();
            if (loginSet.next()) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkAuthentication(String login, String password) {
        try (PreparedStatement pr = connection.prepareStatement("SELECT user_password FROM users WHERE user_login = ?")) {
            pr.setString(1, login);
            ResultSet passwordSet = pr.executeQuery();
            if (!passwordSet.next()) {
                return false;
            }
            if (!passwordSet.getString(1).equals(password)) {
                return false;
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkOnAdmin(String login) {
        try (PreparedStatement pr = connection.prepareStatement("""
                SELECT u.user_login, r.role_name FROM users u
                JOIN user_roles r ON u.user_role = r.role_id
                WHERE u.user_login = ?""")) {
            pr.setString(1, login);
            ResultSet roleSet = pr.executeQuery();
            if (!roleSet.next()) {
                return false;
            }
            if (!roleSet.getString(2).equals("ADMIN")) {
                return false;
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try{
            if(connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
