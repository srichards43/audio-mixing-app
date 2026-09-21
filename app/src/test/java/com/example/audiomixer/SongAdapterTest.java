package com.example.audiomixer;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

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
public class SongAdapterTest {
    private SongAdapter adapter;
    private List<AudioFile> testSongs;

    @Before
    public void setup() {
        Context context = mock(Context.class);
        Resources.Theme theme = mock(Resources.Theme.class);
        when(context.getTheme()).thenReturn(theme);

        // Mock the color lookup so the adapter doesn't crash
        when(theme.resolveAttribute(anyInt(), any(TypedValue.class), anyBoolean())).thenReturn(true);

        testSongs = Arrays.asList(
                new AudioFile("Red", "Artist A", "Album X", 100, "path1", 2000),
                new AudioFile("Blue", "Artist B", "Album Y", 200, "path2", 1000)
        );

        // Spy on adapter and ignore notifyDatasetChanged
        adapter = spy(new SongAdapter(testSongs, position -> {}, context));
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

    @Test
    public void filterSongs_SearchQuery_FiltersCorrectItems() {
        adapter.filterSongs("Blue", "Title", true);
        assertEquals(1, adapter.getItemCount());
        assertEquals("Blue", adapter.getFilteredSongs().get(0).getTitle());
    }

    @Test
    public void filterSongs_NoMatches_ReturnsEmptyList() {
        adapter.filterSongs("qwerty", "Title", true);
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void sortSongs_ByTitle_SortsAlphabetically() {
        adapter.sortSongs("Title", true);
        assertEquals("Blue", adapter.getFilteredSongs().get(0).getTitle());
        assertEquals("Red", adapter.getFilteredSongs().get(1).getTitle());
    }

    @Test
    public void sortSongs_ByDurationDescending_ReversesOrder() {
        adapter.sortSongs("Duration", false); // Descending
        assertEquals(200L, adapter.getFilteredSongs().get(0).getDuration());
        assertEquals(100L, adapter.getFilteredSongs().get(1).getDuration());
    }

}
