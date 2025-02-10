package com.estebandev.minicloud.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.estebandev.minicloud.controller.dto.RegisterUserDTO;
import com.estebandev.minicloud.service.CodeAuthService;
import com.estebandev.minicloud.service.FileManagerService;
import com.estebandev.minicloud.service.UserService;
import com.estebandev.minicloud.service.exception.EmailServiceException;
import com.estebandev.minicloud.service.exception.ManyAttempsException;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
@Import(UserControllerTest.TestSecurityConfig.class) // Importar la configuración de prueba
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FileManagerService fileManagerService;

    @MockitoBean
    private CodeAuthService codeAuthService;

    // Configuración de seguridad para pruebas
    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable()) // Deshabilita CSRF
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    // ==================== GET /register ====================
    @Test
    public void whenGetRegisterAndAuthenticated_redirectToLogout() throws Exception {
        when(userService.isAuthenticated()).thenReturn(true);

        mockMvc.perform(get("/register"))
              .andExpect(status().is3xxRedirection())
              .andExpect(redirectedUrl("/logout"));
    }

    @Test
    public void whenGetRegisterAndNotAuthenticated_showRegisterForm() throws Exception {
        when(userService.isAuthenticated()).thenReturn(false);

        mockMvc.perform(get("/register"))
              .andExpect(status().isOk())
              .andExpect(model().attributeExists("registerUserDTO"))
              .andExpect(view().name("register"));
    }

    // ==================== POST /register ====================
    @Test
    public void whenPostRegisterWithInvalidData_returnsRegisterView() throws Exception {
        RegisterUserDTO invalidUser = new RegisterUserDTO();
        invalidUser.setEmail("invalid-email"); // Suponiendo que hay validación @Email

        mockMvc.perform(post("/register")
                .flashAttr("registerUserDTO", invalidUser))
              .andExpect(status().isOk())
              .andExpect(view().name("register"));
    }

    @Test
    public void whenPostRegisterWithExistingEmail_rejectsEmail() throws Exception {
        RegisterUserDTO existingUser = new RegisterUserDTO();
        existingUser.setEmail("existing@email.com");
        when(userService.isUserExists(anyString())).thenReturn(true);

        mockMvc.perform(post("/register")
                .flashAttr("registerUserDTO", existingUser))
              .andExpect(status().isOk())
              .andExpect(view().name("register"));
    }

    @Test
    public void whenPostRegisterValidData_redirectsToAuthCode() throws Exception {
        RegisterUserDTO validUser = new RegisterUserDTO();
        validUser.setEmail("valid@email.com");
        validUser.setPassword("password");
        validUser.setNickname("nickname");

        mockMvc.perform(post("/register")
                .flashAttr("registerUserDTO", validUser))
              .andExpect(status().isOk())
              .andExpect(view().name("authcode"));

        verify(codeAuthService).sendCodeToEmail(validUser.getEmail());
    }

    @Test
    public void whenPostRegisterFailsToSendCode_returnsError() throws Exception {
        RegisterUserDTO validUser = new RegisterUserDTO();
        validUser.setEmail("valid@email.com");
        doThrow(new EmailServiceException("Error")).when(codeAuthService).sendCodeToEmail(anyString());

        mockMvc.perform(post("/register")
                .flashAttr("registerUserDTO", validUser))
              .andExpect(status().isOk())
              .andExpect(view().name("register"));
    }

    // ==================== POST /register/verifyCode ====================
    @Test
    public void whenVerifyCodeWithValidCode_redirectsToLogin() throws Exception {
        RegisterUserDTO user = new RegisterUserDTO();
        user.setNickname("Tester");
        user.setEmail("test@email.com");
        user.setPassword("anypassword");
        when(codeAuthService.validateCode(anyString(), anyInt())).thenReturn(true);

        mockMvc.perform(post("/register/verifyCode")
                .param("code", "123456")
                .flashAttr("registerUserDTO", user))
              .andExpect(status().is3xxRedirection())
              .andExpect(redirectedUrl("/login?register_ok"));

        verify(userService).createUser(anyString(), anyString(), anyString());
    }

    @Test
    public void whenVerifyCodeWithInvalidCode_returnsAuthcodeWithError() throws Exception {
        RegisterUserDTO user = new RegisterUserDTO();
        user.setEmail("test@email.com");
        when(codeAuthService.validateCode(anyString(), anyInt())).thenReturn(false);

        mockMvc.perform(post("/register/verifyCode")
                .param("code", "1234")
                .flashAttr("registerUserDTO", user))
              .andExpect(status().isOk())
              .andExpect(model().attributeExists("error"))
              .andExpect(view().name("authcode"));
    }

    @Test
    public void whenVerifyCodeExceedsAttempts_redirectsToRegister() throws Exception {
        RegisterUserDTO user = new RegisterUserDTO();
        user.setEmail("test@email.com");
        when(codeAuthService.validateCode(anyString(), anyInt()))
            .thenThrow(new ManyAttempsException("Too many attempts"));

        mockMvc.perform(post("/register/verifyCode")
                .param("code", "123456")
                .flashAttr("registerUserDTO", user))
              .andExpect(status().isOk())
              .andExpect(view().name("register"))
              .andExpect(model().attributeHasFieldErrors("registerUserDTO", "email"));
    }

    // ==================== GET /passwordrecovery ====================
    @Test
    public void whenPasswordRecoveryWithNonExistingEmail_redirectsToLogin() throws Exception {
        when(userService.isUserExists(anyString())).thenReturn(false);

        mockMvc.perform(get("/passwordrecovery")
                .param("email", "nonexisting@email.com"))
              .andExpect(status().is3xxRedirection())
              .andExpect(redirectedUrl("/login"));
    }

    @Test
    public void whenPasswordRecoveryWithExistingEmail_showsRecoveryCode() throws Exception {
        when(userService.isUserExists(anyString())).thenReturn(true);

        mockMvc.perform(get("/passwordrecovery")
                .param("email", "valid@email.com"))
              .andExpect(status().isOk())
              .andExpect(view().name("recoverycode"));
    }

    // ==================== POST /passwordrecovery/changepassword ====================
    @Test
    public void whenChangePasswordWithValidCode_updatesPassword() throws Exception {
        when(codeAuthService.validateCode(anyString(), anyInt())).thenReturn(true);
        when(userService.isUserExists(anyString())).thenReturn(true);

        mockMvc.perform(post("/passwordrecovery/changepassword")
                .param("email", "test@email.com")
                .param("code", "123456")
                .param("password", "newPassword"))
              .andExpect(status().is3xxRedirection())
              .andExpect(redirectedUrl("/login"));

        verify(userService).updatePassword(anyString(), anyString());
    }

}
