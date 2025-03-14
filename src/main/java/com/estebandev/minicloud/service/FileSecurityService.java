package com.estebandev.minicloud.service;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.estebandev.minicloud.entity.User;
import com.estebandev.minicloud.service.exception.FileIsNotDirectoryException;
import com.estebandev.minicloud.service.exception.ServiceException;
import com.estebandev.minicloud.service.utils.FileData;

public interface FileSecurityService {
    List<User> getUserWithAccessTo(String pathString) throws FileIsNotDirectoryException, IOException;

    boolean isUserHasAccessTo(String pathString, User user) throws FileIsNotDirectoryException, IOException;

    boolean isUserFromAuthHasAccessTo(String pathString) throws FileIsNotDirectoryException, IOException;

    void grantAccess(String pathString, String email)
            throws UsernameNotFoundException, IOException, ServiceException;

    void revokeAccess(String pathString, String email)
            throws IOException, ServiceException;

    List<FileData> getFileListUserHasAccess(User user);
}
