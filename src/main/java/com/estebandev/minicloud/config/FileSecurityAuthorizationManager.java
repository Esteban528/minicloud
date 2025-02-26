package com.estebandev.minicloud.config;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.UUID;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.estebandev.minicloud.entity.FileMetadata;
import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.entity.UserMetadata;
import com.estebandev.minicloud.service.FileManagerService;
import com.estebandev.minicloud.service.FileMetadataService;
import com.estebandev.minicloud.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FileSecurityAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FileMetadataService fileMetadataService;
    private final FileManagerService fileManagerService;
    private final UserService userService;
    private final List<String> acceptedUris = List.of(
            "/files",
            "/files/action/createIfNotExistPersonalDirectory",
            "/files/action/go/mydir");

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication,
            RequestAuthorizationContext rAuthorizationContext) {

        if (isUserHasAuthority(authentication.get(), "ADMIN_DASHBOARD"))
            return new AuthorizationDecision(true);

        if (!isUserHasAuthority(authentication.get(), "FILE_DASHBOARD"))
            return new AuthorizationDecision(false);

        HttpServletRequest request = rAuthorizationContext.getRequest();

        if (acceptedUris.contains(request.getRequestURI()))
            return new AuthorizationDecision(true);

        String path = request.getParameter("path");
        if (path == null || path.isEmpty())
            return new AuthorizationDecision(false);

        return check(authentication.get(), path);
    }

    private boolean isUserHasAuthority(Authentication authentication, String authority) {
        return !authentication.getAuthorities().stream()
                .filter(a -> a.getAuthority().equals(authority))
                .findFirst()
                .isEmpty();
    }

    private AuthorizationDecision check(Authentication auth, String pathString) {
        String email = auth.getName();

        if (pathString.startsWith(email))
            return new AuthorizationDecision(true);

        Path path = fileManagerService.getRoot().resolve(pathString);
        FileMetadata ownerMetadata;
        try {
            ownerMetadata = fileMetadataService.findMetadataFromKey(path, "owner");
        } catch (IOException e) {
            return new AuthorizationDecision(false);
        }

        if (email.equals(ownerMetadata.getValue()))
            return new AuthorizationDecision(true);

        User user = userService.findAllDataByEmail(email);
        List<UUID> directoriesWithAccess = user.getUserMetadata().stream()
                .filter(um -> um.getKey().startsWith("ACCESS_TO_") && um.getValue().equals("true"))
                .map(um -> UUID.fromString(um.getKey().substring(10)))
                .toList();

        return new AuthorizationDecision(directoriesWithAccess.contains(UUID.fromString(ownerMetadata.getUuid())));
    }
}
