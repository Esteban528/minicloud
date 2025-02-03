package com.estebandev.minicloud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileDashboardController {
    private final UserService userService;

   @GetMapping("/")
   public String showDashboard(Model model) {
        User user = userService.getUserFromAuth();
        model.addAttribute("nickname", user.getNickname());
        return "files_dashboard";
   }
}
