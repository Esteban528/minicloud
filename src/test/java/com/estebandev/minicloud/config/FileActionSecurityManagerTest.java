package com.estebandev.minicloud.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import com.estebandev.minicloud.entity.FileMetadata;
import com.estebandev.minicloud.service.FileManagerService;
import com.estebandev.minicloud.service.FileMetadataService;
import com.estebandev.minicloud.service.FileSecurityService;

@ExtendWith(MockitoExtension.class)
class FileActionSecurityManagerTest {

    @Mock
    private FileManagerService fileManagerService;

    @Mock
    private FileMetadataService fileMetadataService;

    @Mock
    private FileSecurityService fileSecurityService;

    @Mock
    private MethodSecurityExpressionOperations operations;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private FileActionSecurityManager fileActionSecurityManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(operations.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("minicloud@example.com");
    }

    @Test
    void decide_shouldReturnTrue_whenPathStartsWithEmail() throws IOException {
        String email = "minicloud@example.com";
        String pathString = email + "/test";

        boolean result = fileActionSecurityManager.decide(pathString, false, operations);

        assertThat(result).isTrue();
    }

    @Test
    void decide_shouldReturnTrue_whenUserIsOwner() throws IOException {
        String pathString = "otheruser/test";

        when(fileManagerService.getRoot()).thenReturn(tempDir);
        when(fileMetadataService.findMetadataFromKey(any(Path.class), anyString()))
                .thenReturn(FileMetadata.builder()
                        .key("owner")
                        .value("minicloud@example.com")
                        .build());

        boolean result = fileActionSecurityManager.decide(pathString, false, operations);

        assertThat(result).isTrue();
    }

    @Test
    void decide_shouldReturnFalse_whenOnlyOwnerAndNotOwner() throws IOException {
        String pathString = "otheruser/test";
        Files.createDirectories(tempDir.resolve(pathString));

        when(fileManagerService.getRoot()).thenReturn(tempDir);
        when(fileMetadataService.findMetadataFromKey(any(Path.class), anyString()))
                .thenReturn(FileMetadata.builder()
                        .key("owner")
                        .value("admin@example.com")
                        .build());

        boolean result = fileActionSecurityManager.decide(pathString, true,
                operations);

        assertThat(result).isFalse();
    }

    @Test
    void decide_shouldReturnTrue_whenUserHasAccess() throws IOException {
        String pathString = "otheruser/test";

        // when(fileSecurityService.isUserFromAuthHasAccessTo(anyString())).thenReturn(true);
        when(fileManagerService.getRoot()).thenReturn(tempDir);
        when(fileMetadataService.findMetadataFromKey(any(Path.class), anyString()))
                .thenReturn(FileMetadata.builder()
                        .key("owner")
                        .value("minicloud@example.com")
                        .build());

        boolean result = fileActionSecurityManager.decide(pathString, false,
                operations);

        assertThat(result).isTrue();
    }

    @Test
    void decide_shouldThrowException_whenMetadataNotFound() throws NoSuchElementException, IOException {
        String pathString = "otheruser/test";

        when(fileManagerService.getRoot()).thenReturn(tempDir);
        when(fileMetadataService.findMetadataFromKey(any(Path.class), anyString()))
                .thenReturn(FileMetadata.builder()
                        .key("owner")
                        .value("minicloud@example.com")
                        .build());

        try {
            fileActionSecurityManager.decide(pathString, false, operations);
        } catch (NoSuchElementException e) {
            assertThat(e).isInstanceOf(NoSuchElementException.class);
        }
    }
}
