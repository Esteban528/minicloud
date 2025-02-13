package com.estebandev.minicloud.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import com.estebandev.minicloud.entity.FileMetadata;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;

public interface FileMetadataService {
    List<FileMetadata> findMetadata(String uuid);

    List<FileMetadata> findMetadata(Path path) throws IOException;

    void make(Path dirPath, User owner) throws IOException, FileIsNotDirectoryException;
    
    UUID getUuidFromDir(Path path) throws IOException, FileIsNotDirectoryException;

    void deleteAll(Path path) throws IOException, FileIsNotDirectoryException;

    void save(Path path, FileMetadata fileMetadata) throws IOException;
}
