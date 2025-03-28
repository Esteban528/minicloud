package com.estebandev.minicloud.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.estebandev.minicloud.entity.FileMetadata;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;

public interface FileMetadataService {
    List<FileMetadata> findMetadata(String uuid);

    List<FileMetadata> findMetadata(Path path) throws IOException;

    List<FileMetadata> findMetadataFromKey(String key);

    FileMetadata findMetadataFromKey(String uuid, String key) throws NoSuchElementException;

    List<FileMetadata> findMetadataFromKeyBatch(List<String> uuids, String key);

    FileMetadata findMetadataFromKey(Path path, String key) throws IOException, NoSuchElementException;
    
    List<FileMetadata> findMetadataFromKeyAndValueContains(String key, String valueContains);

    void make(Path dirPath, User owner) throws IOException, FileIsNotDirectoryException;
    
    UUID getUuidFromDir(Path path) throws IOException, FileIsNotDirectoryException;

    void deleteAll(Path path) throws IOException, FileIsNotDirectoryException;

    void save(Path path, FileMetadata fileMetadata) throws IOException;
}
