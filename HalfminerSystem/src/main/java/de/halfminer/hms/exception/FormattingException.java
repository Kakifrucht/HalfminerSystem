package de.halfminer.hms.exception;

/**
 * Exception thrown when user input (config) is not in expected format
 */
public class FormattingException extends Exception {

    private final String message;

    public FormattingException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
