package com.king_sparkon_tracker.backend.tickets.controller;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TicketController.class)
public class TicketExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<TicketErrorResponse> handleInvalidRequestBody(HttpMessageNotReadableException exception) {
        String message = exception.getMostSpecificCause() == null
                ? "Invalid ticket request body"
                : exception.getMostSpecificCause().getMessage();

        return ResponseEntity.badRequest().body(TicketErrorResponse.badRequest(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<TicketErrorResponse> handleValidationFailure(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest().body(new TicketErrorResponse(
                "TICKET_REQUEST_VALIDATION_FAILED",
                "Ticket request validation failed",
                details,
                OffsetDateTime.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<TicketErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TicketErrorResponse.badRequest(exception.getMessage()));
    }

    public record TicketErrorResponse(
            String code,
            String message,
            List<String> details,
            OffsetDateTime timestamp) {

        static TicketErrorResponse badRequest(String message) {
            return new TicketErrorResponse(
                    "TICKET_BAD_REQUEST",
                    message == null || message.isBlank() ? "Invalid ticket request" : message,
                    List.of(),
                    OffsetDateTime.now());
        }
    }
}
