package com.example.audiomixer.utils;

import java.util.Locale;

public class TimeUtility {

    /**
     * Helper method to convert time of type long into readable time
     * @param duration, in milliseconds
     * @return formatted time
     */
    public static String getFormattedDuration(long duration) {
        long minutes = duration / 60000;
        long seconds = (duration % 60000) / 1000;
        return String.format(Locale.UK, "%d:%02d", minutes, seconds);
    }
}
