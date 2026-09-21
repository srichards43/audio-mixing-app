package com.example.audiomixer.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.audiomixer.R;
import com.example.audiomixer.utils.AppPreferences;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> ambientDirectoryPickLauncher;

    private Spinner themeSpinner;
    private Spinner colorSpinner;
    private Spinner launchTabSpinner;
    private SwitchCompat ambientDiskRotationSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        AppPreferences.applyColor(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Settings");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Theme selection
        themeSpinner = findViewById(R.id.themeSpinner);
        ArrayAdapter<CharSequence> themeAdapter = ArrayAdapter.createFromResource(
                this, R.array.theme_options, android.R.layout.simple_spinner_item);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(themeAdapter);
        themeSpinner.setSelection(AppPreferences.getThemeIndex(this));

        findViewById(R.id.themeRow).setOnClickListener(v -> themeSpinner.performClick());
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != AppPreferences.getThemeIndex(SettingsActivity.this)) {
                    AppPreferences.setThemeIndex(SettingsActivity.this, position);
                    AppPreferences.applyTheme(SettingsActivity.this);
                    recreate();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Color selection
        colorSpinner = findViewById(R.id.colorSpinner);
        ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(
                this, R.array.color_options, android.R.layout.simple_spinner_item);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(colorAdapter);
        colorSpinner.setSelection(AppPreferences.getColorIndex(this));

        findViewById(R.id.colorRow).setOnClickListener(v -> colorSpinner.performClick());
        colorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != AppPreferences.getColorIndex(SettingsActivity.this)) {
                    AppPreferences.setColorIndex(SettingsActivity.this, position);
                    AppPreferences.applyColor(SettingsActivity.this);
                    recreate();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Launch tab selection
        launchTabSpinner = findViewById(R.id.launchTabSpinner);
        ArrayAdapter<CharSequence> launchAdapter = ArrayAdapter.createFromResource(
                this, R.array.launch_options, android.R.layout.simple_spinner_item);
        launchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        launchTabSpinner.setAdapter(launchAdapter);
        launchTabSpinner.setSelection(AppPreferences.getLaunchTab(this));

        findViewById(R.id.launchTabRow).setOnClickListener(v -> launchTabSpinner.performClick());
        launchTabSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                AppPreferences.setLaunchTab(SettingsActivity.this, position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Ambient disk rotation toggle
        ambientDiskRotationSwitch = findViewById(R.id.ambientDiskRotationSwitch);
        ambientDiskRotationSwitch.setChecked(AppPreferences.getAmbientDiscRotation(this));
        findViewById(R.id.ambientDiskRotationRow).setOnClickListener(v -> ambientDiskRotationSwitch.toggle());
        ambientDiskRotationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            AppPreferences.setAmbientDiscRotation(SettingsActivity.this, isChecked));

        // Reset button
        findViewById(R.id.resetButton).setOnClickListener(v -> {
            AppPreferences.resetAll(this);
            recreate();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left, 
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right, 
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });
    }
}
