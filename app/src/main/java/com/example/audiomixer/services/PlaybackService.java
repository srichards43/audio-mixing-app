package com.example.audiomixer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.media.session.MediaButtonReceiver;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.audiomixer.R;
import com.example.audiomixer.activities.MainActivity;
import com.example.audiomixer.objects.AudioFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaybackService extends Service {
    private ExoPlayer player;
    private ExoPlayer ambiencePlayer;
    private final IBinder binder = new LocalBinder();
    private Handler handler;
    private Runnable updatePositionRunnable;
    private final List<AudioFile> playlist = new ArrayList<>();
    private final MutableLiveData<Long> currentPositionInSongInternal = new MutableLiveData<>();
    private final MutableLiveData<AudioFile> currentSongInternal = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlayingInternal = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isAmbientPlayingInternal = new MutableLiveData<>(false);
    private final MutableLiveData<AudioFile> currentAmbientInternal = new MutableLiveData<>();
    private final String channelId = "music_channel";

    private MediaSessionCompat musicMediaSession;
    private MediaSessionCompat ambienceMediaSession;
    private final int NOTIFICATION_ID_MUSIC = 1;
    private final int NOTIFICATION_ID_AMBIENT = 2;

    public static final String ACTION_DISMISS = "com.example.audiomixer.ACTION_DISMISS";
    public static final String ACTION_CLOSE = "ACTION_CLOSE";

    private static final long MUSIC_ACTIONS = PlaybackStateCompat.ACTION_PLAY_PAUSE |
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
            PlaybackStateCompat.ACTION_STOP |
            PlaybackStateCompat.ACTION_SEEK_TO;

    private static final long AMBIENT_ACTIONS = PlaybackStateCompat.ACTION_PLAY_PAUSE |
            PlaybackStateCompat.ACTION_STOP;

    private final MediaSessionCompat.Callback musicCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() { resume(); }
        @Override
        public void onPause() { pause(); }
        @Override
        public void onSkipToNext() { skipToNext(); }
        @Override
        public void onSkipToPrevious() { skipToPrevious(); }
        @Override
        public void onStop() { stopMusic(); }
        @Override
        public void onSeekTo(long pos) { goToCurrentPosInSong(pos); }

        @Override
        public void onCustomAction(String action, android.os.Bundle extras) {
            if (ACTION_CLOSE.equals(action)) stopMusic();
        }
    };

    private final MediaSessionCompat.Callback ambientCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() { resumeAmbient(); }
        @Override
        public void onPause() { pauseAmbient(); }
        @Override
        public void onStop() { stopAmbient(); }

        @Override
        public void onCustomAction(String action, android.os.Bundle extras) {
            if (ACTION_CLOSE.equals(action)) stopAmbient();
        }
    };

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long musicEndTime = 0, musicFadeDuration = 0, musicInitialDuration = 0;
    private float musicBaseVolume = 1.0f;
    private final MutableLiveData<Long> musicTimerRemaining = new MutableLiveData<>(null);
    private boolean isMusicTimerPaused = false;

    private long ambienceEndTime = 0, ambienceFadeDuration = 0, ambienceInitialDuration = 0;
    private float ambienceBaseVolume = 1.0f;
    private final MutableLiveData<Long> ambienceTimerRemaining = new MutableLiveData<>(null);
    private boolean isAmbienceTimerPaused = false;

    private final Runnable musicTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long remaining = musicEndTime - SystemClock.elapsedRealtime();
            if (remaining <= 0) {
                player.pause();
                player.setVolume(musicBaseVolume);
                musicTimerRemaining.postValue(0L);
                return;
            }
            musicTimerRemaining.postValue(remaining);
            if (remaining <= musicFadeDuration) {
                player.setVolume(musicBaseVolume * ((float) remaining / musicFadeDuration));
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    private final Runnable ambienceTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long remaining = ambienceEndTime - SystemClock.elapsedRealtime();
            if (remaining <= 0) {
                ambiencePlayer.pause();
                ambiencePlayer.setVolume(ambienceBaseVolume);
                ambienceTimerRemaining.postValue(0L);
                return;
            }
            ambienceTimerRemaining.postValue(remaining);
            if (remaining <= ambienceFadeDuration) {
                ambiencePlayer.setVolume(ambienceBaseVolume * ((float) remaining / ambienceFadeDuration));
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    public class LocalBinder extends Binder {
        public PlaybackService getService() { return PlaybackService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        musicMediaSession = new MediaSessionCompat(this, "MusicService");
        musicMediaSession.setActive(true);
        musicMediaSession.setCallback(musicCallback);

        ambiencePlayer = new ExoPlayer.Builder(this).build();
        ambiencePlayer.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
        ambienceMediaSession = new MediaSessionCompat(this, "AmbientService");
        ambienceMediaSession.setActive(true);
        ambienceMediaSession.setCallback(ambientCallback);

        player.addListener(new ExoPlayer.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                isPlayingInternal.postValue(isPlaying);
                updatePlaybackState();
                if (isPlaying) startUpdatingPositionInSong();
                else stopUpdatingPositionInSong();
                if (getCurrentSong() != null) updateMusicNotification();
            }
            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                if (mediaItem != null) onSongChanged(playlist.get(player.getCurrentMediaItemIndex()));
            }
        });

        ambiencePlayer.addListener(new ExoPlayer.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                isAmbientPlayingInternal.postValue(isPlaying);
                updateAmbientPlaybackState();
                if (currentAmbientInternal.getValue() != null) updateAmbientNotification();
            }
        });

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(new NotificationChannel(channelId, "Music Playback", NotificationManager.IMPORTANCE_LOW));
        }

        handler = new Handler(Looper.getMainLooper());
        updatePositionRunnable = () -> {
            if (player.isPlaying()) {
                currentPositionInSongInternal.setValue(player.getCurrentPosition());
                handler.postDelayed(updatePositionRunnable, 1000);
            }
        };
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        shutdownService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DISMISS.equals(intent.getAction())) {
            shutdownService();
            return START_NOT_STICKY;
        }
        updateMusicNotification();
        updateAmbientNotification();
        return START_STICKY;
    }

    private void stopMusic() {
        Log.i("PlaybackService", "Stop Music requested.");
        player.stop();
        player.clearMediaItems();
        currentSongInternal.postValue(null);
        if (musicMediaSession != null) {
            musicMediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 0)
                    .build());
            musicMediaSession.setActive(false);
        }
        stopForeground(true);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID_MUSIC);
        checkStopService();
    }

    private void stopAmbient() {
        Log.i("PlaybackService", "Stop Ambient requested.");
        ambiencePlayer.stop();
        ambiencePlayer.clearMediaItems();
        currentAmbientInternal.postValue(null);
        if (ambienceMediaSession != null) {
            ambienceMediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 0)
                    .build());
            ambienceMediaSession.setActive(false);
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID_AMBIENT);
        checkStopService();
    }

    private void checkStopService() {
        if (currentSongInternal.getValue() == null && currentAmbientInternal.getValue() == null) {
            stopSelf();
        }
    }

    private void shutdownService() {
        player.stop(); player.clearMediaItems();
        ambiencePlayer.stop(); ambiencePlayer.clearMediaItems();
        currentSongInternal.postValue(null);
        currentAmbientInternal.postValue(null);
        isPlayingInternal.postValue(false);
        isAmbientPlayingInternal.postValue(false);
        if (musicMediaSession != null) musicMediaSession.setActive(false);
        if (ambienceMediaSession != null) ambienceMediaSession.setActive(false);
        stopForeground(true);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID_MUSIC);
        manager.cancel(NOTIFICATION_ID_AMBIENT);
        stopSelf();
    }

    private void updatePlaybackState() {
        int state = isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder().setState(state, player.getCurrentPosition(), 1f).setActions(MUSIC_ACTIONS);
        builder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(ACTION_CLOSE, "Close", R.drawable.ic_close).build());
        musicMediaSession.setPlaybackState(builder.build());
    }

    private void updateAmbientPlaybackState() {
        int state = ambiencePlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder().setState(state, ambiencePlayer.getCurrentPosition(), 1f).setActions(AMBIENT_ACTIONS);
        builder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(ACTION_CLOSE, "Close", R.drawable.ic_close).build());
        ambienceMediaSession.setPlaybackState(builder.build());
    }

    private void updateMusicNotification() {
        AudioFile song = getCurrentSong();
        if (song == null) return;
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setSmallIcon(R.drawable.ic_music).setOngoing(true);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        builder.setContentTitle(song.getTitle()).setContentText(song.getArtist()).setContentIntent(contentIntent)
            .addAction(R.drawable.ic_skip_previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
            .addAction(isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play, "Toggle", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))
            .addAction(R.drawable.ic_skip_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
            .addAction(R.drawable.ic_close, "Close", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(musicMediaSession.getSessionToken()).setShowActionsInCompactView(1, 2, 3));

        if (song.getAlbumCover() != null) builder.setLargeIcon(BitmapFactory.decodeByteArray(song.getAlbumCover(), 0, song.getAlbumCover().length));
        startForeground(NOTIFICATION_ID_MUSIC, builder.build());
    }

    private void updateAmbientNotification() {
        AudioFile ambient = currentAmbientInternal.getValue();
        if (ambient == null) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_ambient).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setOngoing(true)
                .setContentTitle(ambient.getTitle()).setContentText("Ambient Sound")
                .addAction(ambiencePlayer.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play, "Toggle", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))
                .addAction(R.drawable.ic_close, "Close", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(ambienceMediaSession.getSessionToken()).setShowActionsInCompactView(0, 1));

        if (ambient.getAlbumCover() != null) builder.setLargeIcon(BitmapFactory.decodeByteArray(ambient.getAlbumCover(), 0, ambient.getAlbumCover().length));
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID_AMBIENT, builder.build());
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }
    public void play() { if (musicMediaSession != null) musicMediaSession.setActive(true); player.play(); }
    public void seekToSong(int position) { if (position >= 0 && position < playlist.size()) { player.seekTo(position, 0); player.play(); } }
    public void pause() { player.pause(); }
    public void resume() { player.play(); }
    public void skipToNext() { player.seekToNext(); }
    public void skipToPrevious() { player.seekToPrevious(); }
    public void goToCurrentPosInSong(long pos) { player.seekTo(pos); }
    public void setSongVolume(float volume) { player.setVolume(volume); }
    public float getSongVolume() { return player.getVolume(); }
    public LiveData<Long> getCurrentPositionInSong() { return currentPositionInSongInternal; }
    public void startUpdatingPositionInSong() { handler.removeCallbacks(updatePositionRunnable); handler.post(updatePositionRunnable); }
    public void stopUpdatingPositionInSong() { handler.removeCallbacks(updatePositionRunnable); }
    public boolean isPlaying() { return player.isPlaying(); }
    public AudioFile getCurrentSong() { int index = player.getCurrentMediaItemIndex(); return (index >= 0 && index < playlist.size()) ? playlist.get(index) : null; }
    public void setLoop(int state) {
        switch (state) {
            case 0: player.setRepeatMode(ExoPlayer.REPEAT_MODE_OFF); break;
            case 1: player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL); break;
            case 2: player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE); break;
        }
    }
    public List<AudioFile> getPlaylist() { return playlist; }
    public void setPlaylist(List<AudioFile> newPlaylist, int startIndex) {
        if (musicMediaSession != null) musicMediaSession.setActive(true);
        playlist.clear(); playlist.addAll(newPlaylist);
        List<MediaItem> mediaItems = new ArrayList<>();
        for (AudioFile song : playlist) mediaItems.add(MediaItem.fromUri(song.getFilePath()));
        player.setMediaItems(mediaItems, startIndex, 0); player.prepare();
        updateMusicNotification();
    }
    public List<AudioFile> getUpNext() {
        int current = player.getCurrentMediaItemIndex();
        if (current == -1 || playlist.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(playlist.subList(current, playlist.size()));
    }
    public void shufflePlaylist() {
        if (playlist.size() < 2) return;
        int currentIndex = player.getCurrentMediaItemIndex();
        if (currentIndex > 0) Collections.shuffle(playlist.subList(0, currentIndex));
        if (currentIndex < playlist.size() - 1) Collections.shuffle(playlist.subList(currentIndex + 1, playlist.size()));
        List<MediaItem> prev = new ArrayList<>(), next = new ArrayList<>();
        for (int i = 0; i < currentIndex; i++) prev.add(MediaItem.fromUri(playlist.get(i).getFilePath()));
        for (int i = currentIndex + 1; i < playlist.size(); i++) next.add(MediaItem.fromUri(playlist.get(i).getFilePath()));
        if (!prev.isEmpty()) { player.removeMediaItems(0, currentIndex); player.addMediaItems(0, prev); }
        if (!next.isEmpty()) { player.removeMediaItems(currentIndex + 1, player.getMediaItemCount()); player.addMediaItems(currentIndex + 1, next); }
    }
    public void moveItemInQueue(int from, int to) {
        if (from >= 0 && from < playlist.size() && to >= 0 && to < playlist.size()) {
            AudioFile item = playlist.remove(from); playlist.add(to, item); player.moveMediaItem(from, to);
        }
    }
    public LiveData<AudioFile> getCurrentSongInternal() { return currentSongInternal; }
    public LiveData<Boolean> getIsPlaying() { return isPlayingInternal; }
    public int getCurrentIndex() { return player.getCurrentMediaItemIndex(); }
    public int getRepeatMode() { return player.getRepeatMode(); }
    private void onSongChanged(AudioFile newSong) { currentSongInternal.postValue(newSong); updateMusicNotification(); }
    public void playAmbient(AudioFile ambient) {
        if (ambienceMediaSession != null) ambienceMediaSession.setActive(true);
        ambiencePlayer.setMediaItem(MediaItem.fromUri(ambient.getFilePath()));
        ambiencePlayer.prepare(); ambiencePlayer.play(); currentAmbientInternal.postValue(ambient);
        updateAmbientNotification();
    }
    public void pauseAmbient() { ambiencePlayer.pause(); }
    public void resumeAmbient() { ambiencePlayer.play(); }
    public boolean isAmbientPlaying() { return ambiencePlayer.isPlaying(); }
    public void setAmbientVolume(float volume) { ambiencePlayer.setVolume(volume); }
    public float getAmbientVolume() { return ambiencePlayer.getVolume(); }
    public LiveData<AudioFile> getCurrentAmbientInternal() { return currentAmbientInternal; }
    public LiveData<Boolean> getIsAmbientPlaying() { return isAmbientPlayingInternal; }
    public void startMusicSleepTimer(long d, long f) {
        timerHandler.removeCallbacks(musicTimerRunnable); musicBaseVolume = player.getVolume();
        musicInitialDuration = d; musicEndTime = SystemClock.elapsedRealtime() + d; musicFadeDuration = f;
        musicTimerRemaining.postValue(d); timerHandler.post(musicTimerRunnable);
    }
    public void pauseMusicSleepTimer() { if (musicTimerRemaining.getValue() != null) { isMusicTimerPaused = true; musicTimerRemaining.postValue(musicEndTime - SystemClock.elapsedRealtime()); timerHandler.removeCallbacks(musicTimerRunnable); } }
    public void resumeMusicSleepTimer() { Long r = musicTimerRemaining.getValue(); if (r != null && r > 0) { isMusicTimerPaused = false; musicEndTime = SystemClock.elapsedRealtime() + r; timerHandler.post(musicTimerRunnable); } }
    public void resetMusicSleepTimer() { startMusicSleepTimer(musicInitialDuration, musicFadeDuration); }
    public boolean getMusicTimerPaused() { return isMusicTimerPaused; }
    public LiveData<Long> getMusicTimerRemaining() { return musicTimerRemaining; }
    public void startAmbienceSleepTimer(long d, long f) {
        timerHandler.removeCallbacks(ambienceTimerRunnable); ambienceBaseVolume = ambiencePlayer.getVolume();
        ambienceInitialDuration = d; ambienceEndTime = SystemClock.elapsedRealtime() + d; ambienceFadeDuration = f;
        ambienceTimerRemaining.postValue(d); timerHandler.post(ambienceTimerRunnable);
    }
    public void pauseAmbienceSleepTimer() { if (ambienceTimerRemaining.getValue() != null) { isAmbienceTimerPaused = true; ambienceTimerRemaining.postValue(ambienceEndTime - SystemClock.elapsedRealtime()); timerHandler.removeCallbacks(ambienceTimerRunnable); } }
    public void resumeAmbienceSleepTimer() { Long r = ambienceTimerRemaining.getValue(); if (r != null && r > 0) { isAmbienceTimerPaused = false; ambienceEndTime = SystemClock.elapsedRealtime() + r; timerHandler.post(ambienceTimerRunnable); } }
    public void resetAmbienceSleepTimer() { startAmbienceSleepTimer(ambienceInitialDuration, ambienceFadeDuration); }
    public boolean getAmbienceTimerPaused() { return isAmbienceTimerPaused; }
    public LiveData<Long> getAmbienceTimerRemaining() { return ambienceTimerRemaining; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.release();
        ambiencePlayer.release();
        musicMediaSession.release();
        ambienceMediaSession.release();
    }

    @VisibleForTesting
    public void setPlayer(ExoPlayer player) { this.player = player; }
}
