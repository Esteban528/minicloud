package com.estebandev.minicloud.service.exception;

public class UserAlreadyExistsException extends Exception{
   public UserAlreadyExistsException(String message) {
        super(message);
    } 
}
