package tala.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * DateUtil
 * ------------------------------------------------------------
 * Reading and showing dates.
 *   Typed by the user : 2026-11-03   (YYYY-MM-DD)
 *   Shown in tables   : Nov 3, 2026
 */
public final class DateUtil {

    // "uuuu" + STRICT means 2026-02-30 is rejected instead of quietly becoming Feb 28
    private static final DateTimeFormatter INPUT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);

    private DateUtil() { }

    /** "2026-11-03" -> LocalDate, or null if the text is not a real date */
    public static LocalDate parse(String text) {
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), INPUT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** LocalDate -> "2026-11-03" (for text fields) */
    public static String toInput(LocalDate date) {
        return date.format(INPUT);
    }

    /** LocalDate -> "Nov 3, 2026" */
    public static String toLong(LocalDate date) {
        return date == null ? "" : date.format(LONG);
    }

    /** LocalDate -> "Nov 3" */
    public static String toShort(LocalDate date) {
        return date == null ? "" : date.format(SHORT);
    }

    /** LocalDate -> "November 2026" */
    public static String toMonth(LocalDate date) {
        return date.format(MONTH);
    }
}
