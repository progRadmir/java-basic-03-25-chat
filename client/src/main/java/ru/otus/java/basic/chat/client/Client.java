package ru.otus.java.basic.chat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final Scanner scanner;

    public Client(String host, int port) {
        try {
            this.socket = new Socket(host, port);
            this.input = new DataInputStream(socket.getInputStream());
            this.output = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        scanner = new Scanner(System.in);
        start();
    }

    void start() {
        try {
            new Thread(() -> {
                try {
                    while (true) {
                        String msg = input.readUTF();
                        if (msg.equals("/exit")) {
                            output.writeUTF(msg);
                            break;
                        }
                        System.out.println(msg);
                    }
                    } catch(IOException e){
                        throw new RuntimeException(e);
                    } finally{
                        disconnect();
                    }
            }).start();
            while (true) {
                String msg = scanner.nextLine();
                if (msg.equals("/exit")) {
                    output.writeUTF(msg);
                    break;
                }
                output.writeUTF(msg);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void disconnect() {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (output != null) {
            try {
                output.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}


