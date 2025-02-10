package com.estebandev.minicloud.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.service.FileManagerService;
import com.estebandev.minicloud.service.UserService;
import com.estebandev.minicloud.service.utils.FileManagerUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;

@ControllerAdvice(assignableTypes = FileDashboardController.class)
@RequiredArgsConstructor
public class FileDashboardControllerAdvice {
    private final UserService userService;
    private final FileManagerService fileManagerService;

    @SuppressWarnings("rawtypes")
	private final Map<Class, String> predefineMessages = Map.ofEntries(
            Map.entry(HandlerMethodValidationException.class, "Illegal characters"));

    @ModelAttribute
    public void addModelAttributes(Model model) {
        User user = userService.getUserFromAuth();
        boolean userDirectory = fileManagerService.isValidDirectory(user.getEmail());
        model.addAttribute("nickname", user.getNickname());
        model.addAttribute("userDirectoryExists", userDirectory);
    }

    @ExceptionHandler({ IOException.class, FileNotFoundException.class, IllegalArgumentException.class,
            MaxUploadSizeExceededException.class, ConstraintViolationException.class,
            HandlerMethodValidationException.class, FileNotFoundException.class })
    public String manageIOException(RedirectAttributes redirectAttributes, Exception e,
            HttpServletRequest request) {
        String errorMessage = String.format("The action is not possible. %s", e.getMessage());

        redirectAttributes.addFlashAttribute("error", predefineMessages.getOrDefault(e.getClass(), errorMessage));

        String pathString = request.getParameter("path");
        redirectAttributes.addFlashAttribute("pathRequest",
                FileManagerUtils.getParent(pathString).toString());

        return "redirect:/files/error";
    }
}
