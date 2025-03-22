package com.estebandev.minicloud.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.estebandev.minicloud.service.FileManagerService;
import com.estebandev.minicloud.service.UserService;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.config.CustomErrorController;
import com.estebandev.minicloud.entity.User;

@SpringBootTest
@AutoConfigureMockMvc
public class FileDashboardControllerTest {

    @TempDir
    Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FileManagerService fileManagerService;

    @MockitoBean
    private CustomErrorController customErrorController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileManagerService.setPathString(tempDir.toString());
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = { "FILE_DASHBOARD" })
    public void createIfNotExistPersonalDirectory_ShouldRedirect() throws Exception {
        User mockUser = new User();
        mockUser.setEmail("user@example.com");
        mockUser.setScopes(new ArrayList<>());
        when(userService.getUserFromAuth()).thenReturn(mockUser);
        doNothing().when(fileManagerService).makeDirectory("./user@example.com");
        
        mockMvc.perform(get("/files/action/createIfNotExistPersonalDirectory"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files"));
    }
}
