package de.halfminer.hms.exception;

import de.halfminer.hms.util.Language;

/**
 * Exception thrown by HanStorage, if the cache doesn't contain a chapter or cannot parse a file due to syntax error
 */
public class CachingException extends Exception {

    private final Reason reason;
    private final int lineNumber;

    public CachingException(Reason reason) {
        this.reason = reason;
        this.lineNumber = Integer.MIN_VALUE;
    }

    public CachingException(Reason reason, int lineNumber) {
        this.reason = reason;
        this.lineNumber = lineNumber;
    }

    public Reason getReason() {
        return reason;
    }

    public String getCleanReason() {

        String cleanString = Language.makeStringFriendly(reason.name());
        if (lineNumber > 0) cleanString += " (l" + lineNumber + ")";
        return cleanString;
    }

    public enum Reason {
        CANNOT_WRITE,
        CANNOT_READ,
        SYNTAX_ERROR,
        CHAPTER_NOT_FOUND,
        FILE_EMPTY
    }
}
