package de.halfminer.hms.exception;

/**
 * Exception thrown when user input causes
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
