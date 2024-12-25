package com.mycom.ftpserver;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;
    private static final String STORAGE_DIR = Paths.get("").toAbsolutePath()
        .resolve("../resources/storage")
        .normalize()
        .toString();

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress()); // Added logging
                Search.updateFileList(); // Update file list whenever a new client connects
                pool.execute(new ClientHandler(clientSocket, STORAGE_DIR));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
