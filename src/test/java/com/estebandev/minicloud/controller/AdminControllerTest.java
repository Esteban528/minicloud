package com.estebandev.minicloud.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.estebandev.minicloud.controller.dto.UserDTO;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.service.AdminService;
import com.estebandev.minicloud.service.exception.ServiceException;

@AutoConfigureMockMvc
@SpringBootTest
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    public void authorizationTest_userHasNotScope() throws Exception {
        mockMvc.perform(
                get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = { "USER" }, authorities = { "ADMIN_DASHBOARD" })
    public void authorizationTest_userHasScope() throws Exception {
        mockMvc.perform(
                get("/admin/dashboard")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_dashboard"));
    }

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    public void showUserManagement_unauthorizedUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", authorities = { "ADMIN_DASHBOARD" })
    public void showUserManagement_authorizedUser_returnsOk() throws Exception {
        @SuppressWarnings("unchecked")
        Page<User> mockPage = mock(Page.class);
        when(mockPage.getTotalPages()).thenReturn(5);
        when(mockPage.getContent()).thenReturn(Arrays.asList(new User(), new User()));
        when(adminService.findAllUsersPage(0)).thenReturn(mockPage);
        when(adminService.getPageList(mockPage)).thenReturn(Arrays.asList(1, 2, 3));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_users"))
                .andExpect(model().attribute("section", "users"))
                .andExpect(model().attribute("page", 0))
                .andExpect(model().attribute("allPages", 5))
                .andExpect(model().attributeExists("userList"))
                .andExpect(model().attribute("pageList", Arrays.asList(1, 2, 3)));

        verify(adminService).findAllUsersPage(0);
        verify(adminService).getPageList(mockPage);
    }

    @Test
    @WithMockUser(authorities = { "ADMIN_DASHBOARD" })
    public void showUserManagement_serviceException_redirectsToDashboard() throws Exception {
        when(adminService.findAllUsersPage(anyInt())).thenThrow(new ServiceException("Page error"));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @WithMockUser(authorities = { "ADMIN_DASHBOARD" })
    public void showUserManagement_withPageParameter_callsServiceWithCorrectPage() throws Exception {
        int testPage = 2;
        @SuppressWarnings("unchecked")
        Page<User> mockPage = mock(Page.class);
        when(mockPage.getTotalPages()).thenReturn(10);
        when(mockPage.getContent()).thenReturn(Arrays.asList(new User()));
        when(adminService.findAllUsersPage(testPage)).thenReturn(mockPage);
        when(adminService.getPageList(mockPage)).thenReturn(Arrays.asList(1, 2, 3, 4, 5));

        mockMvc.perform(get("/admin/users?page={page}", testPage))
                .andExpect(status().isOk())
                .andExpect(model().attribute("page", testPage));

        verify(adminService).findAllUsersPage(testPage);
    }

    @Test
    @WithMockUser(authorities = { "ADMIN_DASHBOARD" })
    public void showUserManagement_defaultPageParameter_callsServiceWithPageZero() throws Exception {
        @SuppressWarnings("unchecked")
        Page<User> mockPage = mock(Page.class);
        when(adminService.findAllUsersPage(0)).thenReturn(mockPage);
        when(mockPage.getTotalPages()).thenReturn(5);
        when(adminService.getPageList(mockPage)).thenReturn(Arrays.asList(1, 2, 3));

        mockMvc.perform(get("/admin/users"))
                .andExpect(model().attribute("page", 0));

        verify(adminService).findAllUsersPage(0);
    }

    @Test
    @WithMockUser(authorities = { "ADMIN_DASHBOARD" })
    public void seeUser_userExists_returnsOkAndView() throws Exception {
        // Arrange
        long testUserId = 1L;
        User mockUser = new User();
        mockUser.setId(testUserId);
        mockUser.setEmail("test@example.com");
        mockUser.setNickname("testUser");

        when(adminService.findUser(testUserId)).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(get("/admin/users/view/{id}", testUserId))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(view().name("admin_userview")) // Expect the correct view name
                .andExpect(model().attribute("section", "users")) // Verify model attributes
                .andExpect(model().attribute("userDTO", new UserDTO(mockUser))); // Verify DTO mapping

        verify(adminService).findUser(testUserId); // Ensure the service method was called
    }

    @Test
    @WithMockUser(authorities = { "ADMIN_DASHBOARD" })
    public void seeUser_userNotFound_redirectsToUsersPage() throws Exception {
        // Arrange
        long nonExistentUserId = 999L;

        doThrow(new UsernameNotFoundException("User not found")).when(adminService).findUser(nonExistentUserId);

        // Act & Assert
        mockMvc.perform(get("/admin/users/view/{id}", nonExistentUserId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(adminService).findUser(nonExistentUserId);
    }

    @Test
    @WithMockUser(authorities = { "ADMIN_DASHBOARD" })
    public void updateUser_validInput_updatesUserAndRedirects() throws Exception {
        // Arrange
        long testUserId = 1L;
        UserDTO userDTO = new UserDTO();
        userDTO.setId(testUserId);
        userDTO.setEmail("updated@example.com");
        userDTO.setNickname("updatedUser");

        // Act & Assert
        mockMvc.perform(post("/admin/users/update/{id}", testUserId)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .flashAttr("userDTO", userDTO))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(adminService).updateUser(userDTO);
    }

    @Test
    @WithMockUser(authorities = { "ADMIN_DASHBOARD" })
    public void updateUser_invalidId_throwsExceptionAndRedirects() throws Exception {
        // Arrange
        long invalidUserId = 999L;
        UserDTO userDTO = new UserDTO();
        userDTO.setId(invalidUserId);
        userDTO.setEmail("invalid@example.com");
        userDTO.setNickname("invalidUser");

        doThrow(new UsernameNotFoundException("User not found")).when(adminService).updateUser(userDTO);

        // Act & Assert
        mockMvc.perform(post("/admin/users/update/{id}", invalidUserId)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .flashAttr("userDTO", userDTO))
                .andExpect(status().is3xxRedirection());

        verify(adminService).updateUser(userDTO);
    }

    @Test
    @WithMockUser(authorities = { "ADMIN_DASHBOARD" })
    public void updateUserScopes_validRequest_redirectsToUserView() throws Exception {
        // Arrange
        long testUserId = 1L;
        String testAuthority = "NEW_AUTHORITY";

        // Act & Assert
        mockMvc.perform(post("/admin/users/scopes/add/{id}", testUserId)
                .param("authority", testAuthority)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/view/" + testUserId));

        verify(adminService).addScope(testUserId, testAuthority);
    }

    @Test
    @WithMockUser(authorities = { "ADMIN_DASHBOARD" })
    public void updateUserScopes_userNotFound_redirectsToUsers() throws Exception {
        // Arrange
        long invalidUserId = 999L;
        String testAuthority = "NEW_AUTHORITY";

        doThrow(new UsernameNotFoundException("User not found"))
                .when(adminService).addScope(invalidUserId, testAuthority);

        // Act & Assert
        mockMvc.perform(post("/admin/users/scopes/add/{id}", invalidUserId)
                .param("authority", testAuthority)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());

        verify(adminService).addScope(invalidUserId, testAuthority);
    }

    @Test
    @WithMockUser(authorities = { "ADMIN_DASHBOARD" })
    public void removeUserScope_validRequest_redirectsToUserView() throws Exception {
        // Arrange
        long testUserId = 1L;
        long testScopeId = 1L;

        // Act & Assert
        mockMvc.perform(post("/admin/users/scopes/remove/{id}", testUserId)
                .param("scopeId", String.valueOf(testScopeId))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/view/" + testUserId));

        verify(adminService).removeScope(testUserId, testScopeId);
    }

    @Test
    @WithMockUser(authorities = { "ADMIN_DASHBOARD" })
    public void removeUserScope_scopeNotFound_redirectsToDashboard() throws Exception {
        // Arrange
        long testUserId = 1L;
        long invalidScopeId = 999L;

        doThrow(new ServiceException("Scope not found"))
                .when(adminService).removeScope(testUserId, invalidScopeId);

        // Act & Assert
        mockMvc.perform(post("/admin/users/scopes/remove/{id}", testUserId)
                .param("scopeId", String.valueOf(invalidScopeId))
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        verify(adminService).removeScope(testUserId, invalidScopeId);
    }
}
