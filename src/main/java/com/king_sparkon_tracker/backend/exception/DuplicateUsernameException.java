package com.king_sparkon_tracker.backend.exception;

public class DuplicateUsernameException extends RuntimeException {

	public DuplicateUsernameException(String username) {
		super("Username already exists: " + username);
	}
}
