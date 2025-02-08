package com.estebandev.minicloud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.exception.FileNotFoundException;

@SpringBootTest
public class FileManagerServiceTest {

    // @InjectMocks
    @Autowired
    private FileManagerService fileManagerService;

    @BeforeEach
    public void createFile() throws IOException {
        fileManagerService.verifyRootDirectory();
    }

    @AfterEach
    public void deleteAllFiles() throws IOException {
        Files.deleteIfExists(fileManagerService.getRoot());
    }

    @Test
    public void testVerifyFileManager() throws IOException {
        fileManagerService.verifyRootDirectory();
        Path path = fileManagerService.getRoot();

        assertThat(Files.exists(path)).isTrue();
    }
}
