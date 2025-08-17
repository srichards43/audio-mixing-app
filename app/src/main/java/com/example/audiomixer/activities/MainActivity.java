package com.example.audiomixer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.audiomixer.R;
import com.example.audiomixer.adapters.PagerAdapter;
import com.example.audiomixer.fragments.SongFragment;
import com.example.audiomixer.objects.AudioFile;
import com.example.audiomixer.utils.AppPreferences;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import me.tankery.lib.circularseekbar.CircularSeekBar;

public class MainActivity extends AppCompatActivity implements SongFragment.OnSongSelectListener {

    public interface OnSongSelectedListener {
        void onSongSelected (AudioFile song, int position, SongFragment source);
    }

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    /* temp
    SeekBar songVolBar;
    TextView songVolLabel;
    SeekBar ambienceVolBar;

    SeekBar.OnSeekBarChangeListener volumeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean bool) {
            TextView label = null;
            if (seekBar == songVolBar) {
                label = songVolLabel;
            } else if (seekBar == ambienceVolBar) {
                label = null; // temp
            }

            label.setText("" + progress); // Convert to string
        }

        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        public void onStopTrackingTouch(SeekBar seekbar) {

        }
    };
    */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
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

        // Find ids
        /* temp
        songVolBar = findViewById(R.id.songVolumeBar);
        songVolLabel = findViewById(R.id.songVolumeLabel);
        ambienceVolBar = findViewById(R.id.ambienceVolumeBar);

        // Add listeners
        songVolBar.setOnSeekBarChangeListener(volumeListener);
        */

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onSongSelected(AudioFile song) {

    }
}