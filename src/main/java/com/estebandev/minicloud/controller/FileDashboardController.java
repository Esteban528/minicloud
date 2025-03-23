package com.estebandev.minicloud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileDashboardController {

    @GetMapping()
    public String showDashboard(Model model) {
        return "redirect:/files/action/createIfNotExistPersonalDirectory";
    }

    @GetMapping("/error")
    public String showErrorPopup(@RequestParam(required = false, defaultValue = "", name = "msg") String paramError,
            Model model) {
        if (!paramError.isEmpty())
            model.addAttribute("error", paramError);

        return "files_error";
    }
}
