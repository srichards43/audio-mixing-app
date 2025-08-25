package com.example.audiomixer.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.audiomixer.R;
import com.example.audiomixer.activities.SettingsActivity;
import com.example.audiomixer.objects.AudioFile;

import java.util.List;

import me.tankery.lib.circularseekbar.CircularSeekBar;

public class HomeFragment extends Fragment {

    private CircularSeekBar songVolumeSeekBar;
    private CircularSeekBar ambientVolumeSeekBar;
    private TextView songVolumeDisplay;
    private TextView ambientVolumeDisplay;
    private HomeFragment.OnVolumeChangeListener onVolumeChangeListener;



    // Communicate to mainActivity
    public interface OnVolumeChangeListener {
        void onSongVolumeChanged(float volume);
        void onAmbientVolumeChanged(float volume);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        // Connect interface to MainActivity
        super.onAttach(context);
        if (context instanceof HomeFragment.OnVolumeChangeListener) {
            onVolumeChangeListener = (HomeFragment.OnVolumeChangeListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnSongSelectListener");
        }
    }

    CircularSeekBar.OnCircularSeekBarChangeListener volumeListener = new CircularSeekBar.OnCircularSeekBarChangeListener () {

        @Override
        public void onStartTrackingTouch(@Nullable CircularSeekBar circularSeekBar) {

        }

        @Override
        public void onStopTrackingTouch(@Nullable CircularSeekBar circularSeekBar) {

        }

        @Override
        public void onProgressChanged(@Nullable CircularSeekBar circularSeekBar, float v, boolean b) {
            TextView label = null;

            float volume = v / 100f; // Convert into ExoPlayer compatible volume
            // Set corresponding label
            if (circularSeekBar == songVolumeSeekBar) {
                label = songVolumeDisplay;
                onVolumeChangeListener.onSongVolumeChanged(volume);
            } else if (circularSeekBar == ambientVolumeSeekBar) {
                label = ambientVolumeDisplay;
                onVolumeChangeListener.onAmbientVolumeChanged(volume);
            }

            if (label != null) {
                label.setText(String.valueOf((int) v));
            }
        }
    };

    public HomeFragment() {
        // Required empty public constructor
    }

    public void launchSettings(View v) {
        Intent i = new Intent(requireContext(), SettingsActivity.class);
        startActivity(i);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        view.findViewById(R.id.settingsButton).setOnClickListener(this::launchSettings);

        songVolumeSeekBar = view.findViewById(R.id.songVolumeSeekBar);
        songVolumeDisplay = view.findViewById(R.id.songVolumeDisplay);

        songVolumeSeekBar.setOnSeekBarChangeListener(volumeListener);

        ambientVolumeSeekBar = view.findViewById(R.id.ambientVolumeSeekBar);
        ambientVolumeDisplay = view.findViewById(R.id.ambientVolumeDisplay);

        ambientVolumeSeekBar.setOnSeekBarChangeListener(volumeListener);

        // Initialise labels
        volumeListener.onProgressChanged(songVolumeSeekBar, songVolumeSeekBar.getProgress(), false);
        volumeListener.onProgressChanged(ambientVolumeSeekBar, ambientVolumeSeekBar.getProgress(), false);

        return view;
    }
}