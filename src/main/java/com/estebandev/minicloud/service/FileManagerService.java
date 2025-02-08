package com.estebandev.minicloud.service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.estebandev.minicloud.component.MediatypeParser;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.exception.FileNotFoundException;
import com.estebandev.minicloud.service.utils.FileData;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileManagerService {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserService userService;

    @Value("${var.filepath}")
    private String pathString;

    public void verifyRootDirectory() {
        Path root = getRoot();
        if (Files.exists(root) && Files.isDirectory(root) && Files.isWritable(root)) {
            return;
        }

        try {
            if (Files.exists(root) && !Files.isDirectory(root)) {
                Files.delete(root);
            }

            Files.createDirectory(root);
        } catch (IOException e) {
            logger.error("FATAL ERROR {}", e.getMessage(), e.getStackTrace());
            System.exit(1);
        }
    }

    public List<FileData> listFiles(String path)
            throws FileNotFoundException, FileIsNotDirectoryException, IOException {
        verifyRootDirectory();

        Path root = getRoot();
        Path dir = root.resolve(path);

        if (!Files.exists(dir))
            throw new FileNotFoundException();

        if (!Files.isDirectory(dir))
            throw new FileIsNotDirectoryException("The file is not directory");

        return Files.list(dir)
                .map(filePath -> {
                    return FileData.builder()
                            .fileName(filePath.getFileName().toString())
                            .path(getRoot().relativize(filePath))
                            .directory(Files.isDirectory(filePath))
                            .build();
                })
                .toList();
    }

    private void makeDirectory(Path dirPath)
            throws FileAlreadyExistsException, IOException {
        verifyRootDirectory();

        if (Files.exists(dirPath))
            throw new FileAlreadyExistsException("The already exist");

        Files.createDirectory(dirPath);
    }

    public void makeDirectory(String dirPathString)
            throws FileAlreadyExistsException, IOException {
        Path dirPath = getRoot().resolve(dirPathString);

        makeDirectory(dirPath);
    }

    public void makeDirectory(String dirPathString, String dirName)
            throws FileAlreadyExistsException, IOException {
        makeDirectory(getRoot().resolve(dirPathString).resolve(formatName(dirName)));
    }

    public void uploadFile(MultipartFile multipartFile, String dirPathString)
            throws IOException, FileIsNotDirectoryException, FileNotFoundException {
        verifyRootDirectory();

        Path dirPath = getRoot().resolve(dirPathString).normalize();

        if (!Files.exists(dirPath))
            throw new FileNotFoundException(dirPath);

        if (!Files.isDirectory(dirPath))
            throw new FileIsNotDirectoryException(dirPath + " is not directory");

        String fileName = formatName(formatName(multipartFile.getOriginalFilename()));
        Path filePath = dirPath.resolve(fileName);
        if (Files.exists(filePath)) {
            int dotPos = fileName.lastIndexOf('.');

            StringBuilder fileNameBuilder = new StringBuilder();

            double randomNumber = Math.floor(Math.random() * 10);
            if (dotPos > -1) {
                String name = fileName.substring(0, dotPos);
                String format = fileName.substring(dotPos);
                fileNameBuilder.append(name);
                fileNameBuilder.append(randomNumber);
                fileNameBuilder.append(format);
            } else {
                fileNameBuilder.append(fileName);
                fileNameBuilder.append(randomNumber);
            }

            fileName = fileNameBuilder.toString();
            filePath = dirPath.resolve(fileName);
        }

        multipartFile.transferTo(filePath);
    }

    public Path rename(String pathString, String newName) throws IOException {

        Path filePath = getRoot().resolve(pathString).normalize();
        Path newFilePath = filePath.getParent().resolve(formatName(newName));

        Files.move(filePath, newFilePath);
        return getRoot().relativize(newFilePath);
    }

    public Resource findFile(String pathString) throws FileNotFoundException, IOException {
        verifyRootDirectory();

        Path filePath = getRoot().resolve(pathString).normalize();

        if (!Files.exists(filePath))
            throw new FileNotFoundException(filePath);

        if (Files.isDirectory(filePath))
            throw new IOException("The file is a directory");

        validateFile(filePath);

        return new FileSystemResource(filePath);
    }

    public FileData findFileData(String pathString) throws FileNotFoundException, IOException {
        Path path = getRoot().resolve(pathString).normalize();
        String fileName = path.getFileName().toString();
        boolean editable = true;

        if (userService.getUserFromAuth().getEmail().equalsIgnoreCase(fileName)
                || path.equals(getRoot().normalize()))
            editable = false;

        return FileData.builder()
                .fileName(fileName)
                .path(path)
                .mediaType(getMimeType(path))
                .size(convertBytesToMegabytes(Files.size(path)))
                .directory(Files.isDirectory(path))
                .editable(editable)
                .build();
    }

    public void delete(String pathString) throws FileNotFoundException, IOException {
        Path filePath = getRoot().resolve(pathString);

        if (!Files.exists(filePath))
            throw new FileNotFoundException();
        if (!Files.isWritable(filePath))
            throw new IOException("You do not have perms");
        if (Files.isDirectory(filePath) && !listFiles(pathString).isEmpty())
            throw new IOException("The directory is not empty");

        Files.delete(filePath);
    }

    public Path getRoot() {
        return Path.of(pathString);
    }

    public boolean isExist(String path) {
        return Files.exists(getRoot().resolve(path));
    }

    public boolean isValidDirectory(String path) {
        return isExist(path) && Files.isDirectory(getRoot().resolve(path));
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

    public static String getMimeType(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "directory"; 
        }
        return MediatypeParser.getMediaType(fileName.substring(lastDotIndex + 1).toLowerCase());
    }

    public String getMimeType(String filePathString) throws IOException {
        return getMimeType(getRoot().resolve(filePathString));
    }

    public static String formatName(String name) {
        return name.trim().replaceAll("[ /%\\\\:*?\"'<>`]", "-");
    }

    public static double convertBytesToMegabytes(long bytes) {
        double result = (double) bytes / (1024 * 1024);
        return Math.round(result * 100.0) / 100.0;
    }
}
