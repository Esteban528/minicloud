package com.estebandev.minicloud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.web.multipart.MultipartFile;

import com.estebandev.minicloud.entity.FileMetadata;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.utils.FileData;
import com.sun.jdi.connect.Connector.Argument;

public class FileManagerServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock
    private UserService userService;

    @Mock
    private FileMetadataService fileMetadataService;

    @InjectMocks
    private FileManagerServiceImpl fileManagerService;

    @Captor
    ArgumentCaptor<FileMetadata> fileMdCaptor;

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
        User user = User.builder().build();
        when(userService.getUserFromAuth()).thenReturn(user);
        FileMetadata fileMetadata = mock(FileMetadata.class);
        when(fileMetadataService.findMetadataFromKey(newDir, "path")).thenReturn(fileMetadata);

        fileManagerService.makeDirectory("newDir");

        assertTrue(Files.exists(newDir));
        verify(fileMetadataService).make(newDir, user);
        verify(fileMetadataService).save(eq(newDir), any(FileMetadata.class));
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
        User user = User.builder().build();
        when(userService.getUserFromAuth()).thenReturn(user);

        Files.createDirectory(dirPath);
        fileManagerService.uploadFile("uploads", multipartFile);

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
    void testDeleteDirectory_Success() throws IOException {
        Path filePath = tempDir.resolve("deleteMe");
        Files.createDirectory(filePath);
        fileManagerService.delete("deleteMe");
        assertFalse(Files.exists(filePath));
        verify(fileMetadataService).deleteAll(filePath);
    }

    @Test
    void testRenameFile_Success() throws IOException {
        Path filePath = tempDir.resolve("oldName.txt");
        Files.createFile(filePath);
        fileManagerService.rename("oldName.txt", "newName.txt");
        assertFalse(Files.exists(filePath));
        assertTrue(Files.exists(tempDir.resolve("newName.txt")));
    }

    @Test
    void savePathMetadata_create()
            throws IOException, NotFoundException {
        when(fileMetadataService.findMetadataFromKey(any(Path.class), anyString()))
                .thenThrow(new NoSuchElementException());
        Path path = fileManagerService.getRoot().resolve("testPathMd");
        Files.createDirectory(path);

        fileManagerService.savePathMetadata(path);

        verify(fileMetadataService).save(eq(path), fileMdCaptor.capture());
        FileMetadata fm = fileMdCaptor.getValue();
        assertThat(fm.getKey()).isEqualTo("path");
        assertThat(fm.getValue()).isEqualTo(fileManagerService.getRoot().relativize(path).toString());
    }

    @Test
    void savePathMetadata_update()
            throws IOException, NotFoundException {
        Path path = fileManagerService.getRoot().resolve("testPathMd");
        FileMetadata fileMetadata = mock(FileMetadata.class);
        when(fileMetadataService.findMetadataFromKey(path, "path")).thenReturn(fileMetadata);
        Files.createDirectory(path);

        fileManagerService.savePathMetadata(path);

        verify(fileMetadata).setValue(fileManagerService.getRoot().relativize(path).toString());
        verify(fileMetadataService).save(path, fileMetadata);
    }
}
