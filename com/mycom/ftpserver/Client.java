package com.mycom.ftpserver;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        printWelcomeBanner();
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.print("Username: ");
            String username = scanner.nextLine();
            System.out.print("Password: ");
            String password = scanner.nextLine();

            out.println(username);
            out.println(password);

            String response = in.readLine();
            if ("SUCCESS".equals(response)) {
                System.out.println("Authenticated successfully.");
                String serverResponse;
                while (true) {
                    System.out.print("> ");
                    String command = scanner.nextLine().toLowerCase(); // Make command case-insensitive
                    out.println(command);
                    if (command.startsWith("download")) {
                        receiveFile(command.split(" ")[1], socket);
                    } else if (command.startsWith("upload")) {
                        uploadFile(command.split(" ")[1], socket, in, out);
                    } else if (command.startsWith("showfiles")) {
                        showFiles(in);
                    } else if (command.startsWith("search")) {
                        searchFile(in);
                    } else {
                        while (!(serverResponse = in.readLine()).equals("END")) {
                            System.out.println(serverResponse);
                        }
                    }
                }
            } else {
                System.out.println("Authentication failed.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printWelcomeBanner() {
        System.out.println("=========================================");
        System.out.println(" Welcome to the FTP Using Java Socket Programming ");
        System.out.println(" Developed by: ");
        System.out.println(" Sabbir Ahmed, Raihan Kabir, Ramjan Ali ");
        System.out.println(" Green University of Bangladesh ");
        System.out.println("=========================================");
    }

    private static void receiveFile(String fileName, Socket socket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Wait for BEGIN_FILE_TRANSFER
            String response = reader.readLine();
            if (!"BEGIN_FILE_TRANSFER".equals(response)) {
                System.out.println("Server did not send BEGIN_FILE_TRANSFER.");
                return;
            }

            // Read file size
            long fileSize = Long.parseLong(reader.readLine());
            System.out.println("Receiving file: " + fileName + " (" + fileSize + " bytes)");

            // Small delay to ensure we've received all control messages
            Thread.sleep(100);

            // Read file data
            try (FileOutputStream fos = new FileOutputStream(fileName);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                
                InputStream is = socket.getInputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalReceived = 0;

                while (totalReceived < fileSize) {
                    bytesRead = is.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalReceived));
                    if (bytesRead == -1) break;
                    
                    bos.write(buffer, 0, bytesRead);
                    totalReceived += bytesRead;
                    
                    // Show progress
                    System.out.printf("Received: %.2f%%\r", (totalReceived * 100.0) / fileSize);
                }
                bos.flush();
            }
            System.out.println("\nFile downloaded successfully: " + fileName);

            // Read remaining control messages
            while (!(response = reader.readLine()).equals("END")) {
                if (!"END_FILE_TRANSFER".equals(response)) {
                    System.out.println(response);
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error downloading file: " + e.getMessage());
        }
    }

    private static void uploadFile(String fileName, Socket socket,
                                   BufferedReader in, PrintWriter out) {
        String clientDir = System.getProperty("user.dir") + File.separator + 
                          "com" + File.separator + "mycom" + File.separator + "ftpserver";
        File localFile = new File(clientDir, fileName);
        
        System.out.println("Looking for file in package directory: " + localFile.getAbsolutePath());
        
        if (!localFile.exists() || !localFile.isFile()) {
            System.out.println("Local file not found at: " + localFile.getAbsolutePath());
            // Read the END marker from the server to keep protocol in sync
            try {
                String endMarker;
                while (!(endMarker = in.readLine()).equals("END_UPLOAD"));
            } catch (IOException e) {
                System.out.println("Error reading server response: " + e.getMessage());
            }
            return;
        }

        try {
            System.out.println("Found file, size: " + localFile.length() + " bytes");
            
            // Wait for server ready signal
            String response = in.readLine();
            if (!"BEGIN_FILE_UPLOAD".equals(response)) {
                System.out.println("Server not ready: " + response);
                // Read until END_UPLOAD marker
                while (response != null && !response.equals("END_UPLOAD")) {
                    response = in.readLine();
                }
                return;
            }

            // Create data streams
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
            
            // Send file size first
            dataOut.writeLong(localFile.length());
            dataOut.flush();

            // Send file data
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalSent = 0;
            long fileSize = localFile.length();

            try (FileInputStream fis = new FileInputStream(localFile);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                
                while ((bytesRead = bis.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                    totalSent += bytesRead;
                    System.out.printf("Sent: %.2f%%\r", (totalSent * 100.0) / fileSize);
                }
                dataOut.flush();
            }

            // Signal completion
            out.println("END_FILE_UPLOAD");
            out.flush();

            // Wait for server confirmation
            while (!(response = in.readLine()).equals("END_UPLOAD")) { // Changed from "END" to "END_UPLOAD"
                System.out.println(response);
            }
            System.out.println("\nFile upload completed");
        } catch (IOException e) {
            System.out.println("Error uploading file: " + e.getMessage());
            // Read until END_UPLOAD to keep protocol in sync
            try {
                String line;
                while ((line = in.readLine()) != null && !line.equals("END_UPLOAD")) { // Changed from "END" to "END_UPLOAD"
                    System.out.println(line);
                }
            } catch (IOException ex) {
                System.out.println("Error reading server response: " + ex.getMessage());
            }
        }
    }

    private static void showFiles(BufferedReader in) throws IOException {
        String line;
        while (!(line = in.readLine()).equals("END")) {
            System.out.println(line);
        }
    }

    private static void searchFile(BufferedReader in) throws IOException {
        String line;
        while (!(line = in.readLine()).equals("END")) {
            System.out.println(line);
        }
    }
}
