package com.example.audiomixer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity {

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


    public void launchSettings(View v) {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Home");

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
}