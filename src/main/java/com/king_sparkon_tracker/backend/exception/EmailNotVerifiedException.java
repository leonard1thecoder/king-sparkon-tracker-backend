package com.king_sparkon_tracker.backend.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Email address is not verified. Please verify your email before logging in.");
    }
}
