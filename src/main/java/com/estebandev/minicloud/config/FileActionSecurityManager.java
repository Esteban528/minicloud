package com.estebandev.minicloud.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.estebandev.minicloud.entity.FileMetadata;
import com.estebandev.minicloud.service.FileManagerService;
import com.estebandev.minicloud.service.FileMetadataService;
import com.estebandev.minicloud.service.FileSecurityService;

import lombok.RequiredArgsConstructor;

@Component("authF")
@RequiredArgsConstructor
public class FileActionSecurityManager {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FileManagerService fileManagerService;
    private final FileMetadataService fileMetadataService;
    private final FileSecurityService fileSecurityService;

    public boolean decide(String pathString, boolean onlyOwner, MethodSecurityExpressionOperations operations) {
        if (operations.hasAuthority("ADMIN_DASHBOARD")) return true;

        Authentication authentication = operations.getAuthentication();
        String email = authentication.getName();

        if (pathString.toLowerCase().startsWith(email.toLowerCase()))
            return true;

        Path path = fileManagerService.getRoot().resolve(pathString);
        if (!Files.isDirectory(path)) {
            path = fileManagerService.getRoot().relativize(path.getParent());
            pathString = path.toString();
        }

        try {
            FileMetadata ownerMetadata = fileMetadataService.findMetadataFromKey(path, "owner");
            if (ownerMetadata.getValue().equals(email))
                return true;

        } catch (NoSuchElementException | IOException e) {
            if (onlyOwner)
                return false;
        }

        if (onlyOwner)
            return false;

        try {
            boolean result = fileSecurityService.isUserFromAuthHasAccessTo(pathString);
            return result;
        } catch (IOException e) {
            return false;
        }
    }
}
