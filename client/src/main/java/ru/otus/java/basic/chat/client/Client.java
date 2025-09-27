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
    private volatile boolean isBanned;

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
            Thread thread = new Thread(() -> {
                try {
                    while (true) {
                        String msg = input.readUTF();
                        if (msg.equals("/exit")) {
                            break;
                        }
                        if (msg.equals("/kick")) {
                            isBanned = true;
                            continue;
                        }
                        if (msg.equals("/kickCancel")) {
                            isBanned = false;
                            continue;
                        }
                        System.out.println(msg);
                    }
                    } catch(IOException e){
                        throw new RuntimeException(e);
                    } finally{
                        disconnect();
                    }
            });
            thread.start();
            while (true) {
                String msg = scanner.nextLine();
                if(!thread.isAlive()) {
                    break;
                }
                if (isBanned && !msg.equals("/exit")) {
                    continue;
                }
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


