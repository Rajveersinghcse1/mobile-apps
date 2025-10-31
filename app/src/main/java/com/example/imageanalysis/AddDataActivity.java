package com.example.imageanalysis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddDataActivity extends AppCompatActivity {

    private static final String TAG = "AddDataActivity";
    private static final String PREFS_NAME = "SFCPrefs";
    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView ivPreview;
    private TextView tvNoImage;
    private Button btnSelectImage;
    private Button btnSave;
    
    private TextInputEditText etBaseName;
    private TextInputEditText etName;
    private TextInputEditText etId;
    private TextInputEditText etDepartment;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextInputEditText etAdditional;

    private SharedPreferences sharedPreferences;
    private Bitmap selectedImage;
    private Uri selectedImageUri;
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_data);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize views
        ivPreview = findViewById(R.id.ivPreview);
        tvNoImage = findViewById(R.id.tvNoImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSave = findViewById(R.id.btnSave);
        
        etBaseName = findViewById(R.id.etBaseName);
        etName = findViewById(R.id.etName);
        etId = findViewById(R.id.etId);
        etDepartment = findViewById(R.id.etDepartment);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etAdditional = findViewById(R.id.etAdditional);

        // Setup image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        loadSelectedImage();
                    }
                });

        // Check if image passed from HomeActivity
        checkForPassedImage();

        // Button listeners
        btnSelectImage.setOnClickListener(v -> openImagePicker());
        btnSave.setOnClickListener(v -> saveData());
    }

    private void checkForPassedImage() {
        Intent intent = getIntent();
        if (intent.hasExtra("image_path")) {
            String imagePath = intent.getStringExtra("image_path");
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                selectedImage = BitmapFactory.decodeFile(imagePath);
                if (selectedImage != null) {
                    ivPreview.setImageBitmap(selectedImage);
                    tvNoImage.setVisibility(View.GONE);
                    
                    // Auto-generate base name from timestamp
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    etBaseName.setText("capture_" + timestamp);
                }
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void loadSelectedImage() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            selectedImage = BitmapFactory.decodeStream(inputStream);
            
            if (selectedImage != null) {
                ivPreview.setImageBitmap(selectedImage);
                tvNoImage.setVisibility(View.GONE);
                Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show();
            }
            
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveData() {
        // Validate inputs
        String baseName = etBaseName.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String id = etId.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String additional = etAdditional.getText().toString().trim();

        if (baseName.isEmpty()) {
            Toast.makeText(this, "Please enter a file name", Toast.LENGTH_SHORT).show();
            etBaseName.requestFocus();
            return;
        }

        if (selectedImage == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter at least a name", Toast.LENGTH_SHORT).show();
            etName.requestFocus();
            return;
        }

        // Get paths from settings
        String imagePath = SettingsActivity.getImagePath(sharedPreferences);
        String dataPath = SettingsActivity.getDataPath(sharedPreferences);

        if (imagePath.isEmpty() || dataPath.isEmpty()) {
            Toast.makeText(this, "Please configure paths in Settings first", Toast.LENGTH_LONG).show();
            return;
        }

        // Show progress
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        new Thread(() -> {
            try {
                // Ensure directories exist
                File imageDir = new File(imagePath);
                File dataDir = new File(dataPath);
                
                if (!imageDir.exists()) imageDir.mkdirs();
                if (!dataDir.exists()) dataDir.mkdirs();

                // Save image
                File imageFile = new File(imageDir, baseName + ".jpg");
                FileOutputStream fos = new FileOutputStream(imageFile);
                selectedImage.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
                fos.close();
                
                Log.d(TAG, "Image saved: " + imageFile.getAbsolutePath());

                // Create JSON object
                JsonObject jsonData = new JsonObject();
                jsonData.addProperty("name", name);
                if (!id.isEmpty()) jsonData.addProperty("id", id);
                if (!department.isEmpty()) jsonData.addProperty("department", department);
                if (!email.isEmpty()) jsonData.addProperty("email", email);
                if (!phone.isEmpty()) jsonData.addProperty("phone", phone);
                if (!additional.isEmpty()) jsonData.addProperty("additionalInfo", additional);
                
                // Add metadata
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                jsonData.addProperty("createdAt", timestamp);
                jsonData.addProperty("fileName", baseName);

                // Save JSON file
                File jsonFile = new File(dataDir, baseName + ".json");
                FileOutputStream jsonFos = new FileOutputStream(jsonFile);
                jsonFos.write(new Gson().toJson(jsonData).getBytes());
                jsonFos.flush();
                jsonFos.close();
                
                Log.d(TAG, "JSON saved: " + jsonFile.getAbsolutePath());

                // Success
                runOnUiThread(() -> {
                    Toast.makeText(this, "✓ Data saved successfully!\n" + baseName, Toast.LENGTH_LONG).show();
                    
                    // Clear form and image
                    clearForm();
                    
                    btnSave.setEnabled(true);
                    btnSave.setText("💾 Save Data");
                });

            } catch (Exception e) {
                Log.e(TAG, "Error saving data: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("💾 Save Data");
                });
            }
        }).start();
    }

    private void clearForm() {
        etBaseName.setText("");
        etName.setText("");
        etId.setText("");
        etDepartment.setText("");
        etEmail.setText("");
        etPhone.setText("");
        etAdditional.setText("");
        
        selectedImage = null;
        selectedImageUri = null;
        ivPreview.setImageBitmap(null);
        tvNoImage.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
