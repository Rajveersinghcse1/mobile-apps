package com.example.imageanalysis;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SFCPrefs";
    private static final String KEY_IMAGE_PATH = "image_path";
    private static final String KEY_DATA_PATH = "data_path";

    private TextInputEditText etImagePath;
    private TextInputEditText etDataPath;
    private Button btnSaveSettings;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        // Initialize views
        etImagePath = findViewById(R.id.etImagePath);
        etDataPath = findViewById(R.id.etDataPath);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load saved settings
        loadSettings();

        // Set default paths if empty
        setDefaultPaths();

        // Save button click listener
        btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        String imagePath = sharedPreferences.getString(KEY_IMAGE_PATH, "");
        String dataPath = sharedPreferences.getString(KEY_DATA_PATH, "");

        etImagePath.setText(imagePath);
        etDataPath.setText(dataPath);
    }

    private void setDefaultPaths() {
        // Only set defaults if fields are empty after loading
        String currentImagePath = etImagePath.getText().toString();
        String currentDataPath = etDataPath.getText().toString();
        
        if (currentImagePath.isEmpty()) {
            File externalFilesDir = getExternalFilesDir(null);
            if (externalFilesDir != null) {
                String defaultImagePath = new File(externalFilesDir, "SFC/ReferenceImages").getAbsolutePath();
                etImagePath.setText(defaultImagePath);
            }
        }

        if (currentDataPath.isEmpty()) {
            File externalFilesDir = getExternalFilesDir(null);
            if (externalFilesDir != null) {
                String defaultDataPath = new File(externalFilesDir, "SFC/ReferenceData").getAbsolutePath();
                etDataPath.setText(defaultDataPath);
            }
        }
    }

    private void saveSettings() {
        String imagePath = etImagePath.getText().toString().trim();
        String dataPath = etDataPath.getText().toString().trim();

        if (imagePath.isEmpty() || dataPath.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create directories if they don't exist
        File imageDir = new File(imagePath);
        File dataDir = new File(dataPath);

        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        // Save to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_IMAGE_PATH, imagePath);
        editor.putString(KEY_DATA_PATH, dataPath);
        editor.apply();

        Toast.makeText(this, "Settings saved successfully!\n" +
                "Image Path: " + imagePath + "\n" +
                "Data Path: " + dataPath, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static String getImagePath(SharedPreferences prefs) {
        return prefs.getString(KEY_IMAGE_PATH, "");
    }

    public static String getDataPath(SharedPreferences prefs) {
        return prefs.getString(KEY_DATA_PATH, "");
    }
}
