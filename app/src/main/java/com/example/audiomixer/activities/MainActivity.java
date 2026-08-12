package com.example.audiomixer.activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.viewpager2.widget.ViewPager2;

import com.example.audiomixer.R;
import com.example.audiomixer.adapters.PagerAdapter;
import com.example.audiomixer.fragments.AmbientFragment;
import com.example.audiomixer.fragments.HomeFragment;
import com.example.audiomixer.fragments.QueueFragment;
import com.example.audiomixer.fragments.SongFragment;
import com.example.audiomixer.objects.AudioFile;
import com.example.audiomixer.services.PlaybackService;
import com.example.audiomixer.utils.AppPreferences;
import com.example.audiomixer.utils.TimeUtility;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
        implements SongFragment.OnSongSelectListener, HomeFragment.OnVolumeChangeListener, AmbientFragment.OnAmbientSelectListener {

    private ConstraintLayout songMiniplayer;
    private MaterialCardView songToolbar;
    private SeekBar songSeekBar;
    private boolean isSongPanelOpen = false; // Store state of song panel
    private boolean isAmbientButtonOpen = false; // Store state of ambient FAB
    private CardView ambientDisc;
    private ImageView ambientCoverImg;
    private boolean spinAmbientDisc = true;
    private ImageView ambientPauseIcon;
    private Drawable thumb;
    private Drawable invisibleThumb;
    private PlaybackService playbackService;
    private boolean serviceBound = false;
    private ImageButton songPauseButton;
    private TextView songTitle;
    private TextView songInfo;
    private ImageView songAlbumCover;
    private TextView songCurrentTimeText;
    private TextView songDurationText;
    private int loopState = 0; // 0 = off, 1 = repeat current, 2 = repeat playlist
    private ImageView loopIcon;
    private TextView loopPlaylistIndicator;
    private int colorPrimary;
    private int colorDefault;

    public final MutableLiveData<PlaybackService> serviceLiveData = new MutableLiveData<>();



    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder) service;
            playbackService = binder.getService();
            serviceBound = true;

            // Observe position in song from service, continuously update seekbar progress and time display
            playbackService.getCurrentPositionInSong().observe(MainActivity.this, pos -> {
                songSeekBar.setProgress(pos.intValue());
                songCurrentTimeText.setText(TimeUtility.getFormattedDuration(pos));
            });

            // Observe when song changes, call updateSongDetails()
            playbackService.getCurrentSongInternal().observe(MainActivity.this, song -> {
                if (song != null) {
                    updateSongDetails();
                    songMiniplayer.setVisibility(View.VISIBLE);
                } else {
                    songMiniplayer.setVisibility(View.GONE);
                }
            });

            serviceLiveData.setValue(playbackService);
            SyncUIWithService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void SyncUIWithService() {
        if (playbackService == null) return;

        if (playbackService.isPlaying()) {
            songPauseButton.setImageResource(R.drawable.ic_pause);
        } else {
            songPauseButton.setImageResource(R.drawable.ic_play);
        }

        int repeatMode = playbackService.getRepeatMode();

        // Map playback vals to activity vals
        switch (repeatMode) {
            case 1:
                loopState =2;
                break;
            case 2:
                loopState = 1;
                break;
            default: loopState = 0;
        }

        updateLoopUI();

        AudioFile currentSong = playbackService.getCurrentSong();
        if (currentSong != null) {
            openSongMiniplayer();
        }

        AudioFile currentAmbient = playbackService.getCurrentAmbientInternal().getValue();
        if (currentAmbient != null)
        {
            openAmbientDisc();
            if (playbackService.isAmbientPlaying())
            {
                spinAmbientDisc();
            } else {
                ambientPauseIcon.setVisibility(View.VISIBLE);
            }

            if (currentAmbient.getAlbumCover() != null) {
                Bitmap bmp = BitmapFactory.decodeByteArray(currentAmbient.getAlbumCover(), 0, currentAmbient.getAlbumCover().length);
                ambientCoverImg.setImageBitmap(bmp);
            } else {
                ambientCoverImg.setImageResource(R.drawable.shape_circle);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (serviceBound) {
            this.unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Get colorPrimary from theme
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = this.getTheme();
        theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        colorPrimary = typedValue.data;

        // Get default button color from theme
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        colorDefault = typedValue.data;

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        FragmentManager manager = getSupportFragmentManager();
        PagerAdapter pagerAdapter = new PagerAdapter(manager, getLifecycle());
        viewPager.setAdapter(pagerAdapter);

        viewPager.setCurrentItem(AppPreferences.getLaunchTab(this), false);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setIcon(R.drawable.ic_music);
                    //tab.setText("Music");
                    break;
                case 1:
                    tab.setIcon(R.drawable.ic_home);
                    //tab.setText("Home");
                    break;
                case 2:
                    tab.setIcon(R.drawable.ic_ambient);
                    //tab.setText("Ambient");
                    break;
            }
        }).attach();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = new Intent(this, PlaybackService.class);

        // Use correct syntax for version >= Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(intent);
        } else {
            this.startService(intent);
        }

        this.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        songMiniplayer = findViewById(R.id.songPlayer);
        MaterialCardView songPanel = songMiniplayer.findViewById(R.id.songCard);
        songPanel.setOnClickListener(v -> toggleSongPanel());

        songToolbar = songMiniplayer.findViewById(R.id.controlCard);
        songToolbar.setTranslationY(120f); // Hide initially

        songSeekBar = songMiniplayer.findViewById(R.id.songPositionSeekBar);

        // Add listener to block and pass click down to panel if closed, otherwise allow seeking
        songSeekBar.setOnTouchListener((v, event) -> {
            if (!isSongPanelOpen) {
                toggleSongPanel();
                return true;
            }
            return false;
        });

        songSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && serviceBound) {
                    playbackService.goToCurrentPosInSong(progress);
                    songCurrentTimeText.setText(TimeUtility.getFormattedDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        thumb = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_seekbar_thumb, null);

        // Thumb with alpha = 0 and width = 1dp to stop visual errors
        invisibleThumb = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_invisible_thumb, null);

        songPauseButton = songToolbar.findViewById(R.id.panelPauseButton);
        songPauseButton.setOnClickListener(v -> pauseOrResume());

        ImageButton songNextButton = songToolbar.findViewById(R.id.panelSkipNextButton);
        songNextButton.setOnClickListener(v -> {
            playbackService.skipToNext();
            pauseOrResume();
        });

        ImageButton songPreviousButton = songToolbar.findViewById(R.id.panelSkipPreviousButton);
        songPreviousButton.setOnClickListener(v -> {
            playbackService.skipToPrevious();
            pauseOrResume();
        });

        FrameLayout loopButton = songToolbar.findViewById(R.id.panelLoopButton);
        loopIcon = loopButton.findViewById(R.id.loopIcon);
        loopPlaylistIndicator = loopButton.findViewById(R.id.loopText);
        loopPlaylistIndicator.setVisibility(View.GONE);

        loopButton.setOnClickListener(v -> toggleLoop());

        ImageButton queueButton = songToolbar.findViewById(R.id.queueButton);
        queueButton.setOnClickListener(v -> openQueue());


        setToolbarChildrenClickable(false); // Not open on startup

        songTitle = songPanel.findViewById(R.id.songTitle);
        songInfo = songPanel.findViewById(R.id.songInfo);
        songCurrentTimeText = songPanel.findViewById(R.id.songCurrentTime);
        songDurationText = songPanel.findViewById(R.id.songDuration);
        songAlbumCover = songPanel.findViewById(R.id.songAlbumCover);

        songMiniplayer.setVisibility(View.GONE);

        // Ambient UI
        ambientDisc = findViewById(R.id.ambientDisc);
        ambientDisc.setOnClickListener(v -> {
            pauseOrResumeAmbience();
        });
        ambientCoverImg = findViewById(R.id.ambientCover);
        ambientPauseIcon = findViewById(R.id.ambientPauseIcon);
        ambientDisc.setVisibility(View.GONE);

        spinAmbientDisc = AppPreferences.getAmbientDiscRotation(this);
    }

    /**
     * Switch between song panel opening and closing
     */
    private void toggleSongPanel() {
        isSongPanelOpen = !isSongPanelOpen;

        int PANEL_ANIMATION_DURATION = 200;
        if (isSongPanelOpen) {
            // Open
            songSeekBar.setThumb(thumb);

            // Manually set thumb to current time (avoids 1s delay from service updating pos)
            Long posLong = playbackService.getCurrentPositionInSong().getValue();
            if (posLong != null) {
                int progress = posLong.intValue();

                songSeekBar.post(() -> songSeekBar.setProgress(progress));
            }

            songToolbar.animate().translationY(-0f).setDuration(PANEL_ANIMATION_DURATION).start();
            setToolbarChildrenClickable(true);

            // If ambient FAB open, also animate
            if (isAmbientButtonOpen) {
                ambientDisc.animate().translationY(-0f).setDuration(PANEL_ANIMATION_DURATION).start();
            }

        } else {
            // Close
            songSeekBar.setThumb(invisibleThumb);

            songToolbar.animate().translationY(120f).setDuration(PANEL_ANIMATION_DURATION).start();
            setToolbarChildrenClickable(false);

            // If ambient FAB open, also animate
            if (isAmbientButtonOpen) {
                ambientDisc.animate().translationY(120f).setDuration(PANEL_ANIMATION_DURATION).start();
            }
        }
    }

    private void openQueue() {
        if (serviceBound && playbackService != null) {
            QueueFragment fragment = new QueueFragment(playbackService);
            fragment.show(getSupportFragmentManager(), "QueueDialog");
        }
    }

    /**
     * Method to toggle between 3 loop options: no loop, loop current, loop all
     */
    private void toggleLoop() {
        if (loopState == 2) {
            loopState = 0;
        } else {
            loopState++;
        }

        updateLoopUI();

        // Set service state
        playbackService.setLoop(loopState);
    }

    private void updateLoopUI() {
        switch (loopState) {
            case 0:
                loopIcon.setImageTintList(ColorStateList.valueOf(colorDefault));
                loopPlaylistIndicator.setVisibility(View.GONE);
                break;
            case 1:
                loopIcon.setImageTintList(ColorStateList.valueOf(colorPrimary));
                break;
            case 2:
                loopIcon.setImageTintList(ColorStateList.valueOf(colorPrimary));
                loopPlaylistIndicator.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Update pause button UI and state of service
     */
    private void pauseOrResume() {
        if (playbackService.isPlaying()) {
            playbackService.pause();
            songPauseButton.setImageResource(R.drawable.ic_play);
        } else {
            playbackService.resume();
            songPauseButton.setImageResource(R.drawable.ic_pause);
        }
    }

    /**
     * Find current song and update display with details
     */
    private void updateSongDetails() {
        AudioFile song = playbackService.getCurrentSong();

        songTitle.setText(song.getTitle());
        songInfo.setText(song.getArtist());
        if (!Objects.equals(song.getAlbum(), "")) {
            songInfo.append(" • " + song.getAlbum());
        }

        long duration = song.getDuration(); // Cast to int for seekbar
        songSeekBar.setMax((int) duration);

        Long rawPos = playbackService.getCurrentPositionInSong().getValue();
        long songPos = 0;
        if (rawPos != null) {
            songPos = rawPos;
        }

        songSeekBar.setProgress((int) songPos);

        songDurationText.setText("/");
        songDurationText.append(TimeUtility.getFormattedDuration(duration));
        songCurrentTimeText.setText(TimeUtility.getFormattedDuration((int) songPos));

        if (song.getAlbumCover() != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(song.getAlbumCover(), 0, song.getAlbumCover().length);
            songAlbumCover.setImageBitmap(bmp);
        } else {
            // Show default album placeholder
            songAlbumCover.setImageResource(android.R.drawable.ic_menu_report_image);
        }
    }

    /**
     * Helper function to disable/enable clickable children of songToolbar (stops them from blocking main panel)
     * @param clickable true to enable, false to disable
     */
    private void setToolbarChildrenClickable(boolean clickable) {
        for (int i = 0; i < songToolbar.getChildCount(); i++) {
            songToolbar.getChildAt(i).setClickable(clickable);
        }
    }

    /**
     * Called by SongFragment when a song is clicked on
     * @param playlist list of songs to play in order
     * @param position position of the song in playlist
     */
    @Override
    public void onSongSelected(List<AudioFile> playlist, int position) {
        if (serviceBound) {
            playbackService.setPlaylist(playlist, position);
            playbackService.play();
            openSongMiniplayer();
            pauseOrResume();

            songMiniplayer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Called by HomeFragment when volume seekbar is changed
     * @param volume of the songs playing
     */
    @Override
    public void onSongVolumeChanged(float volume) {
        if (playbackService != null) {
            playbackService.setSongVolume(volume);
        }
    }

    private void openSongMiniplayer() {
        songMiniplayer.setVisibility(View.VISIBLE);

        if (ambientDisc.getVisibility() == View.VISIBLE && !isSongPanelOpen) {
            ambientDisc.setTranslationY(120f);
        }

        updateSongDetails();
    }

    private void openAmbientDisc() {
        isAmbientButtonOpen = true;
        ambientDisc.setVisibility(View.VISIBLE);

        if (songMiniplayer.getVisibility() == View.VISIBLE && !isSongPanelOpen) {
            ambientDisc.setTranslationY(120f);
        } else {
            ambientDisc.setTranslationY(0f);
        }
    }

    private void pauseOrResumeAmbience() {
        if (playbackService.isAmbientPlaying()) {
            playbackService.pauseAmbient();
            ambientPauseIcon.setVisibility(View.VISIBLE);
            ambientCoverImg.animate().cancel(); // cancel spin
        } else {
            playbackService.resumeAmbient();
            ambientPauseIcon.setVisibility(View.GONE);
            spinAmbientDisc();
        }
    }

    @Override
    public void onAmbientVolumeChanged(float volume) {
        if (playbackService != null) {
            playbackService.setAmbientVolume(volume);
        }
    }

    /**
     * Begin a new ambient track and update UI
     * @param ambient to play
     */
    @Override
    public void onAmbientSelected(AudioFile ambient) {
        if (serviceBound) {
            playbackService.playAmbient(ambient);

            if (ambientDisc.getVisibility() == View.GONE) {
                openAmbientDisc();
            }

            spinAmbientDisc();
            ambientPauseIcon.setVisibility(View.GONE);

            if (ambient.getAlbumCover() != null) {
                Bitmap bmp = BitmapFactory.decodeByteArray(ambient.getAlbumCover(), 0, ambient.getAlbumCover().length);
                ambientCoverImg.setImageBitmap(bmp);
            } else {
                ambientCoverImg.setImageResource(R.drawable.shape_circle);
            }
        }

    }

    // Recurring method to spin ambience Disc around
    private void spinAmbientDisc() {
        if (!spinAmbientDisc) {
            return;
        }
        ambientCoverImg.animate().rotationBy(360f).setDuration(3000).setInterpolator(new LinearInterpolator())
                .withEndAction(this::spinAmbientDisc).start();
    }

    /**
     * Expose playback service to fragments
     * @return the service
     */
    public PlaybackService getPlaybackService() {
        if (serviceBound) {
            return playbackService;
        } else {
            return null;
        }
    }
}