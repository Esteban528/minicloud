package com.estebandev.minicloud.service;

import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import com.estebandev.minicloud.service.exception.EmailServiceException;
import com.estebandev.minicloud.service.exception.ManyAttempsException;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
@Getter
public class CodeAuthService {
    private int code = 0;
    private final EmailService emailService;

    @Value("${var.title}")
    private String title;

	private String email;

    private int attemps = 0;
    public final int MAX_ATTEMPS = 5;

	public void sendCodeToEmail(String email) throws EmailServiceException {
        if (code == 0) {
            makeCode(email);
            emailService.sendEmail(
                email, 
                "Confirmation code from " + title ,
                "Your confirmation code to you register is: " + code
            );
        } 
    }

    public boolean validateCode(String email, int code) throws ManyAttempsException {
        if (++attemps >= MAX_ATTEMPS) {
            throw new ManyAttempsException("Many attemps to creation code");
        }
        return code == this.code && email.equals(this.email);
    }

    public int makeCode(String email) {
        this.code = generateCode();
        this.email = email;
        return this.code;
    }
    
    public int generateCode() {
        Random random = new Random();
        int code = random.nextInt(9000) + 1000;
        return code;
    }

    public void clearService() {
        this.email = null;
        this.code = 0;
    }
}
