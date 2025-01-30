package com.estebandev.minicloud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.estebandev.minicloud.service.exception.EmailServiceException;

@ExtendWith(MockitoExtension.class)
public class CodeAuthServiceTest {
    @Mock
    private EmailService emailService;

    @InjectMocks
    private CodeAuthService codeAuthService;

    @Test
    public void generateCodeTest() {
        int firstCode = codeAuthService.generateCode();
        int secondCode = codeAuthService.generateCode();

        assertThat(firstCode).isNotEqualTo(secondCode);
    }

    @Test
    public void makeCodeTest() {
        String email = "email@minicloud.com";

        int code = codeAuthService.makeCode(email);

        assertThat(codeAuthService.getCode()).isEqualTo(code);
        assertThat(codeAuthService.getEmail()).isEqualTo(email);
    }

    @Test
    public void clearServiceTest() {
        codeAuthService.makeCode("test@minicloud.com");

        codeAuthService.clearService();

        assertThat(codeAuthService.getCode()).isEqualTo(0);
        assertThat(codeAuthService.getEmail()).isNull();
    }

    @Test
    public void sendCodeToEmailTest() throws EmailServiceException {
        // Arrange
        String email = "test@minicloud.com";

        // Act
        codeAuthService.sendCodeToEmail(email);
        int code = codeAuthService.getCode();

        // Assert
        verify(emailService).sendEmail(
                eq(email),
                anyString(),
                argThat(argument -> argument.contains(Integer.toString(code))));
    }
}
