package com.example.audiomixer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.appcompat.app.AppCompatDelegate;

public class AppPreferences {
    private static final String PREFS_NAME = "AudioMixerPrefs";
    private static final String THEME_INDEX_KEY = "theme_index";
    private static final String LAUNCH_TAB_KEY = "launch_tab";
    private static final String MUSIC_DIRECTORY_URI_KEY = "music_directory_uri";
    private static final String AMBIENT_DIRECTORY_URI_KEY = "ambient_directory_uri";
    private static final String AMBIENT_DISC_ROTATION_KEY = "ambient_disc_rotation";

    public static int getThemeIndex(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(THEME_INDEX_KEY, 0);
    }

    public static void setThemeIndex(Context context, int index) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(THEME_INDEX_KEY, index).apply();
    }

    public static int getLaunchTab(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(LAUNCH_TAB_KEY, 1);
    }

    public static void setLaunchTab(Context context, int index) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(LAUNCH_TAB_KEY, index).apply();
    }


    public static void setAmbientDiscRotation(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(AMBIENT_DISC_ROTATION_KEY, enabled).apply();
    }

    public static boolean getAmbientDiscRotation(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(AMBIENT_DISC_ROTATION_KEY, true);
    }

    /**
     * Apply light/dark mode to app based on preferred theme index
     * @param context context of activity to be applied
     */
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
        prefs.edit().putString(MUSIC_DIRECTORY_URI_KEY, uri.toString()).apply();
    }

    public static Uri getMusicDirectoryUri(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(MUSIC_DIRECTORY_URI_KEY, null);
        if (uriString == null) {
            return null;
        } else {
            return Uri.parse(uriString);
        }
    }

    public static void setAmbientDirectoryUri(Context context, Uri uri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(AMBIENT_DIRECTORY_URI_KEY, uri.toString()).apply();
    }

    public static Uri getAmbientDirectoryUri(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(AMBIENT_DIRECTORY_URI_KEY, null);
        if (uriString == null) {
            return null;
        } else {
            return Uri.parse(uriString);
        }
    }

    public static void resetAll(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}
