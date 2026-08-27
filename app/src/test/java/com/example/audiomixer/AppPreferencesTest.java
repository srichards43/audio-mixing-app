package com.example.audiomixer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.audiomixer.utils.AppPreferences;

import org.junit.Before;
import org.junit.Test;

public class AppPreferencesTest {
    private Context context;
    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;

    @Before
    public void setup() {
        context = mock(Context.class);
        sharedPrefs = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);

        when(editor.clear()).thenReturn(editor);

        // Block applying changes to stop crashes
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPrefs);
        when(sharedPrefs.edit()).thenReturn(editor);
        when(editor.putInt(anyString(), anyInt())).thenReturn(editor);
    }

    @Test
    public void testApplyColor_Green_SetsCorrectTheme() {
        when(sharedPrefs.getInt(eq("color_index"), anyInt())).thenReturn(2);

        AppPreferences.applyColor(context);
        verify(context).setTheme(R.style.Theme_AudioMixer_Green);
    }

    @Test
    public void testResetAll_TellsDbToClear() {
        AppPreferences.resetAll(context);

        verify(editor).clear();
        verify(editor).apply();
    }

    @Test
    public void testGetMusicDirectoryUri_ReturnsNullWhenEmpty() {
        when(sharedPrefs.getString(eq("music_directory_uri"), any())).thenReturn(null);

        android.net.Uri result = AppPreferences.getMusicDirectoryUri(context);
        assertEquals(null, result);
    }

}
