package com.estebandev.minicloud.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.utils.FileData;

public interface FileManagerService {
    List<FileData> listFiles(String path)
            throws FileNotFoundException, FileIsNotDirectoryException, IOException;

    void makeDirectory(String dirPathString)
            throws FileAlreadyExistsException, IOException;

    void makeDirectory(String dirPathString, String dirName)
            throws FileAlreadyExistsException, IOException;

    void uploadFile(String dirPathString, MultipartFile multipartFile)
            throws IOException, FileIsNotDirectoryException, FileNotFoundException;

    Path rename(String pathString, String newName) throws IOException;

    Resource findFile(String pathString) throws FileNotFoundException, IOException;

    FileData findFileData(String pathString) throws FileNotFoundException, IOException;

    public void delete(String pathString) throws FileNotFoundException, IOException;

    Path getRoot();

    long getLastModifiedDateInMinutes(String pathString) throws IOException;

    boolean isExist(String path);

    boolean isValidDirectory(String path);

    String getMimeType(String filePathString) throws IOException;

    void savePathMetadata(Path dirPath) throws IOException;

    void uploadPathChild(Path parentPath) throws IOException;

    void setPathString(String path);
}
