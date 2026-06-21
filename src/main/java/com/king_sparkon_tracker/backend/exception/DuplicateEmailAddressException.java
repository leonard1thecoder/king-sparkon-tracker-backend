package com.king_sparkon_tracker.backend.exception;

public class DuplicateEmailAddressException extends RuntimeException {

	public DuplicateEmailAddressException(String emailAddress) {
		super("Email address already exists: " + emailAddress);
	}
}
