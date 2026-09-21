package com.example.audiomixer.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.example.audiomixer.objects.AudioFile;

import java.util.ArrayList;
import java.util.List;

public class MediaScanner {

    // Use existing system media store and create audio files
    public static List<AudioFile> scanDeviceForAudio(Context context) {
        List<AudioFile> audioFiles = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_MODIFIED
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.DATE_MODIFIED + " DESC";

        try (Cursor cursor = contentResolver.query(uri, projection, selection, null, sortOrder)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String album = cursor.getString(albumCol);
                    long duration = cursor.getLong(durationCol);
                    long dateModified = cursor.getLong(dateCol) * 1000L;

                    Uri contentUri = ContentUris.withAppendedId(uri, id);

                    audioFiles.add(new AudioFile(
                            title != null ? title : "Unknown",
                            artist != null ? artist : "Unknown",
                            album != null ? album : "",
                            duration,
                            contentUri.toString(),
                            dateModified
                    ));
                }
            }
        } catch (Exception e) {
            Log.e("MediaScanner", "MediaStore query failed", e);
        }

        return audioFiles;
    }
}
