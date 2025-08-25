package com.example.audiomixer.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.audiomixer.R;
import com.example.audiomixer.adapters.PagerAdapter;
import com.example.audiomixer.fragments.HomeFragment;
import com.example.audiomixer.fragments.SongFragment;
import com.example.audiomixer.objects.AudioFile;
import com.example.audiomixer.services.MusicPlaybackService;
import com.example.audiomixer.utils.AppPreferences;
import com.example.audiomixer.utils.TimeUtility;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
        implements SongFragment.OnSongSelectListener, HomeFragment.OnVolumeChangeListener {

    private ConstraintLayout songMiniplayer;
    private ConstraintLayout songPanel;
    private LinearLayout songToolbar;
    private SeekBar songSeekBar;
    private boolean isSongPanelOpen = false; // Store state of song panel
    private Drawable thumb;
    private Drawable invisibleThumb;
    private MusicPlaybackService playbackService;
    private boolean serviceBound = false;
    private ImageButton songPauseButton;
    private ImageButton songNextButton;
    private ImageButton songPreviousButton;
    private TextView songTitle;
    private TextView songInfo;
    private TextView songCurrentTimeText;
    private TextView songDurationText;
    private final int PANEL_ANIMATION_DURATION = 200;
    private int loopState = 0; // 0 = off, 1 = repeat current, 2 = repeat playlist
    private FrameLayout loopButton;
    private ImageView loopIcon;
    private TextView loopPlaylistIndicator;
    private int colorPrimary;
    private int colorDefault;


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlaybackService.LocalBinder binder = (MusicPlaybackService.LocalBinder) service;
            playbackService = binder.getService();
            serviceBound = true;

            // Observe position in song from service, continuously update seekbar progress and time display
            playbackService.getCurrentPositionInSong().observe(MainActivity.this, pos -> {
                songSeekBar.setProgress(pos.intValue());
                songCurrentTimeText.setText(TimeUtility.getFormattedDuration(pos));
            });

            // Observe when song changes, call updateSongDetails()
            playbackService.getCurrentSongInternal().observe(MainActivity.this, song -> {
                updateSongDetails();

                // Notify SongFragment to update adapter UI
                SongFragment fragment = (SongFragment) getSupportFragmentManager()
                        .findFragmentByTag("f0");
                if (fragment != null) {
                    fragment.updateCurrentSong(song.getFilePath());
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicPlaybackService.class);

        // Use correct syntax for version >= Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(intent);
        } else {
            this.startService(intent);
        }

        this.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (serviceBound) {
            this.unbindService(serviceConnection);
            serviceBound = false;
        }
    }

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

        viewPager.setCurrentItem(1, false);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Songs");
                    break;
                case 1:
                    tab.setText("Home");
                    break;
                case 2:
                    tab.setText("Ambience");
                    break;
            }
        }).attach();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        songMiniplayer = findViewById(R.id.songPlayer);
        songPanel = songMiniplayer.findViewById(R.id.songPanel);
        songPanel.setOnClickListener(v -> toggleSongPanel());

        songToolbar = songMiniplayer.findViewById(R.id.toolbarPanel);
        songToolbar.setTranslationY(120f); // Hide initially

        songSeekBar = songMiniplayer.findViewById(R.id.songPositionSeekBar);
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

        thumb = ResourcesCompat.getDrawable(getResources(), R.drawable.seekbar_thumb, null);

        // Thumb with alpha = 0 and width = 1dp to stop visual errors
        invisibleThumb = ResourcesCompat.getDrawable(getResources(), R.drawable.invisible_thumb, null);

        songPauseButton = songToolbar.findViewById(R.id.panelPauseButton);
        songPauseButton.setOnClickListener(v -> pauseOrResume());

        songNextButton = songToolbar.findViewById(R.id.panelSkipNextButton);
        songNextButton.setOnClickListener(v -> {
            playbackService.skipToNext();
            pauseOrResume();
        });

        songPreviousButton = songToolbar.findViewById(R.id.panelSkipPreviousButton);
        songPreviousButton.setOnClickListener(v -> {
            playbackService.skipToPrevious();
            pauseOrResume();
        });

        loopButton = songToolbar.findViewById(R.id.panelLoopButton);
        loopIcon = loopButton.findViewById(R.id.loopIcon);
        loopPlaylistIndicator = loopButton.findViewById(R.id.loopText);
        loopPlaylistIndicator.setVisibility(View.GONE);

        loopButton.setOnClickListener(v -> toggleLoop());

        setToolbarChildrenClickable(false); // Not open on startup

        songTitle = songPanel.findViewById(R.id.panelSongTitle);
        songInfo = songPanel.findViewById(R.id.panelSongInfo);
        songCurrentTimeText = songPanel.findViewById(R.id.panelSongCurrentTime);
        songDurationText = songPanel.findViewById(R.id.panelSongDuration);

        songMiniplayer.setVisibility(View.GONE);
    }

    /**
     * Switch between song panel opening and closing
     */
    private void toggleSongPanel() {
        isSongPanelOpen = !isSongPanelOpen;

        if (isSongPanelOpen) {
            // Open
            songSeekBar.setOnTouchListener(null);
            songSeekBar.setThumb(thumb);

            Long posLong = playbackService.getCurrentPositionInSong().getValue();
            if (posLong != null) {
                int progress = posLong.intValue();

                songSeekBar.post(() -> songSeekBar.setProgress(progress));
            }

            songToolbar.animate().translationY(-0f).setDuration(PANEL_ANIMATION_DURATION).start();
            setToolbarChildrenClickable(true);
        } else {
            // Close
            songSeekBar.setOnTouchListener((v, event) -> true);
            songSeekBar.setThumb(invisibleThumb);

            songToolbar.animate().translationY(120f).setDuration(PANEL_ANIMATION_DURATION).start();
            setToolbarChildrenClickable(false);
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

        // Set UI to match state
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

        // Set service state
        playbackService.setLoop(loopState);
    }

    /**
     * Update pause button UI and state of service
     */
    private void pauseOrResume() {
        if (playbackService.isPlaying()) {
            playbackService.pause();
            songPauseButton.setImageResource(R.drawable.play);
        } else {
            playbackService.resume();
            songPauseButton.setImageResource(R.drawable.pause);
        }
    }

    private void updateSongDetails() {
        AudioFile song = playbackService.getCurrentSong();

        songTitle.setText(song.getTitle());
        songInfo.setText(song.getArtist());
        if (!Objects.equals(song.getAlbum(), "")) {
            songInfo.append(" • " + song.getAlbum());
        }

        long duration = song.getDuration(); // Cast to int for seekbar
        songSeekBar.setMax((int) duration);
        songSeekBar.setProgress(0); // Instantly update to avoid visual errors

        songDurationText.setText("/");
        songDurationText.append(TimeUtility.getFormattedDuration(duration));

        // Instantly set currentTime display to 0 to avoid tick based load in
        songCurrentTimeText.setText(TimeUtility.getFormattedDuration(0));
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
            playbackService.setPlaylist(playlist);
            playbackService.play(position);
            updateSongDetails();
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

    @Override
    public void onAmbientVolumeChanged(float volume) {

    }
}