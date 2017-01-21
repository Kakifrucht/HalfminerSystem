package de.halfminer.hms.exception;

import de.halfminer.hms.util.Utils;

/**
 * Exception thrown by HanStorage, if the cache doesn't contain a chapter or cannot parse a file due to syntax error
 */
@SuppressWarnings("SameParameterValue")
public class CachingException extends Exception {

    private final String fileName;
    private final Reason reason;
    private final int lineNumber;
    private final String chapterName;

    public CachingException(String fileName, Reason reason) {
        this.fileName = fileName;
        this.reason = reason;
        this.lineNumber = Integer.MIN_VALUE;
        this.chapterName = "";
    }

    public CachingException(String fileName, Reason reason, int lineNumber, String chapterName) {
        this.fileName = fileName;
        this.reason = reason;
        this.lineNumber = lineNumber;
        this.chapterName = chapterName;
    }

    public Reason getReason() {
        return reason;
    }

    public String getCleanReason() {
        String cleanString = Utils.makeStringFriendly(reason.name() + " in " + fileName);
        if (lineNumber > 0) cleanString += " (l" + lineNumber + ", " + chapterName + ")";
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
