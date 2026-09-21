package com.example.audiomixer.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.Size;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AlbumCoverLoader {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    // Async wrapper for getArt
    public static void load(Context context, String path, Consumer<Bitmap> callback) {
        executor.submit(() -> {
            Bitmap bitmap = getArt(context, path);
            if (callback != null) {
                callback.accept(bitmap);
            }
        });
    }

    // Gets album cover of an audio file, trying to use mediastore before falling back to metadata retriever
    public static Bitmap getArt(Context context, String path) {
        if (path == null) return null;

        Uri uri = Uri.parse(path);

        // Get image from mediaStore if possible, more performant
        if (path.startsWith("content://") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Bitmap thumb = context.getContentResolver().loadThumbnail(uri, new Size(300, 300), null);
                if (thumb != null) return thumb;
            } catch (Exception ignored) {
                // Fall through if fail
            }
        }

        // Manually extract metadata
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            if (path.startsWith("asset:///")) {
                // If ambient then use asset file descriptor
                String assetPath = path.replace("asset:///", "");
                try (android.content.res.AssetFileDescriptor afd = context.getAssets().openFd(assetPath)) {
                    retriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                }
            } else {
                retriever.setDataSource(context, uri);
            }
            
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                return BitmapFactory.decodeByteArray(art, 0, art.length);
            }
        } catch (Exception ignored) { }

        return null;
    }
}
