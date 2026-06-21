package com.king_sparkon_tracker.backend.exception;

public class DuplicateBarcodeException extends RuntimeException {

	public DuplicateBarcodeException(String barcode) {
		super("Barcode already exists: " + barcode);
	}
}
