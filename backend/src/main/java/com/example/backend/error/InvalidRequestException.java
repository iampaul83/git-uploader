package com.example.backend.error;

public class InvalidRequestException extends RuntimeException {

        public InvalidRequestException(String message) {
                super(message);
        }
}
