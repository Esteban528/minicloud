package com.estebandev.minicloud.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.entity.UserMetadata;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.exception.ServiceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileSecurityServiceImpl implements FileSecurityService {
    private final FileManagerService fileManagerService;
    private final FileMetadataService fileMetadataService;
    private final UserService userService;

    @Override
    public List<User> getUserWithAccessTo(String pathString) throws FileIsNotDirectoryException, IOException {
        UUID uuid = fileMetadataService.getUuidFromDir(fileManagerService.getRoot().resolve(pathString));
        List<UserMetadata> metadata = userService.findMetadatasByKeySearch(uuid.toString());
        return metadata.stream()
                .filter(m -> m.getValue().equals("true") && m.getKey().startsWith("ACCESS_TO_"))
                .map(m -> m.getUser())
                .toList();
    }

    @Override
    public void grantAccess(String pathString, String email)
            throws UsernameNotFoundException, IOException, ServiceException {

        if (userService.getUserFromAuth().getEmail().equals(email))
            throw new ServiceException("The user can't be your user");

        if (!getUserWithAccessTo(pathString).stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst().isEmpty())
            throw new ServiceException("This user already has access to");

        User user = userService.findByEmail(email);
        grantAccessTo(fileManagerService.getRoot().resolve(pathString), user);
    }

    @Override
    public void revokeAccess(String pathString, String email)
            throws IOException, ServiceException {

        if (userService.getUserFromAuth().getEmail().equals(email))
            throw new ServiceException("You cannot remove yourself");

        if (getUserWithAccessTo(pathString).stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst().isEmpty())
            throw new ServiceException("The user did not have access");

        User user = userService.findByEmail(email);
        revokeAccessTo(fileManagerService.getRoot().resolve(pathString), user);
    }

    private void grantAccessTo(Path path, User user) throws IOException {
        UUID uuid = fileMetadataService.getUuidFromDir(path);
        String metadataKey = parseMetadataAccess(uuid);

        Optional<UserMetadata> existingMetadata = user.getUserMetadata().stream()
                .filter(m -> m.getKey().equals(metadataKey))
                .findFirst();

        if (existingMetadata.isEmpty()) {
            UserMetadata metadata = UserMetadata.builder()
                    .user(user)
                    .key(metadataKey)
                    .value("true")
                    .build();
            user.getUserMetadata().add(metadata);
        } else {
            existingMetadata.get().setValue("true");
        }

        userService.saveUser(user);
    }

    private void revokeAccessTo(Path path, User user) throws IOException, ServiceException {
        UUID uuid = fileMetadataService.getUuidFromDir(path);
        UserMetadata metadata = userService
                .findMetadatasByKeySearch(user, parseMetadataAccess(uuid))
                .orElseThrow(() -> new ServiceException("The access does not exist"));

        metadata.setValue("false");
        userService.saveUser(user);
    }

    private static String parseMetadataAccess(UUID uuid) {
        return String.format("ACCESS_TO_%s", uuid.toString());
    }
}
