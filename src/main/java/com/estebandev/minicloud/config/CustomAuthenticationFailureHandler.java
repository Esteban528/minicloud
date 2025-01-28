package com.estebandev.minicloud.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private static final Map<String, String> ERROR_MESSAGES = new HashMap<>();

    static {
        ERROR_MESSAGES.put("Bad credentials", "Invalid username or password.");
        ERROR_MESSAGES.put("User is disabled", "Your account is disabled.");
        ERROR_MESSAGES.put("User account is locked", "Your account is locked.");
        ERROR_MESSAGES.put("User account has expired", "Your account has expired.");
        ERROR_MESSAGES.put("User not found", "User not found.");
    }

    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        String errorMessage = ERROR_MESSAGES.getOrDefault(
                exception.getMessage(),
                "Authentication failed. Please try again.");

        request.getSession().setAttribute("error", errorMessage);
        response.sendRedirect("/login?error");
    }
}
