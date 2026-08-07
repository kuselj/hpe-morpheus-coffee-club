package com.hpe.morpheus.coffeeclub.service;

import java.util.Locale;

/**
 * Name handling rules shared by pre-population, balance aggregation and payer selection.
 *
 * <p>Names are stored exactly as the user typed them (after trimming), but they are matched
 * case-insensitively and ignoring repeated whitespace, so that "bob", "Bob" and "Bob " all resolve
 * to the same coworker's lifetime history.</p>
 */
public final class Names {

    private Names() {
    }

    /** Trims and collapses internal whitespace. Returns {@code null} for a {@code null} input. */
    public static String normalise(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    /** Case-insensitive lookup key for a name. Returns an empty string for a {@code null} input. */
    public static String key(String raw) {
        String normalised = normalise(raw);
        return normalised == null ? "" : normalised.toLowerCase(Locale.ROOT);
    }
}
