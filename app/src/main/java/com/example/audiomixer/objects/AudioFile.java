package com.example.audiomixer.objects;

import com.example.audiomixer.utils.TimeUtility;

import java.io.File;
import java.util.Objects;

public class AudioFile {
    private final String title;
    private final String artist;
    private final String album;
    private final long duration;
    private final byte[] albumCover;
    private final String filePath;
    private final long createdAt;

    public AudioFile(String title, String artist, String album, long duration, String filePath, byte[] albumCover, long createdAt) {
        this.title = Objects.requireNonNullElse(title, "Unknown");
        this.artist = Objects.requireNonNullElse(artist, "Unknown");
        this.album = Objects.requireNonNullElse(album, "Unknown");
        this.duration = duration;
        this.filePath = Objects.requireNonNull(filePath);
        this.albumCover = albumCover;
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public long getDuration() {
        return duration;
    }

    // Return the duration in min:sec format
    public String getFormattedDuration() {
        return TimeUtility.getFormattedDuration(duration);
    }

    public String getFilePath() {
        return filePath;
    }
    public long getCreatedAt() { return createdAt; }

    public byte[] getAlbumCover() {
        return albumCover;
    }
}
