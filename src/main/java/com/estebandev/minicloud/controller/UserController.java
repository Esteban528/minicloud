package com.estebandev.minicloud.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.estebandev.minicloud.controller.dto.RegisterUserDTO;
import com.estebandev.minicloud.service.CodeAuthService;
import com.estebandev.minicloud.service.UserService;
import com.estebandev.minicloud.service.exception.EmailServiceException;
import com.estebandev.minicloud.service.exception.ManyAttempsException;
import com.estebandev.minicloud.service.exception.UserAlreadyExistsException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final CodeAuthService codeAuthService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private boolean isAuthenticated() {
        return userService.isAuthenticated();
    }

    @GetMapping("/login")
    public String login() {
        if (isAuthenticated()) {
            return "redirect:/logout";
        }
        return "login";
    }

    @GetMapping("/logout")
    public String logout() {
        return "logout";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (isAuthenticated()) {
            return "redirect:/logout";
        }
        model.addAttribute("registerUserDTO", new RegisterUserDTO());
        return "register";
    }

    @PostMapping("/register")
    public String createUser(
            @Valid RegisterUserDTO userRegistrationForm,
            BindingResult bindingResult,
            Model model,
            @RequestParam(required = false, defaultValue = "0") int code) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        if(userService.isUserExists(userRegistrationForm.getEmail())){
            bindingResult.rejectValue("email", "user.exists", "This email already exists");
            return "register";
        }

        model.addAttribute("registerUserDTO", userRegistrationForm);

        if (code == 0) {
            try {
                codeAuthService.sendCodeToEmail(userRegistrationForm.getEmail());
                return "authcode";
            } catch (EmailServiceException e) {
                logger.error(String.format("EmailServiceException %s: %s", e.getLocalizedMessage()));
                bindingResult.rejectValue("email", "user.exists", "An error occurred. Please try again later.");
                return "register";
            }
        } else {
            try {
                if (codeAuthService.validateCode(userRegistrationForm.getEmail(), code)) {
                    userService.createUser(userRegistrationForm.getEmail(), userRegistrationForm.getNickname(),
                            userRegistrationForm.getPassword());
                    return "redirect:/login?register_ok";
                } else {
                    model.addAttribute("error", "The code was wrong.");
                    return "authcode";
                }
            } catch (ManyAttempsException e) {
                logger.info(String.format("User has reached max attemps - %s", userRegistrationForm.getEmail()));
                bindingResult.rejectValue("email", "user.blocked", e.getMessage());
                return "register";
            }
        }
    }

    @GetMapping("/recoveryportal")
    public String portalRecovery() {
        return "recoveryportal";
    }

    @GetMapping("/passwordrecovery")
    public String passwordRecovery(@RequestParam(required = true) String email, Model model) {
        if (!userService.isUserExists(email))
            return "redirect:/login";

        codeAuthService.clearService();
        model.addAttribute("email", email);

        try {
            codeAuthService.sendCodeToEmail(email);
            return "recoverycode";
        } catch (EmailServiceException e) {
            logger.error(String.format("EmailServiceException %s: %s", e.getLocalizedMessage()));
        }

        return "redirect:/login";
    }

    @PostMapping("/passwordrecovery")
    public String passwordrecoveryPost(@RequestParam(required = true) String email,
            @RequestParam(required = true) int code, Model model) {
        if (!userService.isUserExists(email))
            return "redirect:/login";

        try {
            model.addAttribute("email", email);
            model.addAttribute("code", code);
            if (codeAuthService.validateCode(email, code)) {
                return "recoveryform";
            } else {
                model.addAttribute("error", "The code was wrong.");
                return "recoverycode";
            }
        } catch (ManyAttempsException e) {
            logger.info(String.format("User has reached max attemps - %s", email));
            return "redirect:/login";
        }
    }

    @PostMapping("/passwordrecovery/changepassword")
    public String passwordrecoveryPut(@RequestParam(required = true) String email,
            @RequestParam(required = true) int code,
            @RequestParam(required = true) String password,
            Model model) {

        if (!userService.isUserExists(email))
            return "redirect:/login";

        try {
            if (codeAuthService.validateCode(email, code)) {
                userService.updatePassword(email, password);
            }
        } catch (ManyAttempsException e) {
            logger.info(String.format("User has reached max attemps - %s", email));
        }
        return "redirect:/login";
    }
}
