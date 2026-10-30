// This class lives in the util package (small helper tools).
package com.tala.util;

// BigDecimal stores exact decimal numbers (never use double for money); RoundingMode controls how we round.
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formatting and parsing of peso amounts. */
public final class Money {
    // Private constructor: this is a utility class with only static methods.
    private Money() {}

    // Turns a number into display text such as "₱1500.00".
    public static String currency(BigDecimal amount) {
        // Force exactly 2 decimals (round half up, like normal rounding), then add the peso sign in front.
        return "₱" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Parses an amount with at most two decimals; throws if it has more. */
    public static BigDecimal parse(String text) {
        // Convert text to a number with 2 decimals; UNNECESSARY means an ArithmeticException if rounding would be needed (e.g. "1.234").
        return new BigDecimal(text).setScale(2, RoundingMode.UNNECESSARY);
    }
}
