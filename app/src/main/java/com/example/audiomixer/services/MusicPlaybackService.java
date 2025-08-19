package com.example.audiomixer.services;

import static android.app.PendingIntent.getActivity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;


import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.audiomixer.R;
import com.example.audiomixer.activities.MainActivity;
import com.example.audiomixer.objects.AudioFile;

public class MusicPlaybackService extends Service {
    private ExoPlayer player;
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private final IBinder binder = new LocalBinder();
    String channelId = "music_channel";


    public class LocalBinder extends Binder {
        public MusicPlaybackService getService() {
            return MusicPlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialise ExoPlayer and MediaSession
        player = new ExoPlayer.Builder(this).build();
        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        // Set actions for android compatibility
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_STOP
                );

        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setActive(true);

        player.addListener(new ExoPlayer.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                int state = -1;
                if (isPlaying) {
                    state = PlaybackStateCompat.STATE_PLAYING;
                } else {
                    state = PlaybackStateCompat.STATE_PAUSED;
                }

                // Update session
                mediaSession.setPlaybackState(
                        new PlaybackStateCompat.Builder()
                                .setState(state, player.getCurrentPosition(), 1f)
                                .build()
                );
            }
        });

        NotificationManager manager = getSystemService(NotificationManager.class);
        // Create channel if android version is >= Oreo and doesn't exist yet
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(channelId) == null) {
                NotificationChannel channel = new NotificationChannel(
                        channelId,                // Channel ID
                        "Music Playback",         // Channel name
                        NotificationManager.IMPORTANCE_LOW
                );
                manager.createNotificationChannel(channel);
            }
        }
    }
    public MusicPlaybackService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // todo: TEMP NOT WORKING Placeholder notification until song played
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Music Player")
                .setContentText("Waiting for song...")
                .setSmallIcon(R.drawable.ic_player_icon)
                .setOngoing(true)
                .build();

        startForeground(1, notification);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // Create notification for song playing
    private Notification createNotification(AudioFile song) {
        NotificationChannel channel = null;

        // Create intent to open app when notification clicked
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, "music_channel")
                .setContentTitle(song.getTitle())
                .setContentText(song.getArtist())
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_player_icon)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    // Play song from audioFile
    public void play(AudioFile song) {
        player.setMediaItem(MediaItem.fromUri(song.getFilePath()));
        player.prepare();
        player.play();

        Notification notification = createNotification(song);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(1, notification);
    }

    public void pause() {
        player.pause();
    }

    public void stop() {
        player.stop();
        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.release();
        mediaSession.release();
    }
}