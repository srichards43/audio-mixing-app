package com.example.audiomixer;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    SeekBar songVolBar;
    TextView songVolLabel;
    SeekBar ambienceVolBar;

    // Add event listeners
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Find ids
        songVolBar = findViewById(R.id.songVolumeBar);
        songVolLabel = findViewById(R.id.songVolumeLabel);
        ambienceVolBar = findViewById(R.id.ambienceVolumeBar);

        // Add listeners
        songVolBar.setOnSeekBarChangeListener(volumeListener);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}