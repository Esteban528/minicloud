package com.estebandev.minicloud.service.exception;

public class ManyAttempsException extends Exception {
    public ManyAttempsException(String message) {
        super(message);
    }
}
