package com.estebandev.minicloud.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.estebandev.minicloud.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {
    @Value("${var.title}")
    private String title;

    private final UserService userService;

    @ModelAttribute
    public void addModelAttributes(Model model, HttpServletRequest request) {
        String mainEndpoint = "";
        String[] requestArgs = request.getRequestURI().split("/");

        if (requestArgs.length > 0) {
            mainEndpoint = requestArgs[1];
        }

        model.addAttribute("title", StringUtils.capitalize(title));
        model.addAttribute("request", request);
        model.addAttribute("auth", userService.isAuthenticated());
        model.addAttribute("mainEndpoint", StringUtils.capitalize(mainEndpoint));
    }
}
