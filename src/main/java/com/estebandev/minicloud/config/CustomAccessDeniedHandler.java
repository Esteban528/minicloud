package com.estebandev.minicloud.config;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/files/")) {
            response.sendRedirect("/files/error?msg=Acces%20denied");
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have access to " + requestURI);
        }
    }

}
