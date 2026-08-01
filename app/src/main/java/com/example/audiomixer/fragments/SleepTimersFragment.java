package com.example.audiomixer.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.audiomixer.R;
import com.example.audiomixer.activities.MainActivity;
import com.example.audiomixer.services.MusicPlaybackService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SleepTimersFragment extends BottomSheetDialogFragment {
    private MusicPlaybackService playbackService;

    // Music UI
    private MaterialCardView musicTimerCard;
    private ConstraintLayout musicTimerHeader;
    private SwitchCompat musicTimerSwitch;
    private ConstraintLayout musicResetRow;
    private TextView musicTimerField;
    private View musicTimerPauseOverlay;

    // Ambience UI
    private MaterialCardView ambienceTimerCard;
    private ConstraintLayout ambienceTimerHeader;
    private SwitchCompat ambienceTimerSwitch;
    private ConstraintLayout ambienceResetRow;
    private TextView ambienceTimerField;
    private View ambienceTimerPauseOverlay;

    // Editor UI
    private Spinner timerSpinner;
    private TextView timerDurationField;
    private ConstraintLayout fadeDurationRow;
    private TextView fadeDurationField;
    private ChipGroup presetChipGroup;
    private ConstraintLayout timerStartRow;


    private int timerDuration = 900000; // in ms, 15min default
    private int fadeDuration = 15000; // in ms
    private static final int DEFAULT_TIMER_INDEX = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sleep_timers, container, false);

        musicTimerCard = view.findViewById(R.id.musicTimerCard);
        musicTimerHeader = view.findViewById(R.id.musicTimerHeader);
        musicTimerSwitch = view.findViewById(R.id.musicTimerSwitch);
        musicResetRow = view.findViewById(R.id.musicResetRow);
        musicTimerPauseOverlay = musicTimerCard.findViewById(R.id.musicTimerPauseOverlay);
        musicTimerField = musicTimerCard.findViewById(R.id.musicTimer);

        musicResetRow.setOnClickListener(v -> { playbackService.resetMusicSleepTimer(); });

        musicTimerHeader.setOnClickListener(v -> musicTimerSwitch.toggle());

        // Use on click instead of toggle to avoid pause resume looping
        musicTimerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                musicTimerPauseOverlay.setVisibility(View.GONE);
                playbackService.resumeMusicSleepTimer();
            } else {
                musicTimerPauseOverlay.setVisibility(View.VISIBLE);
                playbackService.pauseMusicSleepTimer();
            }
        });


        ambienceTimerCard = view.findViewById(R.id.ambienceTimerCard);
        ambienceTimerHeader = view.findViewById(R.id.ambienceTimerHeader);
        ambienceTimerSwitch = view.findViewById(R.id.ambienceTimerSwitch);
        ambienceResetRow = view.findViewById(R.id.ambienceResetRow);
        ambienceTimerPauseOverlay = ambienceTimerCard.findViewById(R.id.ambienceTimerPauseOverlay);
        ambienceTimerField = ambienceTimerCard.findViewById(R.id.ambienceTimer);

        ambienceResetRow.setOnClickListener(v -> { playbackService.resetAmbienceSleepTimer(); });

        ambienceTimerHeader.setOnClickListener(v -> ambienceTimerSwitch.toggle());

        // Use on click instead of toggle to avoid pause resume looping
        ambienceTimerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ambienceTimerPauseOverlay.setVisibility(View.GONE);
                playbackService.resumeAmbienceSleepTimer();
            } else {
                ambienceTimerPauseOverlay.setVisibility(View.VISIBLE);
                playbackService.pauseAmbienceSleepTimer();
            }
        });

        // Preset chips
        presetChipGroup = view.findViewById(R.id.presetChipGroup);
        Chip chip30 = presetChipGroup.findViewById(R.id.chip30);
        Chip chip60 = presetChipGroup.findViewById(R.id.chip60);
        Chip chip90 = presetChipGroup.findViewById(R.id.chip90);
        chip30.setOnClickListener(v -> setTimerDuration(1800000));
        chip60.setOnClickListener(v -> setTimerDuration(3600000));
        chip90.setOnClickListener(v -> setTimerDuration(5400000));


        ConstraintLayout timerDurationRow = view.findViewById(R.id.timerDurationRow);
        timerDurationRow.setOnClickListener(v -> {
            showDurationPicker("Set Timer", timerDuration, false);
        });

        timerDurationField = timerDurationRow.findViewById(R.id.timerDurationField);
        setTimerDuration(timerDuration);

        fadeDurationRow = view.findViewById(R.id.fadeDurationRow);
        fadeDurationRow.setOnClickListener(v -> {
            showDurationPicker("Set Fade", fadeDuration, true);
        });

        fadeDurationField = fadeDurationRow.findViewById(R.id.fadeDurationField);
        setFadeDuration(fadeDuration);

        ConstraintLayout timerSelectRow = view.findViewById(R.id.timerSelectRow);
        timerSpinner = view.findViewById(R.id.timerSpinner);

        // Load timer options selection spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.timer_options,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timerSpinner.setAdapter(adapter);
        timerSpinner.setSelection(DEFAULT_TIMER_INDEX);

        timerSelectRow.setOnClickListener(v -> timerSpinner.performClick());

        timerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        timerStartRow = view.findViewById(R.id.timerStartRow);
        timerStartRow.setOnClickListener(v -> {
            int target = timerSpinner.getSelectedItemPosition();
            switch (target) {
                case 0:
                    playbackService.startMusicSleepTimer(timerDuration, fadeDuration);
                    break;
                case 1:
                    playbackService.startAmbienceSleepTimer(timerDuration, fadeDuration);
                    break;
                case 2:
                    playbackService.startMusicSleepTimer(timerDuration, fadeDuration);
                    playbackService.startAmbienceSleepTimer(timerDuration, fadeDuration);
                    break;
            }
        });


        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((MainActivity)requireActivity()).serviceLiveData.observe(getViewLifecycleOwner(), playbackService -> {
            this.playbackService = playbackService;
            syncUIWithService();
        });
    }

    public void startTimer() {

    }

    private void syncUIWithService() {
        if (playbackService == null) return;

        // Music card
        playbackService.getMusicTimerRemaining().observe(getViewLifecycleOwner(), remaining -> {
            if (remaining != null) {
                musicTimerCard.setVisibility(View.VISIBLE);
                if (remaining == 0) {
                    // Timer finished
                    musicTimerField.setText(R.string.timer_finished);
                } else {
                    musicTimerField.setText(formatTime(remaining.intValue()));
                }

                musicTimerSwitch.setChecked(!playbackService.getMusicTimerPaused());
            } else {
                musicTimerCard.setVisibility(View.GONE);
            }
        });

        // Ambience card
        playbackService.getAmbienceTimerRemaining().observe(getViewLifecycleOwner(), remaining -> {
            if (remaining != null) {
                ambienceTimerCard.setVisibility(View.VISIBLE);
                if (remaining == 0) {
                    // Timer finished
                    musicTimerField.setText(R.string.timer_finished);
                } else {
                    musicTimerField.setText(formatTime(remaining.intValue()));
                }

                ambienceTimerSwitch.setChecked(!playbackService.getAmbienceTimerPaused());
            } else {
                ambienceTimerCard.setVisibility(View.GONE);
            }
        });

    }

    private void setTimerDuration(int duration) {
        timerDuration = duration;
        timerDurationField.setText(formatTime(duration));

        // Ensure fade duration not greater than timer duration
        if (timerDuration < fadeDuration) {
            fadeDuration = timerDuration;
        }
    }

    private void setFadeDuration(int duration) {
        fadeDuration = duration;
        fadeDurationField.setText(formatTime(duration));
    }

    /**
     * Init duration picker dialog
     * @param title for header
     * @param initialDuration in ms, to set on open
     * @param isFade if fade or timer
     */
    private void showDurationPicker(String title, int initialDuration, boolean isFade) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_timer, null);

        NumberPicker hourPicker = dialogView.findViewById(R.id.hourPicker);
        NumberPicker minutePicker = dialogView.findViewById(R.id.minutePicker);
        NumberPicker secondPicker = dialogView.findViewById(R.id.secondPicker);

        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);

        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);

        int totalSeconds = initialDuration / 1000;

        hourPicker.setValue(totalSeconds / 3600);
        minutePicker.setValue((totalSeconds % 3600) / 60);
        secondPicker.setValue(totalSeconds % 60);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("Set", (dialog, which) -> {
                    int hours = hourPicker.getValue();
                    int minutes = minutePicker.getValue();
                    int seconds = secondPicker.getValue();

                    int durationMs = (hours * 3600 + minutes * 60 + seconds) * 1000;
                    if (isFade) {
                        setFadeDuration(durationMs);
                    } else {
                        presetChipGroup.clearCheck();
                        setTimerDuration(durationMs);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    /**
     * Internal method to convert int ms to formatted time string
     * @param ms milliseconds
     * @return formatted string in hours, minutes, seconds where applicable
     */
    private String formatTime(int ms) {
        int hours = ms / 3600000;
        int minutes = (ms % 3600000) / 60000;
        int seconds = (ms % 60000) / 1000;

        String duration = "";
        if (hours > 0) {
            duration += hours + "hr ";
        }
        if (minutes > 0) {
            duration += minutes + "min ";
        }
        if (seconds > 0) {
            duration += seconds + "s";
        }

        return duration;
    }

    private void showFadeDurationRow() {
        fadeDurationRow.setAlpha(0f);
        fadeDurationRow.setVisibility(View.VISIBLE);
        fadeDurationRow.animate()
                .alpha(1f)
                .setDuration(200)
                .start();
    }

    private void hideFadeDurationRow() {
        fadeDurationRow.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    fadeDurationRow.setVisibility(View.GONE);
                    fadeDurationRow.setAlpha(1f);
                })
                .start();
    }
}
