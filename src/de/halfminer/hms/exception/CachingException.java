package de.halfminer.hms.exception;

/**
 * Exception thrown by HanStorage, if the cache doesn't contain a chapter or cannot parse a file due to syntax error
 */
public class CachingException extends Exception {

    private final Reason reason;

    public CachingException(Reason reason) {
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        CANNOT_WRITE,
        CANNOT_READ,
        SYNTAX_ERROR,
        CHAPTER_NOT_FOUND,
        FILE_EMPTY
    }
}
