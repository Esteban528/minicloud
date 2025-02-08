package com.estebandev.minicloud.service.exception;

import java.io.IOException;

public class FileIsNotDirectoryException extends IOException {
    public FileIsNotDirectoryException(String message) {
        super(message);
    }
}
