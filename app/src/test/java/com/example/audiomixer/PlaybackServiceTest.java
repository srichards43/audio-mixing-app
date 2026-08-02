package com.example.audiomixer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.audiomixer.objects.AudioFile;
import com.example.audiomixer.services.PlaybackService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.util.ArrayList;
import java.util.List;

public class PlaybackServiceTest {

    // Avoid crashing with threads by performing task instantly
    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    private PlaybackService service;
    private ExoPlayer mockPlayer;

    @Before
    public void setup() {
        service = new PlaybackService();
        mockPlayer = mock(ExoPlayer.class);
        service.setPlayer(mockPlayer);

        // Dummy data
        List<AudioFile> songs = new ArrayList<>();
        songs.add(new AudioFile("Song A", "Artist", "Album", 0, "pathA", null, 1000));
        songs.add(new AudioFile("Song B", "Artist", "Album", 0, "pathB", null, 2000));
        songs.add(new AudioFile("Song C", "Artist", "Album", 0, "pathC", null, 3000));

        service.setPlaylist(songs);
    }


    @Test
    public void upNext_generalTest() {
        when(mockPlayer.getCurrentMediaItemIndex()).thenReturn(1); // mock playing 2nd song

        List<AudioFile> upNext = service.getUpNext();

        assertEquals(2, upNext.size());
        assertEquals("Song B", upNext.get(0).getTitle());
    }

    @Test
    public void shufflePlaylist_preservesCurrentSongIndex() {
        when(mockPlayer.getCurrentMediaItemIndex()).thenReturn(1);
        when(mockPlayer.getMediaItemCount()).thenReturn(3);
        service.shufflePlaylist();

        assertEquals("Song B", service.getPlaylist().get(1).getTitle()); // Check current song is still at index 1

        // Verify ExoPlayer was updated for the before and after segments
        verify(mockPlayer).removeMediaItems(eq(0), eq(1));
        verify(mockPlayer).addMediaItems(eq(0), anyList());
    }

    @Test
    public void moveItemInQueue_updateListAndPlayer() {
        // Move song at index 0 to index 2
        service.moveItemInQueue(0, 2);

        assertEquals("Song A", service.getPlaylist().get(2).getTitle());
        verify(mockPlayer).moveMediaItem(0, 2);
    }

    @Test
    public void setLoop_mapToExoPlayerModes() {
        service.setLoop(1);
        verify(mockPlayer).setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);

        service.setLoop(2);
        verify(mockPlayer).setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
    }
}
