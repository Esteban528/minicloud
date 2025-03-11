package com.estebandev.minicloud.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.estebandev.minicloud.entity.FileMetadata;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.utils.FileData;
import com.estebandev.minicloud.service.utils.FileManagerUtils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Service
@RequiredArgsConstructor
@Setter
public class FileManagerServiceImpl implements FileManagerService {
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

    @Override
    @PreAuthorize("@authF.decide(#pathString, false,#root)")
    public List<FileData> listFiles(String pathString)
            throws FileNotFoundException, FileIsNotDirectoryException, IOException {
        fileLock.readLock().lock();
        try {

            verifyRootDirectory();

            Path root = getRoot();
            Path dir = root.resolve(pathString);

            if (!Files.exists(dir))
                throw new FileNotFoundException();

            if (!Files.isDirectory(dir))
                throw new FileIsNotDirectoryException("The file is not directory");

            return Files.list(dir)
                    .filter(filePath -> !filePath.getFileName().toString().startsWith(".dir"))
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
    @PreAuthorize("@authF.decide(#pathString, false,#root)")
    private void makeDirectory(Path pathString)
            throws FileAlreadyExistsException, IOException {
        fileLock.writeLock().lock();
        try {
            verifyRootDirectory();

            if (Files.exists(pathString))
                throw new FileAlreadyExistsException("The already exist");

            Files.createDirectory(pathString);

            fileMetadataService.make(pathString, userService.getUserFromAuth());
            savePathMetadata(pathString);
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    @Override
    public void makeDirectory(String pathString)
            throws FileAlreadyExistsException, IOException {
        Path dirPath = getRoot().resolve(pathString).normalize();

        makeDirectory(dirPath);
    }

    @Override
    public void makeDirectory(String pathString, String dirName)
            throws FileAlreadyExistsException, IOException {
        makeDirectory(getRoot().resolve(pathString).normalize().resolve(FileManagerUtils.formatName(dirName)));
    }

    @Override
    @PreAuthorize("@authF.decide(#pathString, false,#root)")
    public void uploadFile(String pathString, MultipartFile multipartFile)
            throws IOException, FileIsNotDirectoryException, FileNotFoundException {
        try {
            if (!fileLock.writeLock().tryLock(5, TimeUnit.SECONDS)) {
                throw new IOException("Upload is unable now");
            }

            try {
                verifyRootDirectory();

                Path dirPath = getRoot().resolve(pathString);

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

    @Override
    @Transactional
    @PreAuthorize("@authF.decide(#pathString, true,#root)")
    public Path rename(String pathString, String newName) throws IOException {
        try {
            if (!fileLock.writeLock().tryLock(5, TimeUnit.SECONDS)) {
                throw new IOException("Rename is unable now");
            }

            try {
                Path filePath = getRoot().resolve(pathString);
                Path newFilePath = filePath.getParent().resolve(FileManagerUtils.formatName(newName));

                Files.move(filePath, newFilePath);

                savePathMetadata(newFilePath);
                return getRoot().relativize(newFilePath);
            } finally {
                fileLock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupt to try get lock", e);
        }
    }

    @Override
    @PreAuthorize("@authF.decide(#pathString, false,#root)")
    public Resource findFile(String pathString) throws FileNotFoundException, IOException {
        fileLock.readLock().lock();
        try {
            verifyRootDirectory();

            Path filePath = getRoot().resolve(pathString);

            if (!Files.exists(filePath))
                throw new FileNotFoundException("File not exists");

            if (Files.isDirectory(filePath))
                throw new IOException("The file is a directory");
            if (filePath.getFileName().toString().contains(".dirdata"))
                throw new IOException("The file isn't accesible");

            FileManagerUtils.validateFile(filePath);

            return new FileSystemResource(filePath);

        } finally {
            fileLock.readLock().unlock();
        }
    }

    @Override
    @PreAuthorize("@authF.decide(#pathString, false,#root)")
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

    @Override
    @PreAuthorize("@authF.decide(#pathString, true,#root)")
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
                if (Files.isDirectory(filePath))
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

    @Override
    public Path getRoot() {
        return Path.of(pathString).normalize();
    }

    @Override
    public long getLastModifiedDateInMinutes(String pathString) throws IOException {
        Path filePath = getRoot().resolve(pathString);
        FileTime fTime = Files.getLastModifiedTime(filePath);
        return fTime.to(TimeUnit.MINUTES);
    }

    @Override
    public boolean isExist(String path) {
        return Files.exists(getRoot().resolve(path));
    }

    @Override
    public boolean isValidDirectory(String path) {
        return isExist(path) && Files.isDirectory(getRoot().resolve(path));
    }

    @Override
    public String getMimeType(String filePathString) throws IOException {
        return FileManagerUtils.getMimeType(getRoot().resolve(filePathString));
    }

    @Override
    public void savePathMetadata(Path dirPath) throws IOException {
        if (!Files.isDirectory(dirPath))
            return;

        FileMetadata metadata;
        try {
            metadata = fileMetadataService.findMetadataFromKey(dirPath, "path");
            metadata.setValue(getRoot().relativize(dirPath).toString());
            uploadPathChild(dirPath);
        } catch (NoSuchElementException e) {
            metadata = FileMetadata.builder()
                    .key("path")
                    .value(getRoot().relativize(dirPath).toString())
                    .build();
        }

        fileMetadataService.save(dirPath, metadata);
    }

    @Override
    public void uploadPathChild(Path parentPath) throws IOException {
        if (!Files.isDirectory(parentPath))
            return;

        Files.list(parentPath)
                .filter(p -> Files.isDirectory(p))
                .forEach(t -> {
                    try {
                        savePathMetadata(t);
                    } catch (IOException e) {
                        // Do nothing
                    }
                });
    }
}
