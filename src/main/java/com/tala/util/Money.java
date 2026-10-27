// Package for small helper classes used everywhere.
package com.tala.util;

// BigDecimal = exact decimal numbers. Always use it for money; double/float cause rounding errors (0.1 + 0.2 != 0.3).
import java.math.BigDecimal;
// RoundingMode = rules for rounding (half-up, down, etc.).
import java.math.RoundingMode;

/** Formatting and parsing of peso amounts. */
public final class Money {
    // Private constructor: this is a static-only helper class, never instantiated.
    private Money() {}

    // Turns a number into display text, e.g. 1500 -> "₱1500.00".
    // setScale(2, HALF_UP) forces exactly 2 decimal places, rounding .5 upward (normal school rounding).
    // toPlainString() prevents scientific notation like "1E+3".
    public static String currency(BigDecimal amount) {
        return "₱" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Parses an amount with at most two decimals; throws if it has more. */
    public static BigDecimal parse(String text) {
        // new BigDecimal(text) converts "250.5" into a number (throws NumberFormatException if it's not a number).
        // setScale(2, UNNECESSARY) means: "make it 2 decimals, and if that would require rounding, throw an error".
        // So "250.5" -> 250.50 is fine, but "250.567" throws ArithmeticException (rejected on purpose).
        return new BigDecimal(text).setScale(2, RoundingMode.UNNECESSARY);
    }
}
