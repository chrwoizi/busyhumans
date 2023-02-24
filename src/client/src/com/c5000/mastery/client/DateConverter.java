package com.c5000.mastery.client;

import java.util.Date;

public class DateConverter {

    private static long SECOND = 1000;
    private static long MINUTE = 60 * SECOND;
    private static long HOUR = 60 * MINUTE;
    private static long DAY = 24 * HOUR;
    private static long WEEK = 7 * DAY;
    private static long MONTH = 30 * DAY;
    private static long YEAR = 365 * DAY;

    public static String toNaturalFormatFuture(Date date) {
        long deltaMs = date.getTime() - Sync.serverTime().getTime();
        return toNaturalFormat(deltaMs);
    }

    public static String toNaturalFormatPast(Date date) {
        long deltaMs = Sync.serverTime().getTime() - date.getTime();
        return toNaturalFormat(deltaMs);
    }

    private static String toNaturalFormat(long deltaMs) {
        if (deltaMs < 2 * MINUTE)
            return convert(deltaMs, SECOND) + " seconds";
        else if (deltaMs < 2 * HOUR)
            return convert(deltaMs, MINUTE) + " minutes";
        else if (deltaMs < 2 * DAY)
            return convert(deltaMs, HOUR) + " hours";
        else if (deltaMs < 2 * WEEK)
            return convert(deltaMs, DAY) + " days";
        else if (deltaMs < 2 * MONTH)
            return convert(deltaMs, WEEK) + " weeks";
        else if (deltaMs < 2 * YEAR)
            return convert(deltaMs, MONTH) + " months";
        return convert(deltaMs, YEAR) + " years";
    }

    private static long convert(long deltaMs, long unit) {
        return Math.round(deltaMs / (double) unit);
    }
}
