package com.example.audiomixer.services;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;


import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.audiomixer.R;
import com.example.audiomixer.objects.AudioFile;

import java.util.ArrayList;
import java.util.List;

public class MusicPlaybackService extends Service {
    private ExoPlayer player;
    private MediaSessionCompat mediaSession;
    private final IBinder binder = new LocalBinder();
    private Handler handler;
    private Runnable updatePositionRunnable;
    private final List<AudioFile> playlist = new ArrayList<>();
    private final MutableLiveData<Long> currentPositionInSongInternal = new MutableLiveData<>();
    private final MutableLiveData<AudioFile> currentSongInternal = new MutableLiveData<>();
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
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_STOP
                );

        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setActive(true);

        // Listener to change PlaybackStates and whether to call songPos updates
        player.addListener(new ExoPlayer.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                int state;
                if (isPlaying) {
                    state = PlaybackStateCompat.STATE_PLAYING;
                    startUpdatingPositionInSong();
                } else {
                    state = PlaybackStateCompat.STATE_PAUSED;
                    stopUpdatingPositionInSong();
                }

                // Update session
                mediaSession.setPlaybackState(
                        new PlaybackStateCompat.Builder()
                                .setState(state, player.getCurrentPosition(), 1f)
                                .build()
                );
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                if (mediaItem != null) {
                    int currentIndex = player.getCurrentMediaItemIndex();
                    AudioFile newSong = playlist.get(currentIndex);
                    onSongChanged(newSong);
                }
            }
        });

        NotificationManager manager = getSystemService(NotificationManager.class);

        // Create channel if android version is >= Oreo and doesn't exist yet (otherwise breaks)
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

        // Update position in song every 1 second
        handler = new Handler(Looper.getMainLooper());
        updatePositionRunnable = new Runnable() {
            @Override
            public void run() {
                if (player.isPlaying()) {
                    currentPositionInSongInternal.setValue(player.getCurrentPosition());
                    handler.postDelayed(this, 1000);
                }
            }
        };
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

    // Set a new playlist
    public void setPlaylist(List<AudioFile> newPlaylist) {
        playlist.clear();
        playlist.addAll(newPlaylist);
    }

    // Play song from audioFile
    public void play(int position) {
        if (position < 0 || position >= playlist.size()) {
            return;
        }

        List<MediaItem> mediaItems = new ArrayList<>();
        for(AudioFile song : playlist){
            mediaItems.add(MediaItem.fromUri(song.getFilePath()));
        }
        player.setMediaItems(mediaItems, position, 0);
        player.prepare();
        player.play();
    }

    public void pause() {
        player.pause();
    }

    public void resume() {
        player.play();
    }

    public void skipToNext() {
        player.seekToNext();
    }

    public void skipToPrevious() {
        player.seekToPrevious();
    }

    public void goToCurrentPosInSong(long pos) {
        player.seekTo(pos);
    }

    public void setSongVolume(float volume) { player.setVolume(volume); }

    public void setLoop(int state) {
        switch (state) {
            case 0:
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_OFF);
                break;
            case 1:
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                break;
            case 2:
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                break;
        }
    }

    public LiveData<Long> getCurrentPositionInSong() {
        return currentPositionInSongInternal;
    }

    public void startUpdatingPositionInSong() {
        handler.removeCallbacks(updatePositionRunnable);
        handler.post(updatePositionRunnable);
    }

    public void stopUpdatingPositionInSong() {
        handler.removeCallbacks(updatePositionRunnable);
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public AudioFile getCurrentSong() {
        int index = player.getCurrentMediaItemIndex();
        if (index >= 0 && index < playlist.size()) {
            return playlist.get(index);
        } else {
            // No songs
            return null;
        }
    }

    public List<AudioFile> getPlaylist() {
        return playlist;
    }

    public List<AudioFile> getUpNext() {
        int current = player.getCurrentMediaItemIndex();
        if (current == -1 || playlist.isEmpty()) return new ArrayList<>();

        return new ArrayList<>(playlist.subList(current, playlist.size()));
    }

    public void moveItemInQueue(int from, int to) {
        if (from >= 0 && from < playlist.size()) {
            if (to >= 0 && to < playlist.size()) {
                AudioFile item = playlist.remove(from);
                playlist.add(to, item);

                player.moveMediaItem(from, to);
                return;
            }
            throw new IllegalArgumentException("Invalid 'to' index");
        }
        throw new IllegalArgumentException("Invalid 'from' index");
    }


    public LiveData<AudioFile> getCurrentSongInternal() {
        return currentSongInternal;
    }

    public int getCurrentIndex() {
        return player.getCurrentMediaItemIndex();
    }

    private void onSongChanged(AudioFile newSong) {
        currentSongInternal.postValue(newSong);
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