package com.example.imageanalysis;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.gson.JsonObject;

import java.io.File;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";

    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;
    private TextView tvHistoryStats;
    private Button btnClearHistory;
    private TextInputEditText etSearch;

    private HistoryManager historyManager;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize HistoryManager
        historyManager = new HistoryManager(this);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvHistoryStats = findViewById(R.id.tvHistoryStats);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        etSearch = findViewById(R.id.etSearch);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyItems = historyManager.getAllHistory();
        adapter = new HistoryAdapter(historyItems, new HistoryAdapter.OnHistoryItemListener() {
            @Override
            public void onItemClick(HistoryItem item) {
                showDetailDialog(item);
            }

            @Override
            public void onDeleteClick(HistoryItem item) {
                showDeleteConfirmation(item);
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

        // Clear all button
        btnClearHistory.setOnClickListener(v -> showClearAllConfirmation());

        // Load data
        loadHistory();
    }

    private void loadHistory() {
        historyItems = historyManager.getAllHistory();
        adapter.updateItems(historyItems);

        if (historyItems.isEmpty()) {
            showEmptyState();
            tvHistoryStats.setText("No history records");
            btnClearHistory.setEnabled(false);
        } else {
            hideEmptyState();
            updateStats();
            btnClearHistory.setEnabled(true);
        }
    }

    private void updateStats() {
        HistoryManager.HistoryStats stats = historyManager.getStatistics();
        String statsText = String.format("Total: %d | Avg Similarity: %.1f%%",
                stats.totalRecords,
                stats.averageSimilarity * 100);
        tvHistoryStats.setText(statsText);
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showDetailDialog(HistoryItem item) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_history_detail);
        dialog.getWindow().setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        ImageView ivCapturedDetail = dialog.findViewById(R.id.ivCapturedDetail);
        ImageView ivMatchedDetail = dialog.findViewById(R.id.ivMatchedDetail);
        TextView tvDetailSimilarity = dialog.findViewById(R.id.tvDetailSimilarity);
        TextView tvDetailTime = dialog.findViewById(R.id.tvDetailTime);
        TextView tvDetailData = dialog.findViewById(R.id.tvDetailData);
        Button btnClose = dialog.findViewById(R.id.btnCloseHistoryDetail);

        // Load captured image
        if (item.getCapturedImagePath() != null) {
            File capturedFile = new File(item.getCapturedImagePath());
            if (capturedFile.exists()) {
                Bitmap capturedBitmap = BitmapFactory.decodeFile(capturedFile.getAbsolutePath());
                if (capturedBitmap != null) {
                    ivCapturedDetail.setImageBitmap(capturedBitmap);
                }
            }
        }

        // Load matched image
        if (item.getMatchedImagePath() != null) {
            File matchedFile = new File(item.getMatchedImagePath());
            if (matchedFile.exists()) {
                Bitmap matchedBitmap = BitmapFactory.decodeFile(matchedFile.getAbsolutePath());
                if (matchedBitmap != null) {
                    ivMatchedDetail.setImageBitmap(matchedBitmap);
                }
            }
        }

        // Set similarity and time
        tvDetailSimilarity.setText("✓ Similarity: " + item.getFormattedSimilarity());
        tvDetailTime.setText("📅 " + item.getFormattedDate() + " at " + item.getFormattedTime());

        // Show JSON data
        StringBuilder dataText = new StringBuilder();
        dataText.append("═══ MATCHED DATA ═══\n\n");
        
        if (item.getMatchedData() != null) {
            JsonObject jsonData = item.getMatchedData();
            for (String key : jsonData.keySet()) {
                String value = jsonData.get(key).getAsString();
                dataText.append("• ").append(formatKey(key)).append(": ")
                        .append(value).append("\n");
            }
        } else {
            dataText.append("No data available");
        }

        dataText.append("\n═══ FILE INFO ═══\n");
        dataText.append("• File Name: ").append(item.getMatchedBaseName()).append("\n");
        dataText.append("• Record ID: ").append(item.getId()).append("\n");

        tvDetailData.setText(dataText.toString());

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDeleteConfirmation(HistoryItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete History Record")
                .setMessage("Delete this history record?\n\n" + item.getMatchedName() + 
                           " (" + item.getFormattedDate() + ")")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean success = historyManager.deleteHistory(item.getId());
                    if (success) {
                        Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
                        loadHistory();
                    } else {
                        Toast.makeText(this, "Failed to delete record", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearAllConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All History")
                .setMessage("This will delete all " + historyItems.size() + 
                           " history records. This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    boolean success = historyManager.clearAllHistory();
                    if (success) {
                        Toast.makeText(this, "All history cleared", Toast.LENGTH_SHORT).show();
                        loadHistory();
                    } else {
                        Toast.makeText(this, "Failed to clear history", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatKey(String key) {
        String formatted = key.replaceAll("([A-Z])", " $1")
                .replaceAll("_", " ");
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload history in case it was updated
        loadHistory();
    }
}
