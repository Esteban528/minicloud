package com.estebandev.minicloud.service.exception;

public class FileIsNotDirectoryException extends FileManagerException {
    public FileIsNotDirectoryException(String message) {
        super(message);
    }
}
