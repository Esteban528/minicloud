package com.estebandev.minicloud.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
    @Value("${var.title}")
    private String title;

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @PostMapping("/")
    public String homePost() {
        return "redirect:/";
    }
}
