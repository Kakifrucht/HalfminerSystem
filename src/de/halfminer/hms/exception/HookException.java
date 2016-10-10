package de.halfminer.hms.exception;

/**
 * Thrown if a plugin wasn't hooked
 */
public class HookException extends Exception {

    private final Exception parent;

    public HookException() {
        parent = null;
    }

    public HookException(Exception parent) {
        this.parent = parent;
    }

    /**
     * Check if this Exception was caused by another exception
     * @return true if hook caused another exception, false if plugin not properly hooked
     */
    public boolean hasParentException() {
        return parent != null;
    }

    public Exception getParentException() {
        return parent;
    }

    @Override
    public void printStackTrace() {
        if (hasParentException()) parent.printStackTrace();
        else super.printStackTrace();
    }
}
