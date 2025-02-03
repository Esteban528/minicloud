package com.estebandev.minicloud.service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.exception.FileNotFoundException;

@Service
public class FileManagerService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

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

    public List<Path> listFiles(String path)
            throws FileNotFoundException, FileIsNotDirectoryException, IOException {
        verifyRootDirectory();

        Path root = getRoot();
        Path dir = root.resolve(path).normalize();

        if (!Files.exists(dir))
            throw new FileNotFoundException("The file is not exist");

        if (!Files.isDirectory(dir))
            throw new FileIsNotDirectoryException("The file is not directory");

        return Files.list(dir).toList();
    }

    public void makeDirectory(String dirPathString)
            throws FileAlreadyExistsException, IOException {
        Path dirPath = getRoot().resolve(dirPathString);

        if (Files.exists(dirPath))
            throw new FileAlreadyExistsException("The already exist");

        Files.createDirectory(dirPath);
    }

    public void uploadFile(MultipartFile multipartFile, String dirPathString)
            throws IOException, FileIsNotDirectoryException, FileNotFoundException {

        Path dirPath = getRoot().resolve(dirPathString).normalize();

        if (!Files.exists(dirPath))
            throw new FileNotFoundException(dirPath + " not exists");

        if (!Files.isDirectory(dirPath))
            throw new FileIsNotDirectoryException(dirPath + " is not directory");

        multipartFile.transferTo(dirPath.resolve(multipartFile.getOriginalFilename()));
    }

    public Path getRoot() {
        return Path.of(pathString);
    }
}
