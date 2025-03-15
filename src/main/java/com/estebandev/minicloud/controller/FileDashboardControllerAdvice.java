package com.estebandev.minicloud.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.estebandev.minicloud.entity.FileMetadata;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.service.FileManagerService;
import com.estebandev.minicloud.service.FileMetadataService;
import com.estebandev.minicloud.service.UserService;
import com.estebandev.minicloud.service.utils.FileManagerUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;

@ControllerAdvice(assignableTypes = {FileDashboardController.class, FileManagerController.class})
@RequiredArgsConstructor
public class FileDashboardControllerAdvice {
    private final UserService userService;
    private final FileManagerService fileManagerService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @SuppressWarnings("rawtypes")
    private final Map<Class, String> predefineMessages = Map.ofEntries(
            Map.entry(HandlerMethodValidationException.class, "Illegal characters"));

    @ModelAttribute
    public void addModelAttributes(Model model, HttpServletRequest request) {
        User user = userService.getUserFromAuth();
        boolean hasAccess = !user.getAuthorities().stream()
                .filter(a -> a.getAuthority().equals("FILE_DASHBOARD"))
                .findFirst().isEmpty();

        boolean userDirectory = fileManagerService.isValidDirectory(user.getEmail());
        model.addAttribute("nickname", user.getNickname());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("userDirectoryExists", userDirectory);
        model.addAttribute("accessToDashboard", hasAccess);

        String pathString = request.getParameter("path");
        if(pathString != null && !pathString.isEmpty()){
            model.addAttribute("ownsDir", pathString.contains(user.getEmail()));          
        }
    }

    @ExceptionHandler({ IOException.class, FileNotFoundException.class, IllegalArgumentException.class,
            MaxUploadSizeExceededException.class, ConstraintViolationException.class,
            HandlerMethodValidationException.class, FileNotFoundException.class,
            AccessDeniedException.class })
    public String manageIOException(RedirectAttributes redirectAttributes, Exception e,
            HttpServletRequest request) {
        logger.debug("Exception handler invoked. Exception {} \nMessage: {}", e.getClass(), e.getMessage());

        String errorMessage = String.format("The action is not possible. %s", e.getMessage());

        redirectAttributes.addFlashAttribute("error", predefineMessages.getOrDefault(e.getClass(), errorMessage));

        String pathString = request.getParameter("path");

        String parent = pathString;
        if (!SecurityContextHolder.getContext().getAuthentication().getName().equals(pathString))
            parent = FileManagerUtils.getParent(pathString).toString();

        redirectAttributes.addFlashAttribute("pathRequest", parent);

        return "redirect:/files/error";
    }
}
