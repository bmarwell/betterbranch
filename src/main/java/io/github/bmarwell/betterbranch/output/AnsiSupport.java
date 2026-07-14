package io.github.bmarwell.betterbranch.output;

import java.util.Locale;
import java.util.Map;

public final class AnsiSupport {

    private AnsiSupport() {
        /* utility class */
    }

    /** Returns {@code true} if the current environment is very likely to support ANSI escape sequences. */
    public static boolean supportsAnsi() {
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase(Locale.ROOT).contains("win")) {
            return false;
        }

        if (System.console() == null) {
            return false;
        }

        Map<String, String> env = System.getenv();
        String term = env.get("TERM");
        if (term == null) {
            // TERM not set → probably not a capable terminal
            return false;
        }

        term = term.toLowerCase(Locale.ROOT);
        // Common values that indicate no colour support
        if (term.equals("dumb") || term.contains("no") || term.contains("unknown")) {
            return false;
        }

        // Anything else (xterm, vt100, screen, linux, ansi, cygwin, etc.) is usually fine.
        return true;
    }
}
