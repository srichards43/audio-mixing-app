package com.example.audiomixer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.audiomixer.R;

public class AppPreferences {
    private static final String PREFS_NAME = "AudioMixerPrefs";
    private static final String THEME_INDEX_KEY = "theme_index";
    private static final String DIRECTORY_URI_KEY = "musicDirectoryUri";

    public static int getThemeIndex(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(THEME_INDEX_KEY, 0);
    }

    public static void setThemeIndex(Context context, int index) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(THEME_INDEX_KEY, index).apply();
    }

    public static void applyTheme(Context context) {
        int index = getThemeIndex(context);
        switch (index) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }

    public static void setMusicDirectoryUri(Context context, Uri uri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(DIRECTORY_URI_KEY, uri.toString()).apply();
    }

    public static Uri getMusicDirectoryUri(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(DIRECTORY_URI_KEY, null);
        if (uriString == null) {
            return null;
        } else {
            return Uri.parse(uriString);
        }
    }
}
