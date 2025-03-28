package com.estebandev.minicloud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import com.estebandev.minicloud.entity.FileMetadata;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.repository.FileMetadataRepository;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileMetadataServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FileMetadataServiceImpl fileMetadataService;

    @Captor
    private ArgumentCaptor<FileMetadata> metadataCaptor;

    private User testUser;
    private Path testDirectory;
    private Path testFile;

    @BeforeEach
    void setUp() throws IOException {
        testUser = User.builder().email("test@example.com").build();
        testDirectory = tempDir.resolve("test-dir");
        Files.createDirectory(testDirectory);
        testFile = tempDir.resolve("test-file.txt");
        Files.createFile(testFile);
    }

    @Test
    void make_ShouldCreateMetadataFileAndSaveEntry_WhenValidDirectory() throws Exception {
        // When
        fileMetadataService.make(testDirectory, testUser);

        // Then
        // Verify metadata file creation
        Path metadataFile = testDirectory.resolve(".dirdata.xml");
        assertThat(metadataFile).exists();

        // Verify repository save
        verify(fileMetadataRepository).save(metadataCaptor.capture());
        FileMetadata savedMetadata = metadataCaptor.getValue();
        assertThat(savedMetadata.getKey()).isEqualTo("owner");
        assertThat(savedMetadata.getValue()).isEqualTo(testUser.getEmail());
        assertThat(savedMetadata.getUuid()).isNotNull();
    }

    @Test
    void make_ShouldThrowException_WhenPathDoesNotExist() {
        // Given
        Path nonExistentPath = tempDir.resolve("non-existent");

        // When/Then
        assertThatThrownBy(() -> fileMetadataService.make(nonExistentPath, testUser))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("File does not exist");
    }

    @Test
    void make_ShouldThrowException_WhenPathIsNotDirectory() {
        // When/Then
        assertThatThrownBy(() -> fileMetadataService.make(testFile, testUser))
                .isInstanceOf(FileIsNotDirectoryException.class)
                .hasMessageContaining("File is not directory");
    }

    @Test
    void deleteAll_ShouldRemoveMetadataAndFile_WhenValidDirectory() throws Exception {
        // Given
        fileMetadataService.make(testDirectory, testUser);
        String uuid = fileMetadataService.getUuidFromDir(testDirectory).toString();

        // When
        fileMetadataService.deleteAll(testDirectory);

        // Then
        verify(fileMetadataRepository).deleteByUuid(uuid);
        assertThat(testDirectory.resolve(".dirdata.xml")).doesNotExist();
    }

    @Test
    void getUuidFromDir_ShouldReturnValidUuid_WhenMetadataExists() throws Exception {
        // Given
        fileMetadataService.make(testDirectory, testUser);

        // When
        UUID uuid = fileMetadataService.getUuidFromDir(testDirectory);

        // Then
        assertThat(uuid).isNotNull();
        assertThat(uuid.toString()).isNotEmpty();
    }

    @Test
    void save_ShouldSetUuidFromDirectory_WhenValidPath() throws Exception {
        // Given
        fileMetadataService.make(testDirectory, testUser);
        Mockito.reset(fileMetadataRepository);
        FileMetadata metadata = FileMetadata.builder()
                .key("test-key")
                .value("test-value")
                .build();

        // When
        fileMetadataService.save(testDirectory, metadata);

        // Then
        verify(fileMetadataRepository).save(metadataCaptor.capture());
        FileMetadata savedMetadata = metadataCaptor.getValue();
        assertThat(savedMetadata.getUuid()).isEqualTo(
                fileMetadataService.getUuidFromDir(testDirectory).toString());
    }

    @Test
    void generateMetadataFile_ShouldCreateValidXmlFile() throws Exception {
        // Given
        String testUuid = UUID.randomUUID().toString();
        Path targetDir = Files.createDirectory(tempDir.resolve("metadata-test"));

        // When
        fileMetadataService.generateMetadataFile(targetDir, testUuid);

        // Then
        Path metadataFile = targetDir.resolve(".dirdata.xml");
        assertThat(metadataFile).exists();

        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(metadataFile)) {
            props.loadFromXML(is);
        }
        assertThat(props.getProperty("uuid")).isEqualTo(testUuid);
    }

    @Test
    void findMetadataFromKeyWithoutPathTest_SuccessFull() {
        String key = "testKey";
        List<FileMetadata> fileMetadataOptional = List.of(FileMetadata.builder().build());
        when(fileMetadataRepository.findByKey(key)).thenReturn(fileMetadataOptional);

        fileMetadataService.findMetadataFromKey(key);

        verify(fileMetadataRepository).findByKey(key);
    }

    @Test
    void findMetadataFromKeyTest_SuccessFull() {
        String uuid = UUID.randomUUID().toString();
        String key = "testKey";
        Optional<FileMetadata> fileMetadataOptional = Optional.of(FileMetadata.builder().build());
        when(fileMetadataRepository.findByUuidAndKey(uuid, key)).thenReturn(fileMetadataOptional);

        fileMetadataService.findMetadataFromKey(uuid, key);

        verify(fileMetadataRepository).findByUuidAndKey(uuid, key);
    }

    @Test
    void findMetadataFromKeyTest_NoSuchElementException() {
        String uuid = UUID.randomUUID().toString();
        String key = "testKey";

        assertThrows(NoSuchElementException.class, () -> {
            fileMetadataService.findMetadataFromKey(uuid, key);
        });

        verify(fileMetadataRepository).findByUuidAndKey(uuid, key);
    }

    @Test
    void findMetadataFromKeyBatchTest_SuccessFull() {
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        String key = "testKey";
        List<String> uuids = List.of(uuid1, uuid2);

        fileMetadataService.findMetadataFromKeyBatch(uuids, key);

        verify(fileMetadataRepository).findByUuidsAndKey(uuids, key);
    }

    @Test
    void findMetadataFromKeyAndValueContainsTest() throws NoSuchElementException, IOException {
        String key = "testKey";
        String contains = "sdfas";
        Path path = testDirectory;
        fileMetadataService.make(path, testUser);
        List<FileMetadata> fileMetadataOptional = List.of(FileMetadata.builder().build());
        when(fileMetadataRepository.findByKeyAndValueContaining(eq(key), eq(contains)))
                .thenReturn(fileMetadataOptional);

        fileMetadataService.findMetadataFromKeyAndValueContains(key, contains);

        verify(fileMetadataRepository).findByKeyAndValueContaining(eq(key), eq(contains));
    }

    // @Test
    // void findMetadataFromKeyAndValueContainsTest_IOException() {
    // String key = "testKey";
    // String contains = "sdfas";
    //
    // assertThrows(IOException.class, () -> {
    // fileMetadataService.findMetadataFromKeyAndValueContains(key, contains);
    // });
    //
    // verify(fileMetadataRepository, never()).findByKeyAndValueContaining(eq(key),
    // eq(contains));
    // }

    String generateMetadata(Path path) throws IOException {
        String uuid = UUID.randomUUID().toString();
        fileMetadataService.generateMetadataFile(path, uuid);
        return uuid;
    }
}
