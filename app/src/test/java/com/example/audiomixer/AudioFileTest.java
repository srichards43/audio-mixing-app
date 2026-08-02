package com.example.audiomixer;

import static org.junit.Assert.assertEquals;

import com.example.audiomixer.objects.AudioFile;

import org.junit.Test;

public class AudioFileTest {

    // Check formatting and service method returns for a valid file
    @Test
    public void testAudioFile_ValidData() {
        byte[] dummyCover = new byte[]{1, 2, 3};
        AudioFile file = new AudioFile("My Song", "My Artist", "My Album", 5000, "path/to/song.mp3", dummyCover, 123456789L);

        assertEquals("My Song", file.getTitle());
        assertEquals("My Artist", file.getArtist());
        assertEquals("My Album", file.getAlbum());
        assertEquals(5000, file.getDuration());
        assertEquals("path/to/song.mp3", file.getFilePath());
        assertEquals(dummyCover, file.getAlbumCover());
        assertEquals(123456789L, file.getCreatedAt());
    }

    // Verify that passing nulls to constructor defaults to Unknown
    @Test
    public void testAudioFile_NullData() {
        AudioFile file = new AudioFile(null, null, null, 1000, "test/path", null, 0);

        assertEquals("Unknown", file.getTitle());
        assertEquals("Unknown", file.getArtist());
        assertEquals("Unknown", file.getAlbum());
    }

    // Assert that long ms converts to formatted string
    @Test
    public void testAudioFile_FormattedDuration() {
        AudioFile file = new AudioFile("Title", "Artist", "Album", 61000, "path", null, 0);
        assertEquals("1:01", file.getFormattedDuration());
    }

}
