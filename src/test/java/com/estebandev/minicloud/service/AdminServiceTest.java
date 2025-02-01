package com.estebandev.minicloud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.estebandev.minicloud.controller.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.estebandev.minicloud.entity.Scopes;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.repository.UserRepository;
import com.estebandev.minicloud.service.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminService adminService;

    @Test
    void findAllUsers_ShouldReturnAllUsers() {
        // Given
        List<User> expectedUsers = Arrays.asList(new User(), new User());
        when(userRepository.findAll()).thenReturn(expectedUsers);

        // When
        List<User> result = adminService.findAllUsers();

        // Then
        assertEquals(expectedUsers, result);
        verify(userRepository).findAll();
    }

    @Test
    void findAllUsersPage_ValidPage_ShouldReturnPage() throws ServiceException {
        // Given
        int page = 0;
        List<User> users = Arrays.asList(new User(), new User());
        PageRequest pageRequest = PageRequest.of(page, adminService.PAGE_SIZE);
        Page<User> expectedPage = new PageImpl<>(users, pageRequest, users.size());
        
        when(userRepository.findAll(pageRequest)).thenReturn(expectedPage);

        // When
        Page<User> result = adminService.findAllUsersPage(page);

        // Then
        assertEquals(expectedPage, result);
        verify(userRepository).findAll(pageRequest);
    }

    @Test
    void getPageList_CurrentPageNearStart_ShouldGenerateCorrectPages() {
        // Given
        Page<User> userPage = mock(Page.class);
        when(userPage.getNumber()).thenReturn(0); // Current page 0 (1st page)
        when(userPage.getTotalPages()).thenReturn(5);

        // When
        List<Integer> result = adminService.getPageList(userPage);

        // Then
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    void getPageList_CurrentPageNearEnd_ShouldGenerateCorrectPages() {
        // Given
        Page<User> userPage = mock(Page.class);
        when(userPage.getNumber()).thenReturn(4); // Current page 4 (5th page)
        when(userPage.getTotalPages()).thenReturn(5);

        // When
        List<Integer> result = adminService.getPageList(userPage);

        // Then
        assertEquals(List.of(3, 4, 5), result);
    }

    @Test
    void findUser_ShouldReturnUser() {
        // Given
        User expectedUser = new User();
        when(userService.findById(1L)).thenReturn(expectedUser);

        // When
        User result = adminService.findUser(1L);

        // Then
        assertEquals(expectedUser, result);
        verify(userService).findById(1L);
    }

    @Test
    void updateUser_ShouldUpdateAndSave() {
        // Given
        UserDTO dto = new UserDTO();
        dto.setId(1L);
        dto.setEmail("new@email.com");
        dto.setNickname("newnick");

        User user = new User();
        user.setId(1L);
        user.setEmail("old@email.com");
        user.setNickname("oldnick");
        
        when(userService.findById(1L)).thenReturn(user);

        // When
        adminService.updateUser(dto);

        // Then
        assertEquals("new@email.com", user.getEmail());
        assertEquals("newnick", user.getNickname());
        verify(userRepository).save(user);
    }

    @Test
    void addScope_ShouldAddScopeAndSave() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setScopes(new ArrayList<>());
        
        when(userService.findById(1L)).thenReturn(user);

        // When
        adminService.addScope(1L, "ROLE_ADMIN");

        // Then
        assertEquals(1, user.getScopes().size());
        Scopes addedScope = user.getScopes().get(0);
        assertEquals("ROLE_ADMIN", addedScope.getAuthority());
        assertEquals(user, addedScope.getUser());
        verify(userRepository).save(user);
    }

    @Test
    void removeScope_ScopeExists_ShouldRemoveAndSave() throws ServiceException {
        // Given
        User user = new User();
        Scopes scope = Scopes.builder()
                            .id(1L)
                            .authority("ROLE_USER")
                            .user(user)
                            .build();
        user.setScopes(new ArrayList<>(Collections.singletonList(scope)));
        
        when(userService.findById(1L)).thenReturn(user);

        // When
        adminService.removeScope(1L, 1L);

        // Then
        assertTrue(user.getScopes().isEmpty());
        verify(userRepository).save(user);
    }

    @Test
    void removeScope_ScopeNotFound_ShouldThrowException() {
        // Given
        User user = new User();
        user.setScopes(new ArrayList<>());
        
        when(userService.findById(1L)).thenReturn(user);

        // When/Then
        assertThrows(ServiceException.class, () -> 
            adminService.removeScope(1L, 1L)
        );
        verify(userRepository, never()).save(any());
    }
}
