package com.estebandev.minicloud.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estebandev.minicloud.entity.FileMetadata;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.repository.FileMetadataRepository;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Getter
public class FileMetadataServiceImpl implements FileMetadataService{

    private final FileMetadataRepository fileMetadataRepository;
    private String dirMetadataName = ".dirdata.xml";

    @Override
    public List<FileMetadata> findMetadata(String uuid) {
        return fileMetadataRepository.findByUuid(uuid);
    }

	@Override
	public List<FileMetadata> findMetadata(Path path) throws IOException {
        return findMetadata(getUuidFromDir(path).toString());
	}

    /**
     * This method may be executed after to create directory
     * 
     * @throws IOException
     */
    @Override
    public void make(Path dirPath, User owner) throws IOException, FileIsNotDirectoryException {
        if (!Files.exists(dirPath))
            throw new FileNotFoundException("File does not exist");
        if (!Files.isDirectory(dirPath))
            throw new FileIsNotDirectoryException("File is not directory");

        UUID uuid = generateUuid();
        FileMetadata fileMetadata = FileMetadata.builder()
                .uuid(uuid.toString())
                .key("owner")
                .value(owner.getEmail())
                .build();

        generateMetadataFile(dirPath, uuid.toString());
        save(fileMetadata);
    }

    @Override
    @Transactional
    public void deleteAll(Path path) throws InvalidPropertiesFormatException, IOException {
        if (!Files.exists(path))
            throw new FileNotFoundException("File does not exist");
        if (!Files.isDirectory(path))
            throw new FileIsNotDirectoryException("File is not directory");

        Path filePath = getMetadataPathFromDir(path);
        String uuid = getUuidFromDir(path).toString();
        deleteAll(uuid);
        Files.delete(filePath);
    }

    @Override
    public UUID getUuidFromDir(Path path) throws InvalidPropertiesFormatException, IOException, FileIsNotDirectoryException{
        if (!Files.isDirectory(path))
            throw new FileIsNotDirectoryException("File is not directory");

        String uuid;
		uuid = getPropertiesFromDir(path).getProperty("uuid");
        return UUID.fromString(uuid);
    }

	@Override
	public void save(Path path, FileMetadata fileMetadata) throws IOException {
        if (!Files.exists(path))
            throw new FileNotFoundException("File does not exist");
        if (!Files.isDirectory(path))
            throw new FileIsNotDirectoryException("File is not directory");

        String uuid = getUuidFromDir(path).toString();
        fileMetadata.setUuid(uuid);
        save(fileMetadata);
	}

    public Properties getPropertiesFromDir(Path path) throws InvalidPropertiesFormatException, IOException {
        Path metadataPath = getMetadataPathFromDir(path);

        try (InputStream stream = new FileInputStream(metadataPath.toFile())) {
            Properties properties = new Properties();
            properties.loadFromXML(stream);
            return properties;
        }
    }

    private Path getMetadataPathFromDir(Path path) {
        return path.resolve(getDirMetadataName());
    }

    public void save(FileMetadata fileMetadata) {
        fileMetadataRepository.save(fileMetadata);
    }

    @Transactional
    public void deleteAll(String uuid) {
        fileMetadataRepository.deleteByUuid(uuid);
    }

    private UUID generateUuid() {
        return UUID.randomUUID();
    }

    public void generateMetadataFile(Path path, String uuid) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("uuid", uuid);

        Path metadataPath = path.resolve(dirMetadataName);
        Files.createFile(metadataPath);

        try (OutputStream fileOutputStream = new FileOutputStream(metadataPath.toFile())) {
            properties.storeToXML(fileOutputStream, null);
        }
    }
}
