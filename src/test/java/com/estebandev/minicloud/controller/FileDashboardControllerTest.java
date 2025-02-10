package com.estebandev.minicloud.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.estebandev.minicloud.service.FileManagerService;
import com.estebandev.minicloud.service.UserService;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.entity.User;

@SpringBootTest
@AutoConfigureMockMvc
public class FileDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FileManagerService fileManagerService;

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void createIfNotExistPersonalDirectory_ShouldRedirect() throws Exception {
        User mockUser = new User();
        mockUser.setEmail("user@example.com");
        when(userService.getUserFromAuth()).thenReturn(mockUser);
        doNothing().when(fileManagerService).makeDirectory("./user@example.com");
        
        mockMvc.perform(get("/files/action/createIfNotExistPersonalDirectory"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files"));
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void createIfNotExistPersonalDirectory_WhenAlreadyExists_ShouldRedirectToMyDir() throws Exception {
        when(userService.getUserFromAuth()).thenReturn(User.builder().email("user@example.com").build());
        doThrow(new FileAlreadyExistsException("File exists")).when(fileManagerService).makeDirectory("./user@example.com");
        
        mockMvc.perform(get("/files/action/createIfNotExistPersonalDirectory"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files/action/go/mydir"));
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void goToDir_WhenNotDirectory_ShouldRedirectToFile() throws Exception {
        when(fileManagerService.listFiles(anyString())).thenThrow(new FileIsNotDirectoryException("Not a directory"));
        when(userService.getUserFromAuth()).thenReturn(User.builder().email("user@example.com").build());
        
        mockMvc.perform(get("/files/action/go/dir").param("path", "somefile.txt"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files/action/go/file?path=somefile.txt"));
    }
}
