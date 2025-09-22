package com.example.backend.error;

public class RepoNotFoundException extends RuntimeException {

        public RepoNotFoundException(String message) {
                super(message);
        }
}
