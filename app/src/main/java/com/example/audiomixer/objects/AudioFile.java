package com.example.audiomixer.objects;

import com.example.audiomixer.utils.TimeUtility;

import java.io.File;
import java.util.Locale;
import java.util.Objects;

public class AudioFile {
    private String title;
    private String artist;
    private String album;
    private long duration;
    private byte[] albumCover;
    private String filePath;
    private long createdAt;

    public AudioFile(String title, String artist, String album, long duration, String filePath, byte[] albumCover) {
        this.title = Objects.requireNonNullElse(title, "Unknown");
        this.artist = Objects.requireNonNullElse(artist, "Unknown");
        this.album = Objects.requireNonNullElse(album, "Unknown");
        this.duration = duration;
        this.filePath = Objects.requireNonNull(filePath);
        this.albumCover = albumCover;

        // Get time the file was created
        File file = new File(filePath);
        if (file.exists()) {
            this.createdAt = file.lastModified();
        } else {
            this.createdAt = System.currentTimeMillis(); // fallback to current time
        }
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
