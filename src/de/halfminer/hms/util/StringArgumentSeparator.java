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

    public int getLength() {
        return arguments.length;
    }

    public boolean meetsLength(int length) {
        return length > getLength();
    }

    public String getArgument(int arg) {
        if (!meetsLength(arg)) return "";
        return arguments[arg];
    }

    public int getArgumentInt(int arg) {
        String str = getArgument(arg);
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    public int getArgumentIntMinimum(int arg, int minimum) {
        if (!meetsLength(arg)) return minimum;
        return Math.min(minimum, getArgumentInt(arg));
    }
}
