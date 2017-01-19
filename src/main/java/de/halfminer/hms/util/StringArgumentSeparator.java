package de.halfminer.hms.util;

/**
 * Helper class to parse a String into arguments and easily convert between different types.
 */
@SuppressWarnings("ALL")
public class StringArgumentSeparator {

    private final String[] arguments;

    public StringArgumentSeparator(String string) {
        arguments = string.split(" ");
    }

    public StringArgumentSeparator(String string, char separator) {
        arguments = string.split(separator + "");
    }

    public StringArgumentSeparator(String[] strings) {
        arguments = strings;
    }

    public String[] getArguments() {
        return arguments;
    }

    public int getLength() {
        return arguments.length;
    }

    public boolean meetsLength(int length) {
        return getLength() >= length;
    }

    public String getArgument(int arg) {
        if (!meetsLength(arg + 1)) return "";
        return arguments[arg];
    }

    /**
     * Gets the argument as integer.
     *
     * @param arg argument to get
     * @return parsed integer or Integer.MIN_VALUE if couldn't be parsed
     */
    public int getArgumentInt(int arg) {
        String str = getArgument(arg).replaceAll(" ", "");
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    public int getArgumentIntMinimum(int arg, int minimum) {
        if (!meetsLength(arg + 1)) return minimum;
        return Math.max(minimum, getArgumentInt(arg));
    }
}
