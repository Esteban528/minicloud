package com.estebandev.minicloud.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.estebandev.minicloud.controller.dto.RegisterUserDTO;
import com.estebandev.minicloud.entity.Scopes;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.repository.UserRepository;
import com.estebandev.minicloud.service.exception.UserAlreadyExistsException;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${var.admin.email}")
    private String adminEmail;

    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("The user doesn't exist"));
        return user;
    }

    public User findById(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("The user doesn't exist"));
        return user;
    }

    public User createUser(String email, String nickname, String password) {
        return createUser(
                User.builder()
                        .email(email)
                        .nickname(nickname)
                        .password(password)
                        .scopes(new ArrayList<>())
                        .build());
    }

    public User createUser(User user) {
        String tmpPassword = user.getPassword();
        user.setPassword(passwordEncoder.encode(tmpPassword));
        tmpPassword = null;
        makeAuthorities(user);
        return userRepository.save(user);
    }

    public boolean isUserExists(String email) {
        return userRepository.findByEmail(email.toLowerCase()).orElse(null) != null;
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.isAuthenticated() && !authentication.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().equals("ROLE_USER"))
                .findFirst()
                .isEmpty();
    }

    public void updatePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("The user doesn't exist"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private void makeAuthorities(User user) {
        user.getScopes().add(
                Scopes.builder().user(user).authority("ROLE_USER").build());

        if (user.getEmail().equals(this.adminEmail)) {
            user.getScopes().addAll(
                    List.of(Scopes.builder().user(user).authority("ADMIN_DASHBOARD").build(),

                            Scopes.builder().user(user).authority("FILE_DASHBOARD").build(),

                            Scopes.builder().user(user).authority("FILE_UPLOAD").build()));
        }
    }
}
