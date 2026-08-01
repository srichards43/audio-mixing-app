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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.audiomixer.R;
import com.example.audiomixer.utils.AppPreferences;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> songDirectoryPickLauncher;
    private ActivityResultLauncher<Intent> ambientDirectoryPickLauncher;

    private ConstraintLayout themeRow;
    private ConstraintLayout launchTabRow;
    private ConstraintLayout ambientDiskRotationRow;
    private ConstraintLayout songPathRow;
    private ConstraintLayout ambientPathRow;
    private Spinner themeSpinner;
    private Spinner launchTabSpinner;
    private SwitchCompat ambientDiskRotationSwitch;
    private Button songPathButton;
    private Button ambientPathButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);

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

        themeRow = this.findViewById(R.id.themeRow);
        themeSpinner = this.findViewById(R.id.themeSpinner);

        // Load theme selection spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.theme_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(adapter);

        themeSpinner.setSelection(AppPreferences.getThemeIndex(this));

        themeRow.setOnClickListener(v -> themeSpinner.performClick());

        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != AppPreferences.getThemeIndex(SettingsActivity.this)) {
                    AppPreferences.setThemeIndex(SettingsActivity.this, position);
                    AppPreferences.applyTheme(SettingsActivity.this);
                    recreate(); // Restart activity to show new theme
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        launchTabRow = this.findViewById(R.id.launchTabRow);
        launchTabSpinner = this.findViewById(R.id.launchTabSpinner);

        // Load launch tab selection spinner
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                this,
                R.array.launch_options,
                android.R.layout.simple_spinner_item
        );

        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        launchTabSpinner.setAdapter(adapter2);
        launchTabSpinner.setSelection(AppPreferences.getLaunchTab(this));

        launchTabRow.setOnClickListener(v -> launchTabSpinner.performClick());

        launchTabSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != AppPreferences.getLaunchTab(SettingsActivity.this)) {
                    AppPreferences.setLaunchTab(SettingsActivity.this, position);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        songPathRow = this.findViewById(R.id.songPathRow);
        songPathButton = this.findViewById(R.id.songPathButton);
        // Load path button text
        Uri savedUri = AppPreferences.getMusicDirectoryUri(this);
        if (savedUri != null) {
            songPathButton.setText(savedUri.toString());
        }
        songDirectoryPickLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null && result.getResultCode() == RESULT_OK) {
                        Uri uri = result.getData().getData();

                        // Make permissions persist when app closed
                        assert uri != null;
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        );

                        AppPreferences.setMusicDirectoryUri(this, uri);
                        songPathButton.setText(uri.toString());
                    }
                }
        );
        songPathRow.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            songDirectoryPickLauncher.launch(intent);
        });

        ambientPathRow = this.findViewById(R.id.ambientPathRow);
        ambientPathButton = this.findViewById(R.id.ambientPathButton);
        // Load path button text
        Uri savedAmbientUri = AppPreferences.getAmbientDirectoryUri(this);
        if (savedAmbientUri != null) {
            ambientPathButton.setText(savedAmbientUri.toString());
        }
        ambientDirectoryPickLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null && result.getResultCode() == RESULT_OK) {
                        Uri uri = result.getData().getData();

                        // Make permissions persist when app closed
                        assert uri != null;
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        );

                        AppPreferences.setAmbientDirectoryUri(this, uri);
                        ambientPathButton.setText(uri.toString());
                    }
                }
        );
        ambientPathRow.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            ambientDirectoryPickLauncher.launch(intent);
        });


        ambientDiskRotationRow = this.findViewById(R.id.ambientDiskRotationRow);
        ambientDiskRotationSwitch = this.findViewById(R.id.ambientDiskRotationSwitch);

        ambientDiskRotationRow.setOnClickListener(v -> {
                ambientDiskRotationSwitch.toggle();
        });
        ambientDiskRotationSwitch.setChecked(AppPreferences.getAmbientDiscRotation(this));
        ambientDiskRotationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreferences.setAmbientDiscRotation(this, isChecked);
        });

        Button resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(v -> {
            AppPreferences.resetAll(this);

            themeSpinner.setSelection(0);
            launchTabSpinner.setSelection(1);
            ambientDiskRotationSwitch.setChecked(true);
            songPathButton.setText(R.string.settings_path_button);
            ambientPathButton.setText(R.string.settings_path_button);

            AppPreferences.applyTheme(this);
            recreate();
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}