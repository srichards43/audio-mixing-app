package com.example.audiomixer.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ImageButton;
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
import com.example.audiomixer.fragments.SongFragment;
import com.example.audiomixer.objects.AudioFile;
import com.example.audiomixer.services.MusicPlaybackService;
import com.example.audiomixer.utils.AppPreferences;
import com.example.audiomixer.utils.TimeUtility;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SongFragment.OnSongSelectListener {

    private ConstraintLayout songPanel;
    private SeekBar songSeekBar;
    private boolean isSongPanelOpen = false; // Store state of song panel
    private Drawable thumb;
    private Drawable invisibleThumb;
    private MusicPlaybackService playbackService;
    private boolean serviceBound = false;
    private ImageButton songPauseButton;
    private TextView songTitle;
    private TextView songInfo;
    private TextView songCurrentTimeText;
    private TextView songDurationText;



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

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        FragmentManager manager = getSupportFragmentManager();
        PagerAdapter adapter = new PagerAdapter(manager, getLifecycle());
        viewPager.setAdapter(adapter);

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

        songPanel = findViewById(R.id.songPanel);
        songPanel.setOnClickListener(v -> toggleSongPanel());

        songSeekBar = findViewById(R.id.songPositionSeekBar);
        songSeekBar.setOnTouchListener((v, event) -> true); // Not open on startup

        songSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && serviceBound) {
                    playbackService.goToCurrentPosInSong(progress);
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
        invisibleThumb = ResourcesCompat.getDrawable(getResources(), R.drawable.invisible_thumb, null);

        songPauseButton = findViewById(R.id.panelPauseButton);
        songPauseButton.setOnClickListener(v -> pauseOrResume());

        songTitle = findViewById(R.id.panelSongTitle);
        songInfo = findViewById(R.id.panelSongInfo);
        songCurrentTimeText = findViewById(R.id.panelSongCurrentTime);
        songDurationText = findViewById(R.id.panelSongDuration);
    }

    private void toggleSongPanel() {
        isSongPanelOpen = !isSongPanelOpen;

        if (isSongPanelOpen) {
            songSeekBar.setOnTouchListener(null);
            songSeekBar.setThumb(thumb);

            Long posLong = playbackService.getCurrentPositionInSong().getValue();
            if (posLong != null) {
                int progress = posLong.intValue();

                songSeekBar.post(() -> songSeekBar.setProgress(progress));
            }
        } else {
            songSeekBar.setOnTouchListener((v, event) -> true);
            songSeekBar.setThumb(invisibleThumb);
        }
    }

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

        songDurationText.setText("/");
        songDurationText.append(TimeUtility.getFormattedDuration(duration));

        // Instantly set currentTime display to 0 to avoid tick based load in
        songCurrentTimeText.setText(TimeUtility.getFormattedDuration(0));
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
        }
    }
}