package io.github.bmarwell.betterbranch.output;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

final class HumanReadableTime {

    private HumanReadableTime() {
        /* util class */
    }

    public static String formatElapsed(Instant from, Instant to) {
        return formatElapsed(from, to, ZoneId.systemDefault());
    }

    public static String formatElapsed(Instant from, Instant to, ZoneId zone) {
        ZonedDateTime zFrom = from.atZone(zone);
        ZonedDateTime zTo = to.atZone(zone);

        Period period = Period.between(zFrom.toLocalDate(), zTo.toLocalDate());
        long years = period.getYears();
        long months = period.getMonths();
        long days = period.getDays();

        Duration time = Duration.between(zFrom.toLocalTime(), zTo.toLocalTime());
        long hours = time.toHours();
        long minutes = time.toMinutes() % 60;
        long seconds = time.getSeconds() % 60;

        List<String> parts = new ArrayList<>();

        if (years > 0) {
            parts.add(plural(years, "year"));
            if (months > 0) {
                parts.add(plural(months, "month"));
            }
        } else if (months > 0) {
            parts.add(plural(months, "month"));
            if (days > 0) {
                parts.add(plural(days, "day"));
            }
        } else if (days > 0) {
            parts.add(plural(days, "day"));
            if (hours > 0) {
                parts.add(plural(hours, "hour"));
            }
        } else if (hours > 0) {
            parts.add(plural(hours, "hour"));
            if (minutes > 0) {
                parts.add(plural(minutes, "minute"));
            }
        } else if (minutes > 0) {
            parts.add(plural(minutes, "minute"));
            if (seconds > 0) {
                parts.add(plural(seconds, "second"));
            }
        } else {
            parts.add(plural(seconds, "second"));
        }

        String joined = String.join(", ", parts);

        return joined + " ago";
    }

    private static String plural(long value, String singular) {
        return value + " " + (value == 1 ? singular : singular + "s");
    }
}
