package com.estebandev.minicloud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.estebandev.minicloud.entity.Scopes;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private String adminEmail = "admin@example.com";

    @BeforeEach
    void beforeTest() {
        ReflectionTestUtils.setField(userService, "adminEmail", adminEmail);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void findByEmailTest() {
        String email = "test@minicloud.com";
        User user = User.builder()
                .nickname("TestUser")
                .email(email)
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        userService.findByEmail(email);

        verify(userRepository).findByEmail(email);
    }

    @Test
    public void createUserTestEntity() {
        String email = "test@minicloud.com";
        User user = User.builder()
                .nickname("TestUser")
                .email(email)
                .password("TestPassword")
                .scopes(new ArrayList<>())
                .build();
        when(userRepository.save(user)).thenReturn(user);
        when(passwordEncoder.encode("TestPassword")).thenReturn("asd");

        User finalUser = userService.createUser(user);

        verify(userRepository).save(user);
        verify(passwordEncoder).encode("TestPassword");
        assertThat(finalUser.getPassword()).isEqualTo("asd");
    }

    @Test
    public void createUserTestEntityAdminAuthorities() {
        String email = adminEmail;
        System.out.println(email);
        User user = User.builder()
                .nickname("TestUser")
                .email(email)
                .password("TestPassword")
                .scopes(new ArrayList<>())
                .build();

        when(userRepository.save(user)).thenReturn(user);
        when(passwordEncoder.encode("TestPassword")).thenReturn("asd");

        User finalUser = userService.createUser(user);

        verify(userRepository).save(user);
        verify(passwordEncoder).encode("TestPassword");
        assertThat(finalUser.getPassword()).isEqualTo("asd");
        assertThat(finalUser.getScopes()).contains(
                Scopes.builder().user(finalUser).authority("ROLE_USER").build(),
                Scopes.builder().user(finalUser).authority("ADMIN_DASHBOARD").build(),
                Scopes.builder().user(finalUser).authority("FILE_DASHBOARD").build(),
                Scopes.builder().user(finalUser).authority("FILE_UPLOAD").build());
    }

    @Test
    public void createUserTestEntityUserAuthorities() {
        String email = "example@minicloud.com";
        System.out.println(email);
        User user = User.builder()
                .nickname("TestUser")
                .email(email)
                .password("TestPassword")
                .scopes(new ArrayList<>())
                .build();

        when(userRepository.save(user)).thenReturn(user);
        when(passwordEncoder.encode("TestPassword")).thenReturn("asd");

        User finalUser = userService.createUser(user);

        verify(userRepository).save(user);
        verify(passwordEncoder).encode("TestPassword");
        assertThat(finalUser.getPassword()).isEqualTo("asd");
        assertThat(finalUser.getScopes()).containsOnly(
                Scopes.builder().user(finalUser).authority("ROLE_USER").build());
    }

    @Test
    public void createUserTestParams() {
        String email = "test@minicloud.com";
        String nickname = "TestUser";
        String password = "TestPassword";

        User user = User.builder()
                .nickname(nickname)
                .email(email)
                .password(password)
                .scopes(new ArrayList<>())
                .build();

        when(passwordEncoder.encode(password)).thenReturn("asd");
        when(userRepository.save(argThat(u -> u.getEmail().equals(email) &&
                u.getNickname().equals(nickname) &&
                u.getPassword().equals("asd"))))
                .thenReturn(user);

        userService.createUser(email, nickname, password);

        verify(userRepository).save(argThat(u -> u.getEmail().equals(email) &&
                u.getNickname().equals(nickname) &&
                u.getPassword().equals("asd")));
        verify(passwordEncoder).encode(password);
    }

    @Test
    public void isUserExistTest() {
        String email = "test@minicloud.com";
        User user = User.builder()
                .nickname("TestUser")
                .email(email)
                .password("TestPassword")
                .scopes(new ArrayList<>())
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        boolean isExist = userService.isUserExists(email);

        assertThat(isExist).isTrue();
        verify(userRepository).findByEmail(email);
    }

    @Test
    public void isUserNotExistTest() {
        String email = "test@minicloud.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        boolean isExist = userService.isUserExists(email);

        assertThat(isExist).isFalse();
        verify(userRepository).findByEmail(email);
    }

    @Test
    public void updatePasswordTest() {
        String email = "test@minicloud.com";
        String oldPassword = "TestPassword";
        String newPassword = "TestPassword";

        User user = User.builder()
                .nickname("test nick")
                .email(email)
                .password(oldPassword)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(
                Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn(newPassword);

        userService.updatePassword(email, newPassword);
        verify(userRepository).save(argThat(u -> u.getEmail().equals(email) && u.getPassword().equals(newPassword)));
        verify(passwordEncoder).encode(oldPassword);
    }

    @Test
    void isAuthenticated_WhenAuthenticationIsNull_ReturnsFalse() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(null);
        SecurityContextHolder.setContext(context);

        boolean result = userService.isAuthenticated();
        assertThat(result).isFalse();
    }

    @Test
    void isAuthenticated_WhenNotAuthenticated_ReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        boolean result = userService.isAuthenticated();
        assertThat(result).isFalse();
    }

    @Test
    void isAuthenticated_WhenAuthenticatedButNoUserAuthority_ReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ADMIN"));
        // when(auth.getAuthorities()).thenReturn((Collection<? extends
        // GrantedAuthority>) authorities);
        Mockito.doReturn(authorities).when(auth).getAuthorities();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        boolean result = userService.isAuthenticated();
        assertThat(result).isFalse();
    }

    @Test
    void isAuthenticated_WhenAuthenticatedAndUserAuthority_ReturnsTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER"));
        Mockito.doReturn(authorities).when(auth).getAuthorities();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        boolean result = userService.isAuthenticated();
        assertThat(result).isTrue();
    }
}
