package com.estebandev.minicloud.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.estebandev.minicloud.controller.dto.UserDTO;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.service.AdminService;
import com.estebandev.minicloud.service.exception.ServiceException;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ADMIN_DASHBOARD')")
    public String showDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        model.addAttribute("section", "dashboard");
        model.addAttribute("authObject", auth);

        return "admin_dashboard";
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN_DASHBOARD')")
    public String showUserManagement(
            Model model,
            @RequestParam(defaultValue = "0") int page) {

        try {
            Page<User> userPage = adminService.findAllUsersPage(page);

            model.addAttribute("section", "users");
            model.addAttribute("page", page);
            model.addAttribute("allPages", userPage.getTotalPages());
            model.addAttribute("userList", userPage.getContent());
            model.addAttribute("pageList", adminService.getPageList(userPage));
        } catch (ServiceException e) {
            return "redirect:/admin/dashboard";
        }

        return "admin_users";
    }

    @GetMapping("/users/view/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DASHBOARD')")
    public String seeUser(
            Model model,
            @PathVariable(required = true) long id) {

        try {
            User user = adminService.findUser(id);
            UserDTO userDTO = new UserDTO(user);

            model.addAttribute("section", "users");
            model.addAttribute("userDTO", userDTO);
            model.addAttribute("userScopes", user.getScopes());
            return "admin_userview";
        } catch (UsernameNotFoundException e) {
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/users/update/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DASHBOARD')")
    public String updateUser(
            @PathVariable(required = true) long id, UserDTO userDTO) {

        adminService.updateUser(userDTO);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/scopes/add/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DASHBOARD')")
    public String updateUserScopes(
            @PathVariable(required = true) long id, @RequestParam(required = true) String authority) {

        adminService.addScope(id, authority);
        return "redirect:/admin/users/view/" + id;
    }

    @PostMapping("/users/scopes/remove/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DASHBOARD')")
    public String removeUserScope(
            @PathVariable(required = true) long id, @RequestParam(required = true) long scopeId) {

        try {
            adminService.removeScope(id, scopeId);
            return "redirect:/admin/users/view/" + id;
        } catch (ServiceException e) {
            return "redirect:/admin/dashboard";
        }
    }
}
