package com.example.imageanalysis;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportActivity extends AppCompatActivity {

    private static final String TAG = "ExportActivity";
    
    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;
    private PdfListAdapter adapter;
    private List<File> pdfFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        pdfFiles = new ArrayList<>();
        adapter = new PdfListAdapter(pdfFiles, this::onPdfItemClick);
        recyclerView.setAdapter(adapter);

        // Load PDFs
        loadPdfFiles();
    }

    private void loadPdfFiles() {
        File documentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        
        if (documentsDir == null || !documentsDir.exists()) {
            showEmptyState();
            return;
        }

        File[] files = documentsDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".pdf") && name.startsWith("SFC_Report_"));

        if (files == null || files.length == 0) {
            showEmptyState();
            return;
        }

        // Sort by date (newest first)
        Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        pdfFiles.clear();
        pdfFiles.addAll(Arrays.asList(files));
        adapter.notifyDataSetChanged();

        recyclerView.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    private void onPdfItemClick(File pdfFile) {
        // Show options dialog: View or Download
        new android.app.AlertDialog.Builder(this)
                .setTitle(pdfFile.getName())
                .setMessage("Choose an action:")
                .setPositiveButton("📥 Download", (dialog, which) -> downloadPdf(pdfFile))
                .setNegativeButton("👁️ View", (dialog, which) -> viewPdf(pdfFile))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void viewPdf(File pdfFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri pdfUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".provider",
                    pdfFile
                );
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                pdfUri = Uri.fromFile(pdfFile);
            }

            intent.setDataAndType(pdfUri, "application/pdf");
            startActivity(Intent.createChooser(intent, "View PDF"));
        } catch (Exception e) {
            Log.e(TAG, "Error viewing PDF: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadPdf(File pdfFile) {
        new Thread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ - Use MediaStore
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, pdfFile.getName());
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    
                    if (uri != null) {
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        FileInputStream fis = new FileInputStream(pdfFile);
                        
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        
                        fis.close();
                        os.close();

                        runOnUiThread(() -> {
                            Toast.makeText(this, "✓ Downloaded to Downloads folder", Toast.LENGTH_LONG).show();
                        });
                    }
                } else {
                    // Android 9 and below
                    File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File destFile = new File(downloadDir, pdfFile.getName());

                    FileInputStream fis = new FileInputStream(pdfFile);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(destFile);

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }

                    fis.close();
                    fos.close();

                    runOnUiThread(() -> {
                        Toast.makeText(this, "✓ Downloaded to: " + destFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error downloading PDF: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPdfFiles();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // PDF List Adapter
    private static class PdfListAdapter extends RecyclerView.Adapter<PdfListAdapter.ViewHolder> {
        
        private List<File> pdfFiles;
        private OnPdfClickListener listener;

        interface OnPdfClickListener {
            void onPdfClick(File pdfFile);
        }

        PdfListAdapter(List<File> pdfFiles, OnPdfClickListener listener) {
            this.pdfFiles = pdfFiles;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pdf, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            File pdfFile = pdfFiles.get(position);
            holder.bind(pdfFile, listener);
        }

        @Override
        public int getItemCount() {
            return pdfFiles.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvFileName;
            TextView tvFileSize;
            TextView tvFileDate;
            Button btnView;
            Button btnDownload;

            ViewHolder(View itemView) {
                super(itemView);
                tvFileName = itemView.findViewById(R.id.tvFileName);
                tvFileSize = itemView.findViewById(R.id.tvFileSize);
                tvFileDate = itemView.findViewById(R.id.tvFileDate);
                btnView = itemView.findViewById(R.id.btnView);
                btnDownload = itemView.findViewById(R.id.btnDownload);
            }

            void bind(File pdfFile, OnPdfClickListener listener) {
                tvFileName.setText(pdfFile.getName());
                
                // File size
                long sizeInKb = pdfFile.length() / 1024;
                tvFileSize.setText(sizeInKb + " KB");
                
                // File date
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                tvFileDate.setText(sdf.format(new Date(pdfFile.lastModified())));

                btnView.setOnClickListener(v -> {
                    if (listener != null) {
                        viewPdf(pdfFile, v.getContext());
                    }
                });

                btnDownload.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPdfClick(pdfFile);
                    }
                });

                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPdfClick(pdfFile);
                    }
                });
            }

            private void viewPdf(File pdfFile, android.content.Context context) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri pdfUri;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        pdfUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.getApplicationContext().getPackageName() + ".provider",
                            pdfFile
                        );
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } else {
                        pdfUri = Uri.fromFile(pdfFile);
                    }

                    intent.setDataAndType(pdfUri, "application/pdf");
                    context.startActivity(Intent.createChooser(intent, "View PDF"));
                } catch (Exception e) {
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
