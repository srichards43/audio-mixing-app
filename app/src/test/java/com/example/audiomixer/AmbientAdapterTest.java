package com.example.audiomixer;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import com.example.audiomixer.adapters.AmbientAdapter;
import com.example.audiomixer.adapters.SongAdapter;
import com.example.audiomixer.objects.AudioFile;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;
public class AmbientAdapterTest {
    private AmbientAdapter adapter;
    private List<AudioFile> testAmbients;

    @Before
    public void setup() {
        Context context = mock(Context.class);
        Resources.Theme theme = mock(Resources.Theme.class);
        when(context.getTheme()).thenReturn(theme);

        // Mock the color lookup so the adapter doesn't crash
        when(theme.resolveAttribute(anyInt(), any(TypedValue.class), anyBoolean())).thenReturn(true);

        testAmbients = Arrays.asList(
                new AudioFile("Rain", "", "", 0, "path1", null, 0),
                new AudioFile("Waves", "", "", 0, "path2", null, 0)
        );

        // Spy on adapter and ignore notifyDatasetChanged
        adapter = spy(new AmbientAdapter(testAmbients, position -> {}, context));
        doNothing().when(adapter).notifyDataSetChanged();
        doNothing().when(adapter).notifyItemChanged(anyInt());
    }

    @Test
    public void testSetCurrentlyPlaying_UpdatesOldAndNew() {
        adapter.setCurrentlyPlaying("path1");
        // Verify updates new
        verify(adapter).notifyItemChanged(0);

        adapter.setCurrentlyPlaying("path2");
        // Verify updates both new and old
        verify(adapter, times(2)).notifyItemChanged(0);
        verify(adapter).notifyItemChanged(1);
    }

}
