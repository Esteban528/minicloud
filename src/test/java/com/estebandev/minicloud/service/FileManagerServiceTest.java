package com.estebandev.minicloud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.utils.FileData;
import com.estebandev.minicloud.service.utils.FileManagerUtils;

class FileManagerServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private UserService userService;

    @InjectMocks
    private FileManagerService fileManagerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileManagerService.setPathString(tempDir.toString());
    }

    @Test
    void testVerifyRootDirectory() {
        fileManagerService.verifyRootDirectory();
        assertTrue(Files.exists(tempDir));
    }

    @Test
    void testMakeDirectory_Success() throws IOException {
        Path newDir = tempDir.resolve("newDir");
        fileManagerService.makeDirectory("newDir");
        assertTrue(Files.exists(newDir));
    }

    @Test
    void testMakeDirectory_AlreadyExists() throws IOException {
        Path existingDir = tempDir.resolve("existingDir");
        Files.createDirectory(existingDir);
        assertThrows(FileAlreadyExistsException.class, () -> fileManagerService.makeDirectory("existingDir"));
    }

    @Test
    void testUploadFile_Success() throws IOException, FileIsNotDirectoryException, FileNotFoundException {
        MultipartFile multipartFile = mock(MultipartFile.class);
        Path dirPath = tempDir.resolve("uploads");
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        Files.createDirectory(dirPath);
        fileManagerService.uploadFile(multipartFile, "uploads");
        verify(multipartFile).transferTo(any(Path.class));
    }

    @Test
    void testFindFile_Success() throws IOException {
        Path filePath = tempDir.resolve("file.txt");
        Files.createFile(filePath);
        Resource resource = fileManagerService.findFile("file.txt");
        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void testFindFile_NotFound() {
        assertThrows(FileNotFoundException.class, () -> fileManagerService.findFile("missing.txt"));
    }

    @Test
    void testListFiles_Success() throws IOException, FileIsNotDirectoryException {
        Path dir = tempDir.resolve("testDir");
        Files.createDirectory(dir);
        Files.createFile(dir.resolve("file1.txt"));
        Files.createFile(dir.resolve("file2.txt"));
        List<FileData> files = fileManagerService.listFiles("testDir");
        assertEquals(2, files.size());
    }

    @Test
    void testDeleteFile_Success() throws IOException {
        Path filePath = tempDir.resolve("deleteMe.txt");
        Files.createFile(filePath);
        fileManagerService.delete("deleteMe.txt");
        assertFalse(Files.exists(filePath));
    }

    @Test
    void testRenameFile_Success() throws IOException {
        Path filePath = tempDir.resolve("oldName.txt");
        Files.createFile(filePath);
        fileManagerService.rename("oldName.txt", "newName.txt");
        assertFalse(Files.exists(filePath));
        assertTrue(Files.exists(tempDir.resolve("newName.txt")));
    }
}
