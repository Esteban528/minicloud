package com.estebandev.minicloud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.entity.UserMetadata;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
class FileSecurityServiceImplTest {
    @TempDir
    private Path tempDir;

    @Mock
    private UserService userService;

    @Mock
    private FileManagerService fileManagerService;

    @Mock
    private FileMetadataService fileMetadataService;

    @InjectMocks
    private FileSecurityServiceImpl fileSecurityService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private UUID randomUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(fileManagerService.getRoot()).thenReturn(tempDir);
    }

    @Test
    void getUserWithAccessTo_Test() throws FileIsNotDirectoryException, IOException {
        String pathString = "estebandev/folder";
        Path path = tempDir.resolve(pathString);
        Files.createDirectories(path);
        User user = User.builder().email("example@minicloud.com").build();
        when(fileMetadataService.getUuidFromDir(path)).thenReturn(randomUuid);
        List<UserMetadata> metadata = List.of(
                UserMetadata.builder().key("ACCESS_TO_" + randomUuid).value("true").user(user).build(),
                UserMetadata.builder().key("lala" + randomUuid).value("false").user(user).build());
        when(userService.findMetadatasByKeySearch(randomUuid.toString())).thenReturn(metadata);

        List<User> userList = fileSecurityService.getUserWithAccessTo(pathString);

        verify(fileMetadataService).getUuidFromDir(path);
        verify(userService).findMetadatasByKeySearch(randomUuid.toString());
        assertThat(userList.size()).isEqualTo(1);
        assertThat(userList.get(0)).isEqualTo(user);
    }

    @Test
    void getUserWithAccessTo_Test_withThrow() throws IOException {
        String pathString = "estebandev/folder";
        Path path = tempDir.resolve(pathString);
        when(fileMetadataService.getUuidFromDir(path)).thenThrow(FileIsNotDirectoryException.class);

        assertThrows(FileIsNotDirectoryException.class, () -> {
            fileSecurityService.getUserWithAccessTo(pathString);
        });

        verify(userService, never()).findMetadatasByKeySearch(randomUuid.toString());
    }

    @Test
    void grantAccessTest_create()
            throws FileIsNotDirectoryException, IOException, UsernameNotFoundException, ServiceException {
        String pathString = "estebandev/folder";
        Path path = tempDir.resolve(pathString);
        Files.createDirectories(path);
        String email = "example@minicloud.com";
        User user = User.builder().email(email).build();
        when(fileMetadataService.getUuidFromDir(path)).thenReturn(randomUuid);
        List<UserMetadata> metadata = new ArrayList<>();
        metadata.add(
                UserMetadata.builder().key("lala" + randomUuid).value("false").user(user).build());
        user.setUserMetadata(metadata);
        when(userService.findMetadatasByKeySearch(randomUuid.toString())).thenReturn(metadata);
        when(userService.getUserFromAuth()).thenReturn(User.builder().email("otherEmail@example.com").build());
        when(userService.findByEmail(email)).thenReturn(user);

        fileSecurityService.grantAccess(pathString, email);

        verify(userService).saveUser(userCaptor.capture());
        User userResult = userCaptor.getValue();
        assertThat(userResult.getEmail()).isEqualTo(email);
        assertThat(user.getUserMetadata().size()).isEqualTo(2);
        UserMetadata metadataUser = user.getUserMetadata().stream()
                .filter(m -> m.getValue().equals("true") && m.getKey().startsWith("ACCESS_TO_"))
                .findFirst().get();

        assertThat(metadataUser.getKey().equals("ACCESS_TO_" + randomUuid));
        assertThat(metadataUser.getValue().equals("true"));
    }

    @Test
    void grantAccessTest_update()
            throws FileIsNotDirectoryException, IOException, UsernameNotFoundException, ServiceException {
        String pathString = "estebandev/folder";
        Path path = tempDir.resolve(pathString);
        Files.createDirectories(path);
        String email = "example@minicloud.com";
        User user = User.builder().email(email).build();
        when(fileMetadataService.getUuidFromDir(path)).thenReturn(randomUuid);
        List<UserMetadata> metadata = new ArrayList<>();
        metadata.add(
                UserMetadata.builder().key("lala" + randomUuid).value("false").user(user).build());
        metadata.add(
                UserMetadata.builder().key("ACCESS_TO_" + randomUuid).value("false").user(user).build());
        user.setUserMetadata(metadata);
        when(userService.findMetadatasByKeySearch(randomUuid.toString())).thenReturn(metadata);
        when(userService.getUserFromAuth()).thenReturn(User.builder().email("otherEmail@example.com").build());
        when(userService.findByEmail(email)).thenReturn(user);

        fileSecurityService.grantAccess(pathString, email);

        verify(userService).saveUser(userCaptor.capture());
        User userResult = userCaptor.getValue();
        assertThat(userResult.getEmail()).isEqualTo(email);
        assertThat(user.getUserMetadata().size()).isEqualTo(2);
        UserMetadata metadataUser = user.getUserMetadata().stream()
                .filter(m -> m.getValue().equals("true") && m.getKey().startsWith("ACCESS_TO_"))
                .findFirst().get();

        assertThat(metadataUser.getKey().equals("ACCESS_TO_" + randomUuid));
        assertThat(metadataUser.getValue().equals("true"));
    }

    @Test
    void grantAccessTest_withFileIsNotDirectory()
            throws FileIsNotDirectoryException, IOException, UsernameNotFoundException, ServiceException {
        String pathString = "estebandev/folder";
        Path path = tempDir.resolve(pathString);
        Files.createDirectories(path);
        String email = "example@minicloud.com";
        when(fileMetadataService.getUuidFromDir(path)).thenThrow(FileIsNotDirectoryException.class);
        when(userService.getUserFromAuth()).thenReturn(User.builder().email("otherEmail@example.com").build());
        assertThrows(FileIsNotDirectoryException.class, () -> fileSecurityService.grantAccess(pathString, email));

        verify(userService, never()).saveUser(any(User.class));
    }

    @Test
    void grantAccessTest_with_sameUser()
            throws IOException {
        String pathString = "estebandev/folder";
        Path path = tempDir.resolve(pathString);
        Files.createDirectories(path);
        String email = "example@minicloud.com";
        when(userService.getUserFromAuth()).thenReturn(User.builder().email(email).build());
        assertThrows(ServiceException.class, () -> fileSecurityService.grantAccess(pathString, email));

        verify(userService, never()).saveUser(any(User.class));
    }

    @Test
    void grantAccessTest_with_UserAlreadyHasAccess()
            throws IOException {
        String pathString = "estebandev/folder";
        Path path = tempDir.resolve(pathString);
        Files.createDirectories(path);
        String email = "example@minicloud.com";
        User user = User.builder().email(email).build();
        List<UserMetadata> metadata = List.of(
                UserMetadata.builder().key("ACCESS_TO_" + randomUuid).value("true").user(user).build(),
                UserMetadata.builder().key("lala" + randomUuid).value("false").user(user).build());
        when(userService.findMetadatasByKeySearch(randomUuid.toString())).thenReturn(metadata);
        when(fileMetadataService.getUuidFromDir(path)).thenReturn(randomUuid);
        when(userService.getUserFromAuth()).thenReturn(User.builder().email("asdasd@asdasda.asdasd").build());

        assertThrows(ServiceException.class, () -> fileSecurityService.grantAccess(pathString, email));

        verify(userService, never()).saveUser(any(User.class));
    }

    @Test
    void revokeAccess()
            throws IOException, ServiceException {
        String pathString = "estebandev/folder";
        Path path = tempDir.resolve(pathString);
        Files.createDirectories(path);
        String email = "example@minicloud.com";
        User user = User.builder().email(email).build();
        when(fileMetadataService.getUuidFromDir(path)).thenReturn(randomUuid);
        List<UserMetadata> metadata = new ArrayList<>();
        UserMetadata userMetadata = UserMetadata.builder().key("ACCESS_TO_" + randomUuid).value("true").user(user)
                .build();
        metadata.add(userMetadata);
        user.setUserMetadata(metadata);
        when(userService.findMetadatasByKeySearch(randomUuid.toString())).thenReturn(metadata);
        when(userService.findMetadatasByKeySearch(user, "ACCESS_TO_" + randomUuid))
                .thenReturn(Optional.of(userMetadata));
        when(userService.getUserFromAuth()).thenReturn(User.builder().email("otherEmail@example.com").build());
        when(userService.findByEmail(email)).thenReturn(user);

        fileSecurityService.revokeAccess(pathString, email);

        verify(userService).saveUser(userCaptor.capture());
        User userResult = userCaptor.getValue();
        assertThat(userResult.getEmail()).isEqualTo(email);
        assertThat(user.getUserMetadata().size()).isEqualTo(1);
        UserMetadata metadataUser = user.getUserMetadata().stream()
                .filter(m -> m.getKey().startsWith("ACCESS_TO_"))
                .findFirst().get();

        assertThat(metadataUser.getKey().equals("ACCESS_TO_" + randomUuid));
        assertThat(metadataUser.getValue().equals("false"));
    }

    @Test
    void revokeAccess_tryRemoverYourself()
            throws IOException, ServiceException {
        String pathString = "estebandev/folder";
        Path path = tempDir.resolve(pathString);
        Files.createDirectories(path);
        String email = "example@minicloud.com";
        when(userService.getUserFromAuth()).thenReturn(User.builder().email(email).build());

        assertThrows(ServiceException.class, () -> fileSecurityService.revokeAccess(pathString, email));

        verify(userService, never()).saveUser(userCaptor.capture());
    }

    @Test
    void revokeAccess_theUserHasNotAccess()
            throws IOException, ServiceException {
        String pathString = "estebandev/folder";
        Path path = tempDir.resolve(pathString);
        Files.createDirectories(path);
        String email = "example@minicloud.com";
        User user = User.builder().email(email).build();
        when(fileMetadataService.getUuidFromDir(path)).thenReturn(randomUuid);
        List<UserMetadata> metadata = new ArrayList<>();
        UserMetadata userMetadata = UserMetadata.builder().key("ACCESS_TO_" + randomUuid).value("false").user(user)
                .build();
        metadata.add(userMetadata);
        user.setUserMetadata(metadata);
        when(userService.findMetadatasByKeySearch(randomUuid.toString())).thenReturn(metadata);
        when(userService.getUserFromAuth()).thenReturn(User.builder().email("otherEmail@example.com").build());

        assertThrows(ServiceException.class, () -> fileSecurityService.revokeAccess(pathString, email));

        verify(userService, never()).saveUser(userCaptor.capture());
    }
}
