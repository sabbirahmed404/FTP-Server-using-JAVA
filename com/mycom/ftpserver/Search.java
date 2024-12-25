package com.mycom.ftpserver;

import java.io.*;
import java.nio.file.*;

public class Search {
    private static final String STORAGE_DIR = Paths.get("").toAbsolutePath()
        .resolve("../resources/storage")
        .normalize()
        .toString();
    private static final String LIST_FILE = Paths.get("").toAbsolutePath()
        .resolve("../resources/config/list.txt")
        .normalize()
        .toString();

    public static void updateFileList() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LIST_FILE))) {
            Files.walk(Paths.get(STORAGE_DIR))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        writer.write(path.toString());
                        writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String searchFile(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(LIST_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().endsWith(fileName.toLowerCase())) {
                    String subdirectory = line.split("\\\\")[line.split("\\\\").length - 2];
                    return "File found in " + subdirectory;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "File not found";
    }
}
