package com.estebandev.minicloud.service.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import com.estebandev.minicloud.component.MediatypeParser;

public class FileManagerUtils {
    public static String formatName(String name) {
        return name.trim().replaceAll("[ /%\\\\:*?\"'<>`]", "-");
    }

    public static double convertBytesToMegabytes(long bytes) {
        double result = (double) bytes / (1024 * 1024);
        return Math.round(result * 100.0) / 100.0;
    }

    public static String generateTwoDigitId() {
        Random random = new Random();
        int number = random.nextInt(100);
        return String.format("%02d", number);
    }

    public static String uniqueName(String fileName) {
        int dotPos = fileName.lastIndexOf('.');
        String id = generateTwoDigitId();

        if (dotPos > -1) {
            String name = fileName.substring(0, dotPos);
            String extension = fileName.substring(dotPos);
            fileName = name + id + extension;
        } else {
            fileName += id;
        }
        return fileName;
    }

    public static String getMimeType(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "directory";
        }
        return MediatypeParser.getMediaType(fileName.substring(lastDotIndex + 1).toLowerCase());
    }

    public static void validateFile(Path filePath) throws IOException, FileNotFoundException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException();
        }
        if (!Files.isRegularFile(filePath)) {
            throw new IOException("Is not archive " + filePath);
        }
        if (!Files.isReadable(filePath)) {
            throw new IOException("You do not have perms to read it: " + filePath);
        }
    }

    public static Path getParent(String pathString) {
        Path path = Path.of(pathString).getParent();
        return path == null ? Path.of(".") : path;
    }

}
