package com.estebandev.minicloud.service;

import java.util.Base64;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.estebandev.minicloud.service.exception.EmailServiceException;

@Service
public class EmailRestServiceImpl implements EmailService {

    @Value("${var.email-api.url}")
    private String apiUrl;

    @Value("${var.email-api.username}")
    private String username;

    @Value("${var.email-api.password}")
    private String password;

    private void sendEmailRequest(String email, String subject, String message) throws EmailServiceException {
        String auth = String.format("%s:%s", username, password);
        RestClient restClient = RestClient.create(this.apiUrl);

        String response = restClient
                .post()
                .uri("/email/send")
                .headers(header -> {
                    header.add("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes()));
                })
                .body(String.format("""
                                            {
                        "email": "%s",
                        "subject": "%s",
                        "message": "%s"
                                            }
                                        """, email, subject, message))
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

        if (!response.toLowerCase().contains("true"))
            throw new EmailServiceException("An error occurred while sending the mail.");
    }

    @Override
    public void sendEmail(String email, String subject, String message) throws EmailServiceException {
        sendEmailRequest(email, subject, message);
    }
}
