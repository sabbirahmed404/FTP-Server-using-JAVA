package com.mycom.ftpserver;

import java.io.*;
import java.net.*;
import java.nio.file.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String storageDir;
    private Authentication auth;
    private BufferedReader in;
    private PrintWriter out;
    private Path currentDir;

    public ClientHandler(Socket clientSocket, String storageDir) {
        this.clientSocket = clientSocket;
        this.storageDir = Paths.get("").toAbsolutePath()
            .resolve("../resources/storage")
            .normalize()
            .toString();
        this.auth = new Authentication();
        this.currentDir = Paths.get(this.storageDir);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String username = in.readLine();
            String password = in.readLine();

            if (auth.authenticate(username, password)) {
                out.println("SUCCESS");
                sendWelcomeBanner();
                handleClientCommands();
            } else {
                out.println("FAILURE");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Comment out or remove the socket close line:
            // clientSocket.close();
        }
    }

    private void handleClientCommands() throws IOException {
        while (true) {
            String rawCommand = in.readLine();
            if (rawCommand == null) {
                break; // Exit if client disconnects
            }
            // Trim the command
            String command = rawCommand.trim();
            String[] parts = command.split("\\s+", 2);
            String mainCmd = parts[0];

            // Handle download and upload commands separately
            if (mainCmd.equalsIgnoreCase("download") || mainCmd.equalsIgnoreCase("upload")) {
                if (mainCmd.equalsIgnoreCase("download")) {
                    downloadFile(command);
                } else {
                    uploadFile(command);
                }
            } else {
                // Wrap other commands with command result markers
                out.println("\n=== Command Result Start ===");
                if (mainCmd.equalsIgnoreCase("ls")) {
                    listFiles();
                } else if (mainCmd.equalsIgnoreCase("cd")) {
                    changeDirectory(command);
                } else if (mainCmd.equalsIgnoreCase("pwd")) {
                    printWorkingDirectory();
                } else if (mainCmd.equalsIgnoreCase("showfiles")) {
                    showFiles(command);
                } else if (mainCmd.equalsIgnoreCase("search")) {
                    searchFile(command);
                } else if (mainCmd.equalsIgnoreCase("help")) {
                    sendHelp();
                } else {
                    out.println("Invalid command");
                }
                out.println("=== Command Result End ===\n");
                out.println("END");
            }
        }
    }

    private void listFiles() {
        File dir = currentDir.toFile();
        File[] files = dir.listFiles();
        if (files != null) {
            out.println("Contents of: " + currentDir);  // Add header
            for (File file : files) {
                if (file.isDirectory()) {
                    out.println("[DIR] " + file.getName());  // Mark directories
                } else {
                    out.println(file.getName());
                }
            }
        } else {
            out.println("Directory not found or is invalid.");
        }
    }

    private void changeDirectory(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 2) {
            Path newPath = currentDir.resolve(parts[1]).normalize();
            if (newPath.startsWith(storageDir) && Files.isDirectory(newPath)) {
                currentDir = newPath;
                out.println("Changed directory to: " + currentDir);
                // List contents of new directory immediately
                listFiles();
            } else {
                out.println("Invalid directory");
            }
        } else {
            out.println("Usage: cd <directory>");
        }
    }

    private void printWorkingDirectory() {
        out.println(currentDir.toString());
    }

    private void downloadFile(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 2) {
            Path filePath = currentDir.resolve(parts[1]).normalize();
            if (filePath.startsWith(storageDir) && Files.exists(filePath) && !Files.isDirectory(filePath)) {
                try {
                    // Send control messages using PrintWriter
                    out.println("BEGIN_FILE_TRANSFER");
                    out.println(Files.size(filePath));
                    out.flush(); // Ensure messages are sent immediately

                    // Send file data using raw OutputStream
                    OutputStream os = clientSocket.getOutputStream();
                    try (FileInputStream fis = new FileInputStream(filePath.toFile());
                         BufferedInputStream bis = new BufferedInputStream(fis)) {
                        
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        long totalSent = 0;
                        long fileSize = Files.size(filePath);
                        
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                            totalSent += bytesRead;
                            // Optional: Log progress on server side
                            System.out.printf("Sent: %.2f%%\r", (totalSent * 100.0) / fileSize);
                        }
                        os.flush();
                    }

                    // Send end markers
                    out.println("END_FILE_TRANSFER");
                    out.println("END");
                    out.flush();
                    
                } catch (IOException e) {
                    out.println("Error: " + e.getMessage());
                    out.println("END");
                    out.flush();
                }
            } else {
                out.println("File not found or is a directory");
                out.println("END");
                out.flush();
            }
        } else {
            out.println("Usage: download <file>");
            out.println("END");
            out.flush();
        }
    }

    private void uploadFile(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 2) {
            String fileName = parts[1];
            Path filePath = currentDir.resolve(fileName).normalize();
            
            try {
                // Signal ready to receive
                out.println("BEGIN_FILE_UPLOAD");
                out.flush();

                // Create data streams
                DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
                
                // Read file size
                long fileSize = dataIn.readLong();
                System.out.println("Receiving file: " + fileName + " (" + fileSize + " bytes)");

                // Receive file data
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalReceived = 0;

                try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    
                    while (totalReceived < fileSize) {
                        bytesRead = dataIn.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalReceived));
                        if (bytesRead == -1) break;
                        
                        bos.write(buffer, 0, bytesRead);
                        totalReceived += bytesRead;
                        System.out.printf("Received: %.2f%%\r", (totalReceived * 100.0) / fileSize);
                    }
                    bos.flush();
                }

                // Signal completion
                out.println("END_FILE_UPLOAD");
                out.flush();

                // Wait for client to send 'END_UPLOAD'
                String marker = in.readLine();
                if ("END_UPLOAD".equals(marker)) { // Changed from "END" to "END_UPLOAD"
                    System.out.println("\nFile received successfully");
                    out.println("File upload complete: " + fileName);
                    Search.updateFileList();
                }
            } catch (IOException e) {
                out.println("Error during upload: " + e.getMessage());
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException ex) {
                    System.err.println("Could not delete partial file: " + ex.getMessage());
                }
            }
            out.println("END_UPLOAD"); // Changed from "END" to "END_UPLOAD"
            out.flush();
        } else {
            out.println("Usage: upload <file>");
            out.println("END_UPLOAD"); // Changed from "END" to "END_UPLOAD"
            out.flush();
        }
    }

    private void showFiles(String command) {
        Path listPath = Paths.get("").toAbsolutePath()
            .resolve("../resources/config/list.txt")
            .normalize();
        try (BufferedReader reader = new BufferedReader(new FileReader(listPath.toFile()))) {
            String subdirectory = "";
            String[] parts = command.split(" ");
            if (parts.length > 1) {
                subdirectory = parts[1].toLowerCase();
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (subdirectory.isEmpty() || line.toLowerCase().contains("\\" + subdirectory + "\\")) {
                    String fileName = line.substring(line.lastIndexOf("\\") + 1);
                    out.println("File: " + fileName);
                }
            }
        } catch (IOException e) {
            out.println("Error reading file list: " + e.getMessage());
        }
    }

    private void searchFile(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 2) {
            String fileName = parts[1];
            String result = Search.searchFile(fileName);
            out.println(result);
        } else {
            out.println("Usage: search <file>");
        }
    }

    private void sendHelp() {
        out.println("Available commands:");
        out.println("ls - List files and directories");
        out.println("cd <directory> - Change directory");
        out.println("pwd - Print working directory");
        out.println("upload <file> - Upload a file to the server");
        out.println("download <file> - Download a file from the server");
        out.println("showfiles - Show contents of list.txt");
        out.println("search <file> - Search for a file");
        out.println("help - Show this help message");
        out.println("Exit - To Terminate the Show");

    }

    private void sendWelcomeBanner() {
        out.println("=========================================");
        out.println(" Welcome to the FTP Using Java Socket Programming ");
        out.println(" Developed by: ");
        out.println(" Sabbir Ahmed");
        out.println(" github.com/sabbirahmed404 ");
        out.println("=========================================");
        out.println(); // Empty line for spacing
        out.println(" Type Help to see the Commands and Their Description");
        out.println("END_BANNER"); // Add marker instead of empty line
    }
}
