package com.estebandev.minicloud.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.estebandev.minicloud.controller.dto.UserDTO;
import com.estebandev.minicloud.entity.Scopes;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.repository.UserRepository;
import com.estebandev.minicloud.service.exception.ServiceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final UserService userService;
    public final int PAGE_SIZE = 10;

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> findAllUsersPage(int page) throws ServiceException {
        PageRequest pageRequest = PageRequest.of(page, PAGE_SIZE);
        Page<User> pageUser = userRepository.findAll(pageRequest);

        if (pageRequest.isUnpaged()) {
            throw new ServiceException("This page doesn't exist");
        }
        return pageUser;
    }

    public List<Integer> getPageList(Page<? extends User> userPage) {
        int currentPage = userPage.getNumber();
        int totalPages = userPage.getTotalPages();

        int range = 2;
        int start = Math.max(0, currentPage - range);
        int end = Math.min(totalPages - 1, currentPage + range);

        List<Integer> pages = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            pages.add(i + 1);
        }
        return pages;
    }

    public User findUser(long id) {
        return userService.findById(id);
    }

    public void updateUser(UserDTO userDTO) {
        User user = findUser(userDTO.getId());
        user.setEmail(userDTO.getEmail());
        user.setNickname(userDTO.getNickname());
        userRepository.save(user);
    }

    public void addScope(long userId, String authority) {
        User user = findUser(userId);
        user.getScopes().add(
                Scopes.builder()
                        .authority(authority)
                        .user(user)
                        .build());
        userRepository.save(user);
    }

    public void removeScope(long userId, long scopeId) throws ServiceException {
        User user = findUser(userId);
        Scopes scope = user.getScopes().stream()
                .filter(s -> s.getId().equals(scopeId))
                .findFirst().orElseThrow(() -> new ServiceException("scope not found"));

        user.getScopes().remove(scope);

        userRepository.save(user);
    }
}
