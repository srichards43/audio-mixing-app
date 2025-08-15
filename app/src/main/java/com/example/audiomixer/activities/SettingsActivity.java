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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.audiomixer.R;
import com.example.audiomixer.utils.AppPreferences;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> directoryPickLauncher;

    private Spinner themeSpinner;
    private SwitchCompat vibrationSwitch;
    private Button pathButton;

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

        // Get references to inputs
        themeSpinner = this.findViewById(R.id.themeSpinner);
        vibrationSwitch = this.findViewById(R.id.vibrationsSwitch);
        pathButton = this.findViewById(R.id.songPathButton);

        // Load theme selection spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.theme_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(adapter);

        // Load path button text
        Uri savedUri = AppPreferences.getMusicDirectoryUri(this);
        if (savedUri != null) {
            pathButton.setText(savedUri.toString());
        }

        // Set inputs to saved pref values (to avoid getting overridden)
        int savedThemeIndex = AppPreferences.getThemeIndex(this);
        themeSpinner.setSelection(savedThemeIndex);

        // Theme spinner logic
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

        // Path button logic
        directoryPickLauncher = registerForActivityResult(
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
                        pathButton.setText(uri.toString());
                    }
                }
        );
        pathButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            directoryPickLauncher.launch(intent);
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}