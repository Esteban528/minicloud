package com.estebandev.minicloud.controller;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriComponentsBuilder;

import com.estebandev.minicloud.service.AdminService;
import com.estebandev.minicloud.service.CodeAuthServiceTest;
import com.estebandev.minicloud.service.FileManagerService;
import com.estebandev.minicloud.service.FileSecurityService;
import com.estebandev.minicloud.service.FileSecurityServiceImpl;
import com.estebandev.minicloud.service.UserService;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.utils.FileData;
import com.estebandev.minicloud.entity.User;

@SpringBootTest
@AutoConfigureMockMvc
public class FileManagerControllerTest {
    @TempDir
    Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    protected UserService userService;

    @MockitoBean
    protected FileManagerService fileManagerService;

    @MockitoBean
    protected FileSecurityServiceImpl fileSecurityService;

    // @MockitoBean
    // protected AdminService adminService;
    //
    // @MockitoBean
    // protected CodeAuthServiceTest codeAuthService;

    private User user = User.builder()
            .email("user@example.com")
            .scopes(new ArrayList<>())
            .build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileManagerService.setPathString(tempDir.toString());
        when(fileManagerService.getRoot()).thenReturn(tempDir);
        when(userService.getUserFromAuth()).thenReturn(user);
    }

    // Existing tests
    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void createIfNotExistPersonalDirectory_WhenAlreadyExists_ShouldRedirectToMyDir() throws Exception {
        doThrow(new FileAlreadyExistsException("File exists")).when(fileManagerService)
                .makeDirectory("./user@example.com");

        mockMvc.perform(get("/files/action/createIfNotExistPersonalDirectory"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files/action/go/mydir"));
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void actionGoToMyDir_success() throws Exception {
        mockMvc.perform(get("/files/action/go/mydir"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files/action/go/dir?path=user%40example.com"));
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void actionGoToMyShortcuts_success() throws Exception {
        User user = User.builder().email("user@example.com").build();
        when(userService.getUserAllDataFromAuth()).thenReturn(user);
        mockMvc.perform(get("/files/action/go/myshortcuts"))
                .andExpect(status().isOk());
        verify(fileSecurityService).getFileListUserHasAccess(user);
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void goToDir_onlyOwnsDirectories() throws Exception {
        mockMvc.perform(get("/files/action/go/dir").param("path", user.getEmail()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/files/action/go/dir")
                .param("path", Path.of(user.getEmail()).resolve("another_directory").toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void goToDir_onlyOwnsDirectories_fileIsNotDirectory() throws Exception {
        when(fileManagerService.listFiles(user.getEmail())).thenThrow(FileIsNotDirectoryException.class);
        mockMvc.perform(get("/files/action/go/dir").param("path", user.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files/action/go/file?path=user%40example.com"));
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void goToDir_otherDirWithoutPerms() throws Exception {
        mockMvc.perform(get("/files/action/go/dir").param("path", "otherDirectory"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files/error?msg=Acces%20denied"));
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD", "ADMIN_DASHBOARD" })
    public void goToDir_withAdminPerms() throws Exception {
        mockMvc.perform(get("/files/action/go/dir").param("path", "otherDirectory"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void goToFile_withPerms() throws Exception {
        String pathString = user.getEmail() + "/text.txt";
        when(fileManagerService.findFileData(pathString)).thenReturn(
                FileData.builder()
                        .fileName("text.txt")
                        .path(tempDir.resolve(pathString))
                        .mediaType("text/txt")
                        .size(22.00)
                        .directory(false)
                        .editable(true)
                        .build());
        mockMvc.perform(get("/files/action/go/file").param("path", pathString))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void goToFile_withPerms_FileDoesNotExist() throws Exception {
        String pathString = user.getEmail() + "/text.txt";
        when(fileManagerService.findFileData(pathString)).thenThrow(FileNotFoundException.class);
        mockMvc.perform(get("/files/action/go/file").param("path", pathString))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files/action/go/dir?path=user%40example.com"));
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void read_successfull() throws Exception {
        String pathString = user.getEmail() + "/text.txt";
        Files.createDirectory(tempDir.resolve(user.getEmail()));
        Files.createFile(tempDir.resolve(pathString));

        Resource resource = new FileSystemResource(tempDir.resolve(pathString));
        when(fileManagerService.findFile(pathString)).thenReturn(resource);
        when(fileManagerService.getMimeType(pathString)).thenReturn("text/txt");

        mockMvc.perform(get("/files/action/read").param("path", pathString))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().contentType("text/txt"));
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void read_FileNotFound() throws Exception {
        String pathString = user.getEmail() + "/text.txt";
        when(fileManagerService.findFile(pathString)).thenThrow(FileNotFoundException.class);
        mockMvc.perform(get("/files/action/read").param("path", pathString))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files/error"));
    }

    // New test cases
    @Nested
    class UploadTests {
        @Test
        @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
        void uploadFilePost_success() throws Exception {
            String pathString = user.getEmail() + "/docs";
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "content".getBytes());

            mockMvc.perform(MockMvcRequestBuilders.multipart("/files/action/upload")
                    .file(file)
                    .param("path", pathString)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/files/action/go/dir?path=" + encodePath(pathString)));

            verify(fileManagerService).uploadFile(eq(pathString), any());
        }

        @Test
        @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
        void uploadFilePost_directoryNotFound() throws Exception {
            String pathString = "invalid/path";
            doThrow(new FileNotFoundException()).when(fileManagerService).uploadFile(eq(pathString), any());

            mockMvc.perform(MockMvcRequestBuilders.multipart("/files/action/upload")
                    .file(new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes()))
                    .param("path", pathString)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/files/error?msg=Acces%20denied"));
        }
    }

    @Nested
    class DirectoryOperationTests {
        @Test
        @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
        void mkdir_success() throws Exception {
            String parentPath = user.getEmail();
            String dirName = "new-dir";

            mockMvc.perform(post("/files/action/mkdir")
                    .param("name", dirName)
                    .param("path", parentPath)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/files/action/go/dir?path=" + encodePath(parentPath)));

            verify(fileManagerService).makeDirectory(eq(parentPath), eq(dirName));
        }

        @Test
        @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
        void delete_success() throws Exception {
            String pathString = user.getEmail() + "/file.txt";

            mockMvc.perform(post("/files/action/delete")
                    .param("path", pathString)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/files/action/go/dir*"));

            verify(fileManagerService).delete(eq(pathString));
        }
    }

    @Nested
    class FileOperationTests {
        @Test
        @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
        void download_success() throws Exception {
            String pathString = user.getEmail() + "/text.txt";
            Files.createDirectory(tempDir.resolve(user.getEmail()));
            Files.createFile(tempDir.resolve(pathString));

            Resource resource = new FileSystemResource(tempDir.resolve(pathString));
            when(fileManagerService.findFile(pathString)).thenReturn(resource);
            when(fileManagerService.getMimeType(pathString)).thenReturn("text/txt");

            mockMvc.perform(get("/files/action/download")
                    .param("path", pathString))
                    .andExpect(status().isOk())
                    .andDo(print())
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=text.txt"));
        }

        @Test
        @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
        void rename_success() throws Exception {
            String pathString = user.getEmail() + "/old.txt";
            String newName = "new.txt";
            Path newPath = Path.of(user.getEmail(), newName);

            when(fileManagerService.rename(eq(pathString), eq(newName))).thenReturn(newPath);

            mockMvc.perform(post("/files/action/rename")
                    .param("path", pathString)
                    .param("newName", newName)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/files/action/go/dir?path=" + encodePath(newPath.toString())));
        }
    }

    @Nested
    class AccessManagementTests {
        @Test
        @WithMockUser(username = "admin@example.com", authorities = { "ADMIN_DASHBOARD", "FILE_DASHBOARD" })
        void manageAccess_success() throws Exception {
            String pathString = "shared-dir";

            when(fileSecurityService.getUserWithAccessTo(pathString))
                    .thenReturn(Collections.singletonList(User.builder().email("user2@example.com").build()));

            mockMvc.perform(get("/files/action/manage/access")
                    .param("path", pathString))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("onboardMaccess"))
                    .andExpect(model().attributeExists("usersWithAccess"));
        }

        @Test
        @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
        void grantAccess_userNotFound() throws Exception {
            String pathString = user.getEmail();

            doThrow(UsernameNotFoundException.class).when(fileSecurityService).grantAccess(pathString,
                    "invalid@user.com");

            mockMvc.perform(post("/files/action/manage/access/grant")
                    .param("path", pathString)
                    .param("email", "invalid@user.com")
                    .with(csrf()))
                    .andExpect(model().attributeExists("error"));
        }
    }

    private String encodePath(String path) throws Exception {
        return URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
    }
}
