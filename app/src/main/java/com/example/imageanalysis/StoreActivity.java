package com.example.imageanalysis;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StoreActivity extends AppCompatActivity {

    private static final String TAG = "StoreActivity";
    private static final String PREFS_NAME = "SFCPrefs";

    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;
    private TextView tvStoreInfo;
    private Button btnRefresh;
    private TextInputEditText etSearch;

    private SharedPreferences sharedPreferences;
    private StoreItemAdapter adapter;
    private List<StoreItemAdapter.StoreItem> storeItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

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
        recyclerView = findViewById(R.id.recyclerView);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvStoreInfo = findViewById(R.id.tvStoreInfo);
        btnRefresh = findViewById(R.id.btnRefresh);
        etSearch = findViewById(R.id.etSearch);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        storeItems = new ArrayList<>();
        adapter = new StoreItemAdapter(storeItems, this::showPreviewDialog);
        adapter.setActionListener(new StoreItemAdapter.OnItemActionListener() {
            @Override
            public void onEditClick(StoreItemAdapter.StoreItem item, int position) {
                editItem(item);
            }

            @Override
            public void onDeleteClick(StoreItemAdapter.StoreItem item, int position) {
                deleteItem(item, position);
            }
        });
        recyclerView.setAdapter(adapter);

        // Search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Refresh button
        btnRefresh.setOnClickListener(v -> loadData());

        // Load initial data
        loadData();
    }

    private void loadData() {
        String imagePath = SettingsActivity.getImagePath(sharedPreferences);
        String dataPath = SettingsActivity.getDataPath(sharedPreferences);

        if (imagePath.isEmpty() || dataPath.isEmpty()) {
            Toast.makeText(this, "Please configure paths in Settings first", Toast.LENGTH_LONG).show();
            showEmptyState();
            return;
        }

        btnRefresh.setEnabled(false);
        btnRefresh.setText("⏳");
        tvStoreInfo.setText("Loading...");

        new Thread(() -> {
            try {
                File imageDir = new File(imagePath);
                File dataDir = new File(dataPath);

                Map<String, StoreItemAdapter.StoreItem> itemMap = new HashMap<>();

                // Scan image files
                if (imageDir.exists() && imageDir.isDirectory()) {
                    File[] imageFiles = imageDir.listFiles((dir, name) -> {
                        String lower = name.toLowerCase();
                        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
                    });

                    if (imageFiles != null) {
                        for (File imageFile : imageFiles) {
                            String fileName = imageFile.getName();
                            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

                            StoreItemAdapter.StoreItem item = itemMap.get(baseName);
                            if (item == null) {
                                item = new StoreItemAdapter.StoreItem(baseName);
                                itemMap.put(baseName, item);
                            }

                            item.imageFile = imageFile;
                            item.hasImage = true;
                        }
                    }
                }

                // Scan JSON files
                if (dataDir.exists() && dataDir.isDirectory()) {
                    File[] jsonFiles = dataDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

                    if (jsonFiles != null) {
                        Gson gson = new Gson();
                        for (File jsonFile : jsonFiles) {
                            String fileName = jsonFile.getName();
                            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

                            StoreItemAdapter.StoreItem item = itemMap.get(baseName);
                            if (item == null) {
                                item = new StoreItemAdapter.StoreItem(baseName);
                                itemMap.put(baseName, item);
                            }

                            item.jsonFile = jsonFile;
                            item.hasData = true;

                            // Load JSON data
                            try {
                                String jsonContent = readFile(jsonFile);
                                item.jsonData = gson.fromJson(jsonContent, JsonObject.class);
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading JSON: " + jsonFile.getName(), e);
                            }
                        }
                    }
                }

                // Convert to list
                List<StoreItemAdapter.StoreItem> items = new ArrayList<>(itemMap.values());
                
                // Sort: complete items first
                items.sort((a, b) -> {
                    if (a.isComplete() && !b.isComplete()) return -1;
                    if (!a.isComplete() && b.isComplete()) return 1;
                    return a.baseName.compareTo(b.baseName);
                });

                // Count stats
                int totalItems = items.size();
                int completeItems = 0;
                int missingImages = 0;
                int missingData = 0;

                for (StoreItemAdapter.StoreItem item : items) {
                    if (item.isComplete()) completeItems++;
                    else if (item.hasImage && !item.hasData) missingData++;
                    else if (!item.hasImage && item.hasData) missingImages++;
                }

                int finalCompleteItems = completeItems;
                int finalTotalItems = totalItems;
                int finalMissingImages = missingImages;
                int finalMissingData = missingData;

                runOnUiThread(() -> {
                    storeItems = items;
                    adapter.updateItems(items);

                    if (totalItems == 0) {
                        showEmptyState();
                        tvStoreInfo.setText("No data found");
                    } else {
                        hideEmptyState();
                        tvStoreInfo.setText(String.format("Total: %d | ✓ Complete: %d | ⚠ Issues: %d",
                                finalTotalItems, finalCompleteItems, 
                                (finalMissingImages + finalMissingData)));
                    }

                    btnRefresh.setEnabled(true);
                    btnRefresh.setText("🔄 Refresh");
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading data: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnRefresh.setEnabled(true);
                    btnRefresh.setText("🔄 Refresh");
                });
            }
        }).start();
    }

    private String readFile(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return new String(data, StandardCharsets.UTF_8);
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showPreviewDialog(StoreItemAdapter.StoreItem item) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_preview);
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        ImageView ivPreview = dialog.findViewById(R.id.ivPreviewDialog);
        TextView tvDataPreview = dialog.findViewById(R.id.tvDataPreview);
        Button btnClose = dialog.findViewById(R.id.btnCloseDialog);

        // Load image
        if (item.hasImage && item.imageFile != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(item.imageFile.getAbsolutePath());
            if (bitmap != null) {
                ivPreview.setImageBitmap(bitmap);
            }
        } else {
            ivPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Show JSON data
        StringBuilder dataText = new StringBuilder();
        dataText.append("📄 File: ").append(item.baseName).append("\n\n");

        if (item.hasData && item.jsonData != null) {
            dataText.append("═══ DATA ═══\n\n");
            for (String key : item.jsonData.keySet()) {
                String value = item.jsonData.get(key).getAsString();
                dataText.append("• ").append(formatKey(key)).append(": ")
                        .append(value).append("\n");
            }
        } else {
            dataText.append("⚠ No JSON data available\n");
        }

        if (!item.hasImage) {
            dataText.append("\n⚠ Image file missing");
        }

        tvDataPreview.setText(dataText.toString());

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String formatKey(String key) {
        String formatted = key.replaceAll("([A-Z])", " $1")
                .replaceAll("_", " ");
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }

    private void editItem(StoreItemAdapter.StoreItem item) {
        if (!item.hasData || item.jsonData == null) {
            Toast.makeText(this, "No data to edit for: " + item.baseName, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create edit dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_item);
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Find views
        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        TextInputEditText etName = dialog.findViewById(R.id.etName);
        TextInputEditText etId = dialog.findViewById(R.id.etId);
        TextInputEditText etDepartment = dialog.findViewById(R.id.etDepartment);
        TextInputEditText etEmail = dialog.findViewById(R.id.etEmail);
        TextInputEditText etPhone = dialog.findViewById(R.id.etPhone);
        TextInputEditText etAdditionalInfo = dialog.findViewById(R.id.etAdditionalInfo);
        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        tvTitle.setText("Edit: " + item.baseName);

        // Pre-fill existing data
        if (item.jsonData.has("name")) {
            etName.setText(item.jsonData.get("name").getAsString());
        }
        if (item.jsonData.has("id")) {
            etId.setText(item.jsonData.get("id").getAsString());
        }
        if (item.jsonData.has("department")) {
            etDepartment.setText(item.jsonData.get("department").getAsString());
        }
        if (item.jsonData.has("email")) {
            etEmail.setText(item.jsonData.get("email").getAsString());
        }
        if (item.jsonData.has("phone")) {
            etPhone.setText(item.jsonData.get("phone").getAsString());
        }
        if (item.jsonData.has("additionalInfo")) {
            etAdditionalInfo.setText(item.jsonData.get("additionalInfo").getAsString());
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String id = etId.getText().toString().trim();

            if (name.isEmpty() || id.isEmpty()) {
                Toast.makeText(this, "Name and ID are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update JSON data
            new Thread(() -> {
                try {
                    JsonObject updatedData = new JsonObject();
                    updatedData.addProperty("name", name);
                    updatedData.addProperty("id", id);
                    updatedData.addProperty("department", etDepartment.getText().toString().trim());
                    updatedData.addProperty("email", etEmail.getText().toString().trim());
                    updatedData.addProperty("phone", etPhone.getText().toString().trim());
                    updatedData.addProperty("additionalInfo", etAdditionalInfo.getText().toString().trim());

                    // Save to JSON file
                    String jsonString = new Gson().toJson(updatedData);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(item.jsonFile);
                    fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
                    fos.close();

                    runOnUiThread(() -> {
                        Toast.makeText(this, "✓ Updated: " + item.baseName, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadData(); // Reload to show updated data
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error updating item: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });

        dialog.show();
    }

    private void deleteItem(StoreItemAdapter.StoreItem item, int position) {
        // Show confirmation dialog
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Delete '" + item.baseName + "'?\n\nThis will delete:\n" +
                        (item.hasImage ? "✓ Image file\n" : "") +
                        (item.hasData ? "✓ JSON data file" : ""))
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(() -> {
                        boolean success = true;
                        StringBuilder result = new StringBuilder();

                        // Delete image file
                        if (item.hasImage && item.imageFile != null && item.imageFile.exists()) {
                            if (item.imageFile.delete()) {
                                result.append("✓ Image deleted\n");
                            } else {
                                result.append("✗ Failed to delete image\n");
                                success = false;
                            }
                        }

                        // Delete JSON file
                        if (item.hasData && item.jsonFile != null && item.jsonFile.exists()) {
                            if (item.jsonFile.delete()) {
                                result.append("✓ Data deleted\n");
                            } else {
                                result.append("✗ Failed to delete data\n");
                                success = false;
                            }
                        }

                        boolean finalSuccess = success;
                        String finalMessage = result.toString();

                        runOnUiThread(() -> {
                            if (finalSuccess) {
                                Toast.makeText(this, "✓ Deleted: " + item.baseName, Toast.LENGTH_SHORT).show();
                                loadData(); // Reload data
                            } else {
                                Toast.makeText(this, "Partial delete:\n" + finalMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
