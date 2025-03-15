package com.estebandev.minicloud.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estebandev.minicloud.entity.Scopes;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.entity.UserMetadata;
import com.estebandev.minicloud.repository.UserMetadataRepository;
import com.estebandev.minicloud.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMetadataRepository userMetadataRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${var.admin.email}")
    private String adminEmail;

    public User findByEmail(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("The user doesn't exist"));
        return user;
    }

    @Transactional
    public User findAllDataByEmail(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("The user doesn't exist"));
        user.getUserMetadata().size();
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
        return saveUser(user);
    }

    public User saveUser(User user) {
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
        saveUser(user);
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

    public User getUserFromAuth() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication auth = securityContext.getAuthentication();
        var user = User.builder()
                .email(auth.getName())
                .build();
        var scopeList = auth.getAuthorities().stream()
                .map(a -> Scopes.builder().user(user).authority(a.getAuthority()).build())
                .toList();
        user.setScopes(scopeList);
        return user;
    }

    @Transactional
    public User getUserAllDataFromAuth() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication auth = securityContext.getAuthentication();
        return findAllDataByEmail(auth.getName());
    }

    public List<UserMetadata> findMetadatasByKeySearch(String contain) {
        return userMetadataRepository.findByKeyContaining(contain);
    }

    public Optional<UserMetadata> findMetadatasByKeySearch(User user, String contain) {
        return findMetadatasByKeySearchList(user, contain).stream().findFirst();
    }

    public List<UserMetadata> findMetadatasByKeySearchList(User user, String contain) {
        return userMetadataRepository.findByUserAndKeyContaining(user, contain);
    }
}
