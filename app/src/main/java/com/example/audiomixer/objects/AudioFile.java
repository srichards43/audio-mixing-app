package com.example.audiomixer.objects;

import java.util.Locale;
import java.util.Objects;

public class AudioFile {
    private String title;
    private String artist;
    private String album;
    private long duration;
    private byte[] albumCover;
    private String filePath;

    public AudioFile(String title, String artist, String album, long duration, String filePath, byte[] albumCover) {
        this.title = Objects.requireNonNullElse(title, "Unknown");
        this.artist = Objects.requireNonNullElse(artist, "Unknown");
        this.album = Objects.requireNonNullElse(album, "Unknown");
        this.duration = duration;
        this.filePath = Objects.requireNonNull(filePath);
        this.albumCover = albumCover;
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
        long minutes = duration / 60000;
        long seconds = (duration % 60000) / 1000;
        return String.format(Locale.UK, "%d:%02d", minutes, seconds);
    }

    public String getFilePath() {
        return filePath;
    }

    public byte[] getAlbumCover() {
        return albumCover;
    }
}
