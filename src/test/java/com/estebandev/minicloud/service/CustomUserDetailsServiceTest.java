package com.estebandev.minicloud.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.estebandev.minicloud.entity.User;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
	public void loadUserByUsernameTest() {
        String email = "mail@minicloud.com";
        when(userService.findByEmail(anyString())).thenReturn(
            User.builder()
            .email(email)
            .build()
        );

        customUserDetailsService.loadUserByUsername(email);

        verify(userService).findByEmail(email);
	}
}
