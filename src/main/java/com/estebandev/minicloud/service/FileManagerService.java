package com.estebandev.minicloud.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.utils.FileData;
import com.estebandev.minicloud.service.utils.FileManagerUtils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Service
@RequiredArgsConstructor
@Setter
public class FileManagerService {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserService userService;
    private final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();
    private final FileMetadataService fileMetadataService;

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
            logger.error("FATAL ERROR {}", e.getMessage());
            System.exit(1);
        }
    }

    public List<FileData> listFiles(String path)
            throws FileNotFoundException, FileIsNotDirectoryException, IOException {
        fileLock.readLock().lock();
        try {

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
        } finally {
            fileLock.readLock().unlock();
        }
    }

    @Transactional
    private void makeDirectory(Path dirPath)
            throws FileAlreadyExistsException, IOException {
        fileLock.writeLock().lock();
        try {
            verifyRootDirectory();

            if (Files.exists(dirPath))
                throw new FileAlreadyExistsException("The already exist");

            Files.createDirectory(dirPath);
            fileMetadataService.make(dirPath, userService.getUserFromAuth());
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    public void makeDirectory(String dirPathString)
            throws FileAlreadyExistsException, IOException {
        Path dirPath = getRoot().resolve(dirPathString).normalize();

        makeDirectory(dirPath);
    }

    public void makeDirectory(String dirPathString, String dirName)
            throws FileAlreadyExistsException, IOException {
        makeDirectory(getRoot().resolve(dirPathString).normalize().resolve(FileManagerUtils.formatName(dirName)));
    }

    @Transactional
    public void uploadFile(MultipartFile multipartFile, String dirPathString)
            throws IOException, FileIsNotDirectoryException, FileNotFoundException {
        try {
            if (!fileLock.writeLock().tryLock(5, TimeUnit.SECONDS)) {
                throw new IOException("Upload is unable now");
            }

            try {
                verifyRootDirectory();

                Path dirPath = getRoot().resolve(dirPathString);

                if (!Files.exists(dirPath))
                    throw new FileNotFoundException("File does not exist");

                if (!Files.isDirectory(dirPath))
                    throw new FileIsNotDirectoryException(dirPath + " is not directory");

                String fileName = FileManagerUtils.formatName(multipartFile.getOriginalFilename());
                Path filePath = dirPath.resolve(fileName);
                if (Files.exists(filePath)) {
                    filePath = dirPath.resolve(FileManagerUtils.uniqueName(fileName));
                }

                multipartFile.transferTo(filePath);
            } finally {
                fileLock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupt to try get lock", e);
        }
    }

    @Transactional
    public Path rename(String pathString, String newName) throws IOException {
        try {
            if (!fileLock.writeLock().tryLock(5, TimeUnit.SECONDS)) {
                throw new IOException("Rename is unable now");
            }

            try {

                Path filePath = getRoot().resolve(pathString);
                Path newFilePath = filePath.getParent().resolve(FileManagerUtils.formatName(newName));

                Files.move(filePath, newFilePath);
                return getRoot().relativize(newFilePath);
            } finally {
                fileLock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupt to try get lock", e);
        }
    }

    public Resource findFile(String pathString) throws FileNotFoundException, IOException {
        fileLock.readLock().lock();
        try {
            verifyRootDirectory();

            Path filePath = getRoot().resolve(pathString);

            if (!Files.exists(filePath))
                throw new FileNotFoundException("File not exists");

            if (Files.isDirectory(filePath))
                throw new IOException("The file is a directory");

            FileManagerUtils.validateFile(filePath);

            return new FileSystemResource(filePath);

        } finally {
            fileLock.readLock().unlock();
        }
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
                .mediaType(FileManagerUtils.getMimeType(path))
                .size(FileManagerUtils.convertBytesToMegabytes(Files.size(path)))
                .directory(Files.isDirectory(path))
                .editable(editable)
                .build();
    }

    public void delete(String pathString) throws FileNotFoundException, IOException {
        try {
            if (!fileLock.writeLock().tryLock(5, TimeUnit.SECONDS)) {
                throw new IOException("Renable is unable now");
            }
            try {
                Path filePath = getRoot().resolve(pathString);

                if (!Files.exists(filePath))
                    throw new FileNotFoundException();
                if (!Files.isWritable(filePath))
                    throw new IOException("You do not have perms");
                if (Files.isDirectory(filePath) && !listFiles(pathString).isEmpty())
                    throw new IOException("The directory is not empty");
                if(Files.isDirectory(filePath))
                    fileMetadataService.deleteAll(filePath);

                Files.delete(filePath);
            } finally {
                fileLock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupt to try get lock", e);
        }
    }

    public Path getRoot() {
        return Path.of(pathString).normalize();
    }

    public long getLastModifiedDateInMinutes(String pathString) throws IOException {
        Path filePath = getRoot().resolve(pathString);
        FileTime fTime = Files.getLastModifiedTime(filePath);
        return fTime.to(TimeUnit.MINUTES);
    }

    public boolean isExist(String path) {
        return Files.exists(getRoot().resolve(path));
    }

    public boolean isValidDirectory(String path) {
        return isExist(path) && Files.isDirectory(getRoot().resolve(path));
    }

    public String getMimeType(String filePathString) throws IOException {
        return FileManagerUtils.getMimeType(getRoot().resolve(filePathString));
    }

}
