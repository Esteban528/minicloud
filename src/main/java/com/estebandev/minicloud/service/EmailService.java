package com.estebandev.minicloud.service;

import com.estebandev.minicloud.service.exception.EmailServiceException;

public interface EmailService {
    void sendEmail(String email, String subject, String message) throws EmailServiceException;
}
