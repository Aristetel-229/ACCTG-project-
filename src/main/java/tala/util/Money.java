package tala.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Money
 * ------------------------------------------------------------
 * Helpers for peso amounts.
 *
 * We ALWAYS use BigDecimal for money, never double:
 *   0.1 + 0.2 as double  = 0.30000000000000004  (wrong for accounting)
 *   0.1 + 0.2 BigDecimal = 0.3                  (exact)
 */
public final class Money {

    /** The peso sign. Theme switches it to "PHP " if the font cannot draw it. */
    public static String PESO_SIGN = "\u20B1";

    private Money() { }   // only static helpers here

    /** 15000 -> "15,000.00" */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        // A new DecimalFormat each time keeps this thread-safe and simple
        DecimalFormat format = new DecimalFormat("#,##0.00");
        return format.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    /** 15000 -> "P15,000.00",  -500 -> "-P500.00" */
    public static String peso(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        if (amount.signum() < 0) {
            return "-" + PESO_SIGN + format(amount.abs());
        }
        return PESO_SIGN + format(amount);
    }

    /** Empty text for null or zero, otherwise "15,000.00" (used in tables) */
    public static String formatOrBlank(BigDecimal amount) {
        if (amount == null || amount.signum() == 0) {
            return "";
        }
        return format(amount);
    }

    /**
     * Reads what the user typed ("15,000.50", "P300") as a BigDecimal.
     * Throws IllegalArgumentException with a friendly message if it is not a valid amount.
     */
    public static BigDecimal parse(String text) {
        String cleaned = text == null ? "" : text.trim()
                .replace(",", "")
                .replace(PESO_SIGN, "")
                .replace("\u20B1", "")
                .replace("PHP", "")
                .trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Enter an amount.");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The amount must be a number, like 1500 or 1,500.00.");
        }
        if (value.scale() > 2 && value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Use at most 2 decimal places (centavos).");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Safe "is this zero?" check (BigDecimal.equals would treat 0 and 0.00 as different) */
    public static boolean isZero(BigDecimal amount) {
        return amount == null || amount.signum() == 0;
    }
}
