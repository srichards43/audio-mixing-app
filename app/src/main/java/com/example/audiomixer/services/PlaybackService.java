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
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;


import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.audiomixer.R;
import com.example.audiomixer.objects.AudioFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaybackService extends Service {
    private ExoPlayer player;
    private ExoPlayer ambiencePlayer; // Low maintenance player for ambient sounds
    private MediaSessionCompat mediaSession;
    private final IBinder binder = new LocalBinder();
    private Handler handler;
    private Runnable updatePositionRunnable;
    private final List<AudioFile> playlist = new ArrayList<>();
    private final MutableLiveData<Long> currentPositionInSongInternal = new MutableLiveData<>();
    private final MutableLiveData<AudioFile> currentSongInternal = new MutableLiveData<>();
    private final MutableLiveData<AudioFile> currentAmbientInternal = new MutableLiveData<>();
    final String channelId = "music_channel";

    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    // Music timer state
    private long musicEndTime = 0;
    private long musicFadeDuration = 0;
    private float musicBaseVolume = 1.0f;
    private final MutableLiveData<Long> musicTimerRemaining = new MutableLiveData<>(null);
    private boolean isMusicTimerPaused = false;
    private long musicInitialDuration = 0;

    // Ambience timer state
    private long ambienceEndTime = 0;
    private long ambienceFadeDuration = 0;
    private float ambienceBaseVolume = 1.0f;
    private final MutableLiveData<Long> ambienceTimerRemaining = new MutableLiveData<>(null);
    private boolean isAmbienceTimerPaused = false;
    private long ambienceInitialDuration = 0;

    private final Runnable musicTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long remaining = musicEndTime - SystemClock.elapsedRealtime();

            // Break once time reaches 0 and reset state
            if (remaining <= 0) {
                player.pause();
                player.setVolume(musicBaseVolume);
                musicTimerRemaining.postValue(0L);
                return;
            }

            musicTimerRemaining.postValue(remaining);

            // Handle Fading
            if (remaining <= musicFadeDuration && musicFadeDuration > 0) {
                float fadeProgress = (float) remaining / musicFadeDuration;
                player.setVolume(musicBaseVolume * fadeProgress);
            }

            timerHandler.postDelayed(this, 1000); // Check again in 1 second
        }
    };

    private final Runnable ambienceTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long remaining = ambienceEndTime - SystemClock.elapsedRealtime();

            // Break once time reaches 0 and reset state
            if (remaining <= 0) {
                player.pause();
                player.setVolume(ambienceBaseVolume);
                ambienceTimerRemaining.postValue(0L);
                return;
            }

            ambienceTimerRemaining.postValue(remaining);

            // Handle Fading
            if (remaining <= ambienceFadeDuration && ambienceFadeDuration > 0) {
                float fadeProgress = (float) remaining / ambienceFadeDuration;
                player.setVolume(ambienceBaseVolume * fadeProgress);
            }

            timerHandler.postDelayed(this, 1000); // Check again in 1 second
        }
    };

    public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialise ExoPlayers
        player = new ExoPlayer.Builder(this).build();
        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        ambiencePlayer = new ExoPlayer.Builder(this).build();
        ambiencePlayer.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);

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

    // Kill players and service when app is closed.
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        if (player != null) player.stop();
        if (ambiencePlayer != null) ambiencePlayer.stop();

        stopForeground(true);
        stopSelf();
    }

    public PlaybackService() {
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
    public void play() {
        player.play();
    }

    /**
     * Jump to a song in the current playlist
     * @param position of song in playlist
     */
    public void seekToSong(int position) {
        if (position < 0 || position >= playlist.size()) {
            return;
        }

        player.seekTo(position, 0);
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
    public float getSongVolume() { return player.getVolume(); }


    /**
     * Toggle between looping state
     * @param state 0 = off, 1 = repeat current, 2 = repeat all
     */
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

    /**
     * Get song that is currently playing, return null if no songs
     */
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

    /**
     * Set a new playlist at a specified starting point
     * @param newPlaylist playlist to update with
     * @param startIndex index of song to start on
     */
    public void setPlaylist(List<AudioFile> newPlaylist, int startIndex) {
        playlist.clear();
        playlist.addAll(newPlaylist);

        List<MediaItem> mediaItems = new ArrayList<>();
        for (AudioFile song : playlist) {
            mediaItems.add(MediaItem.fromUri(song.getFilePath()));
        }

        player.setMediaItems(mediaItems, startIndex, 0);
        player.prepare();
    }

    /**
     * Retrieve songs that are due to play in playlist, including current song.
     * @return list of audiofiles
     */
    public List<AudioFile> getUpNext() {
        int current = player.getCurrentMediaItemIndex();
        if (current == -1 || playlist.isEmpty()) return new ArrayList<>();

        return new ArrayList<>(playlist.subList(current, playlist.size()));
    }

    /**
     * Shuffles the playlist around the current song
     */
    public void shufflePlaylist() {
        if (playlist.size() < 2) return;

        int currentIndex = player.getCurrentMediaItemIndex();

        // Shuffle everything before and after the current song
        if (currentIndex > 0) {
            Collections.shuffle(playlist.subList(0, currentIndex));
        }
        if (currentIndex < playlist.size() - 1) {
            Collections.shuffle(playlist.subList(currentIndex + 1, playlist.size()));
        }

        // Load each side into separate arraylist
        List<MediaItem> previousSongs = new ArrayList<>();
        for (int i = 0; i < currentIndex; i++) {
            previousSongs.add(MediaItem.fromUri(playlist.get(i).getFilePath()));
        }
        List<MediaItem> futureSongs = new ArrayList<>();
        for (int i = currentIndex + 1; i < playlist.size(); i++) {
            futureSongs.add(MediaItem.fromUri(playlist.get(i).getFilePath()));
        }

        // Sync with player by swapping out old sub-playlists with new ones
        if (!previousSongs.isEmpty()) {
            player.removeMediaItems(0, currentIndex);
            player.addMediaItems(0, previousSongs);
        }
        if (!futureSongs.isEmpty()) {
            player.removeMediaItems(currentIndex + 1, player.getMediaItemCount());
            player.addMediaItems(currentIndex + 1, futureSongs);
        }
    }

    /**
     * Move item within a playlist
     * @param from initial pos in playlist
     * @param to new pos in playlist
     */
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
    public int getRepeatMode() { return player.getRepeatMode(); }

    private void onSongChanged(AudioFile newSong) {
        currentSongInternal.postValue(newSong);
    }

    public void playAmbient(AudioFile ambient) {
        MediaItem mediaItem = MediaItem.fromUri(ambient.getFilePath());
        ambiencePlayer.setMediaItem(mediaItem);
        ambiencePlayer.prepare();
        ambiencePlayer.play();
        currentAmbientInternal.postValue(ambient);
    }

    public void pauseAmbient() {
        ambiencePlayer.pause();
    }

    public void resumeAmbient() {
        ambiencePlayer.play();
    }

    public boolean isAmbientPlaying() {
        return ambiencePlayer.isPlaying();
    }

    public void setAmbientVolume(float volume) {
        ambiencePlayer.setVolume(volume);
    }

    public float getAmbientVolume() { return ambiencePlayer.getVolume(); }

    public LiveData<AudioFile> getCurrentAmbientInternal() {
        return currentAmbientInternal;
    }

    public void startMusicSleepTimer(long duration, long fade) {
        timerHandler.removeCallbacks(musicTimerRunnable);
        musicBaseVolume = player.getVolume(); // Capture current user volume

        musicInitialDuration = duration;
        musicEndTime = SystemClock.elapsedRealtime() + duration;
        musicFadeDuration = fade;
        isMusicTimerPaused = false;

        musicTimerRemaining.postValue(duration); // post immediately to update UI
        timerHandler.post(musicTimerRunnable);
    }

    public void pauseMusicSleepTimer() {
        if (musicTimerRemaining.getValue() != null) {
            isMusicTimerPaused = true;

            // Calculate current time (between second postings)
            long remaining = musicEndTime - SystemClock.elapsedRealtime();
            musicTimerRemaining.postValue(remaining);
            timerHandler.removeCallbacks(musicTimerRunnable);
        }
    }

    public void resumeMusicSleepTimer() {
        Long remaining = musicTimerRemaining.getValue();
        if (remaining != null && remaining > 0) {
            isMusicTimerPaused = false;

            musicEndTime = SystemClock.elapsedRealtime() + remaining;
            timerHandler.post(musicTimerRunnable);
        }
    }

    public void resetMusicSleepTimer() {
        startMusicSleepTimer(musicInitialDuration, musicFadeDuration);
    }

    public boolean getMusicTimerPaused() {
        return isMusicTimerPaused;
    }

    public LiveData<Long> getMusicTimerRemaining() {
        return musicTimerRemaining;
    }

    public void startAmbienceSleepTimer(long duration, long fade) {
        timerHandler.removeCallbacks(ambienceTimerRunnable);
        ambienceBaseVolume = ambiencePlayer.getVolume(); // Capture current user volume

        ambienceInitialDuration = duration;
        ambienceEndTime = SystemClock.elapsedRealtime() + duration;
        ambienceFadeDuration = fade;
        isAmbienceTimerPaused = false;

        ambienceTimerRemaining.postValue(duration); // post immediately to update UI
        timerHandler.post(ambienceTimerRunnable);
    }

    public void pauseAmbienceSleepTimer() {
        if (ambienceTimerRemaining.getValue() != null) {
            isAmbienceTimerPaused = true;

            // Calculate current time (between second postings)
            long remaining = ambienceEndTime - SystemClock.elapsedRealtime();
            ambienceTimerRemaining.postValue(remaining);
            timerHandler.removeCallbacks(ambienceTimerRunnable);
        }
    }

    public void resumeAmbienceSleepTimer() {
        Long remaining = ambienceTimerRemaining.getValue();
        if (remaining != null && remaining > 0) {
            isAmbienceTimerPaused = false;

            ambienceEndTime = SystemClock.elapsedRealtime() + remaining;
            timerHandler.post(ambienceTimerRunnable);
        }
    }

    public void resetAmbienceSleepTimer() {
        startAmbienceSleepTimer(ambienceInitialDuration, ambienceFadeDuration);
    }


    public boolean getAmbienceTimerPaused() {
        return isAmbienceTimerPaused;
    }

    public LiveData<Long> getAmbienceTimerRemaining() {
        return ambienceTimerRemaining;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.release();
        ambiencePlayer.release();
        mediaSession.release();
    }

    @VisibleForTesting
    public void setPlayer(ExoPlayer player) {
        this.player = player;
    }
}