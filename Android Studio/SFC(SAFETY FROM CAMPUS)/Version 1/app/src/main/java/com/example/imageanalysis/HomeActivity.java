package com.example.imageanalysis;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String PREFS_NAME = "SFCPrefs";

    private PreviewView previewView;
    private ImageView imagePreview;
    private TextView tvPlaceholder;
    private TextView tvAnalysisResult;
    private Button btnStartCamera;
    private Button btnStopCamera;
    private Button btnCaptureImage;
    private Button btnAnalyzeMatch;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private boolean isCameraStarted = false;
    private ExecutorService cameraExecutor;
    private File outputDirectory;
    private SharedPreferences sharedPreferences;
    private File lastCapturedFile;
    private JsonObject matchedData;
    private AdvancedImageAnalyzer advancedAnalyzer;
    private Bitmap lastCapturedBitmap;
    private HistoryManager historyManager;
    private ImageMatcher imageMatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        previewView = findViewById(R.id.previewView);
        imagePreview = findViewById(R.id.imagePreview);
        tvPlaceholder = findViewById(R.id.tvPlaceholder);
        tvAnalysisResult = findViewById(R.id.tvAnalysisResult);
        btnStartCamera = findViewById(R.id.btnStartCamera);
        btnStopCamera = findViewById(R.id.btnStopCamera);
        btnCaptureImage = findViewById(R.id.btnCaptureImage);
        btnAnalyzeMatch = findViewById(R.id.btnAnalyzeMatch);

        btnStopCamera.setEnabled(false);
        btnCaptureImage.setEnabled(false);

        cameraExecutor = Executors.newSingleThreadExecutor();
        outputDirectory = getOutputDirectory();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        setupDefaultFolders();
        historyManager = new HistoryManager(this);
        advancedAnalyzer = new AdvancedImageAnalyzer();

        try {
            imageMatcher = new ImageMatcher(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize ImageMatcher", e);
            Toast.makeText(this, "Failed to initialize facial recognition.", Toast.LENGTH_LONG).show();
        }


        btnStartCamera.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                startCamera();
            } else {
                requestCameraPermission();
            }
        });

        btnStopCamera.setOnClickListener(v -> stopCamera());
        btnCaptureImage.setOnClickListener(v -> captureImage());
        btnAnalyzeMatch.setOnClickListener(v -> findMatchingImage());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_data) {
            Intent intent = new Intent(this, AddDataActivity.class);
            if (lastCapturedFile != null && lastCapturedFile.exists()) {
                intent.putExtra("image_path", lastCapturedFile.getAbsolutePath());
            }
            startActivity(intent);
            return true;
        } else if (id == R.id.action_store) {
            Intent intent = new Intent(this, StoreActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_history) {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_alert) {
            showAlertDialog();
            return true;
        } else if (id == R.id.action_export) {
            Intent intent = new Intent(this, ExportActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private File getOutputDirectory() {
        File mediaDir = new File(getExternalMediaDirs()[0], getString(R.string.app_name));
        if (!mediaDir.exists()) {
            if (!mediaDir.mkdirs()) {
                Log.e(TAG, "Failed to create media directory");
            }
        }
        return mediaDir.exists() ? mediaDir : getFilesDir();
    }

    private void setupDefaultFolders() {
        String imagePath = sharedPreferences.getString("image_path", "");
        String dataPath = sharedPreferences.getString("data_path", "");

        if (imagePath.isEmpty() || dataPath.isEmpty()) {
            File externalFilesDir = getExternalFilesDir(null);

            if (externalFilesDir != null) {
                File sfcFolder = new File(externalFilesDir, "SFC");
                File imagesFolder = new File(sfcFolder, "ReferenceImages");
                if (!imagesFolder.exists()) {
                    if (!imagesFolder.mkdirs()) {
                        Log.e(TAG, "Failed to create ReferenceImages directory");
                    }
                }

                File dataFolder = new File(sfcFolder, "ReferenceData");
                if (!dataFolder.exists()) {
                    if (!dataFolder.mkdirs()) {
                        Log.e(TAG, "Failed to create ReferenceData directory");
                    }
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("image_path", imagesFolder.getAbsolutePath());
                editor.putString("data_path", dataFolder.getAbsolutePath());
                editor.apply();

                Toast.makeText(this,
                        "Default folders created:\n" + sfcFolder.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "External storage not available");
                Toast.makeText(this, "External storage not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        imagePreview.setImageBitmap(null);
        tvPlaceholder.setVisibility(View.GONE);
        tvAnalysisResult.setVisibility(View.GONE);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
                isCameraStarted = true;
                updateButtonStates();
                previewView.setVisibility(View.VISIBLE);
                imagePreview.setVisibility(View.GONE);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage(), e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind camera use cases: " + e.getMessage(), e);
        }
    }

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            isCameraStarted = false;
            updateButtonStates();
            previewView.setVisibility(View.GONE);
        }
    }

    private void captureImage() {
        if (imageCapture == null) return;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
        String fileName = "IMG_" + timestamp + ".jpg";
        File photoFile = new File(outputDirectory, fileName);

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                        if (bitmap != null) {
                            lastCapturedFile = photoFile;
                            lastCapturedBitmap = bitmap;

                            runOnUiThread(() -> {
                                previewView.setVisibility(View.GONE);
                                tvPlaceholder.setVisibility(View.GONE);
                                imagePreview.setImageBitmap(bitmap);
                                imagePreview.setVisibility(View.VISIBLE);
                                btnCaptureImage.setEnabled(false);
                                btnStartCamera.setEnabled(true);
                                btnAnalyzeMatch.setVisibility(View.VISIBLE);
                                performAdvancedAnalysis(bitmap);
                            });
                        } else {
                            Log.e(TAG, "Failed to decode saved image file");
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        if (photoFile.exists()) {
                            if (!photoFile.delete()) {
                                Log.e(TAG, "Failed to delete failed photo file");
                            }
                        }
                    }
                });
    }

    private void updateButtonStates() {
        btnStartCamera.setEnabled(!isCameraStarted);
        btnStopCamera.setEnabled(isCameraStarted);
        btnCaptureImage.setEnabled(isCameraStarted);
    }

    private void performAdvancedAnalysis(Bitmap bitmap) {
        runOnUiThread(() -> {
            tvAnalysisResult.setText(R.string.performing_advanced_analysis);
            tvAnalysisResult.setVisibility(View.VISIBLE);
        });

        advancedAnalyzer.analyzeImage(bitmap, new AdvancedImageAnalyzer.AnalysisCallback() {
            @Override
            public void onSuccess(AdvancedImageAnalyzer.AnalysisResult result) {
                String report = result.getFormattedReport();
                runOnUiThread(() -> tvAnalysisResult.setText(report));
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Advanced analysis error: " + error);
                runOnUiThread(() -> tvAnalysisResult.setText(getString(R.string.analysis_error, error)));
            }
        });
    }

    private static class ImageCandidate {
        String name;
        Bitmap image;
        String filePath;

        ImageCandidate(String name, Bitmap image, String filePath) {
            this.name = name;
            this.image = image;
            this.filePath = filePath;
        }
    }

    // Inside HomeActivity.java

    private void findMatchingImage() {
        if (lastCapturedBitmap == null) {
            Toast.makeText(this, "Please capture an image first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (imageMatcher == null) {
            Toast.makeText(this, "Image matcher is not initialized.", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageFolderPath = sharedPreferences.getString("image_path", "");
        File imageFolder = new File(imageFolderPath);
        File[] imageFiles = imageFolder.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png"));

        if (imageFiles == null || imageFiles.length == 0) {
            tvAnalysisResult.setText("No reference images found in the directory.");
            tvAnalysisResult.setVisibility(View.VISIBLE);
            return;
        }

        double bestScore = -1.0;
        File bestMatchFile = null;

        // This loop is CPU-intensive and should be run in a background thread
        // for a better user experience. For simplicity, it's shown here directly.
        for (File imageFile : imageFiles) {
            // Load the reference image file into a Bitmap
            Bitmap referenceBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (referenceBitmap != null) {
                // *** FIX: Call the correct method, e.g., compareImages(Bitmap, Bitmap) ***
                double score = imageMatcher.compareImages(lastCapturedBitmap, referenceBitmap); // Assuming the method is compareImages

                if (score > bestScore) {
                    bestScore = score;
                    bestMatchFile = imageFile;
                }
                // It's good practice to recycle bitmaps you load manually to save memory
                referenceBitmap.recycle();
            }
        }

        if (bestMatchFile != null) {
            String resultText = String.format(Locale.US, "Best Match: %s\nScore: %.2f", bestMatchFile.getName(), bestScore);
            tvAnalysisResult.setText(resultText);
            // ... (rest of your logic for handling the match)
        } else {
            tvAnalysisResult.setText("Could not find a match.");
        }
        tvAnalysisResult.setVisibility(View.VISIBLE);
    }

    private String readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            if (fis.read(buffer) == -1) {
                Log.w(TAG, "End of file reached before all bytes could be read");
            }
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }

    private void displayMatchedResult(Bitmap matchedImage, JsonObject data, double similarity) {
        imagePreview.setImageBitmap(matchedImage);

        int similarityPercent = (int) (similarity * 100);
        StringBuilder result = new StringBuilder();
        result.append("═══ MATCH FOUND ═══\n\n");
        result.append("✓ Similarity Score: ").append(similarityPercent).append("%\n\n");
        result.append("📋 Retrieved Data:\n");

        for (String key : data.keySet()) {
            String value = data.get(key).getAsString();
            result.append("• ").append(formatKey(key)).append(": ").append(value).append("\n");
        }

        result.append("\n💾 Ready to export as PDF");

        tvAnalysisResult.setText(result.toString());
        tvAnalysisResult.setVisibility(View.VISIBLE);
    }

    private String formatKey(String key) {
        String formatted = key.replaceAll("([A-Z])", " $1").replaceAll("_", " ");
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }

    private void showExportPdfOption() {
        runOnUiThread(() -> {
            btnAnalyzeMatch.setText(R.string.export_to_pdf);
            btnAnalyzeMatch.setOnClickListener(v -> exportToPdf());
        });
    }

    private void exportToPdf() {
        if (matchedData == null) return;

        new Thread(() -> {
            try {
                PdfDocument pdfDocument = new PdfDocument();
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                Canvas canvas = page.getCanvas();
                Paint paint = new Paint();

                paint.setTextSize(24);
                paint.setFakeBoldText(true);
                canvas.drawText("SFC - Image Analysis Report", 50, 50, paint);

                Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) imagePreview.getDrawable()).getBitmap();
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 400, 300, true);
                canvas.drawBitmap(scaledBitmap, 50, 80, paint);

                paint.setTextSize(14);
                paint.setFakeBoldText(false);
                int yPos = 420;

                canvas.drawText("Analysis Data:", 50, yPos, paint);
                yPos += 30;

                for (String key : matchedData.keySet()) {
                    String text = key + ": " + matchedData.get(key).getAsString();
                    canvas.drawText(text, 50, yPos, paint);
                    yPos += 25;
                }

                pdfDocument.finishPage(page);

                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
                File pdfFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SFC_Report_" + timestamp + ".pdf");

                pdfDocument.writeTo(new FileOutputStream(pdfFile));
                pdfDocument.close();

                runOnUiThread(() -> Toast.makeText(this, "PDF saved: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                Log.e(TAG, "PDF export error: " + e.getMessage(), e);
            }
        }).start();
    }

    private void saveToHistory(String capturedImagePath, String matchedImagePath,
                               String matchedBaseName, JsonObject matchedData,
                               double similarity) {
        try {
            HistoryItem historyItem = new HistoryItem(capturedImagePath, matchedImagePath, matchedBaseName, matchedData, similarity);
            historyManager.addHistory(historyItem);
        } catch (Exception e) {
            Log.e(TAG, "Error saving to history: " + e.getMessage(), e);
        }
    }

    private void showAlertDialog() {
        String imagePath = SettingsActivity.getImagePath(sharedPreferences);
        String dataPath = SettingsActivity.getDataPath(sharedPreferences);

        StringBuilder message = new StringBuilder();
        message.append("\n");
        message.append(getString(R.string.storage_paths_title)).append("\n");
        message.append(imagePath.isEmpty() ? getString(R.string.images_not_configured) : getString(R.string.images_configured)).append("\n");
        message.append(dataPath.isEmpty() ? getString(R.string.data_not_configured) : getString(R.string.data_configured)).append("\n\n");
        message.append(getString(R.string.camera_status_title)).append(" ").append(isCameraStarted ? getString(R.string.camera_active) : getString(R.string.camera_stopped)).append("\n");
        message.append(getString(R.string.captured_image_status_title)).append(" ").append(lastCapturedFile != null && lastCapturedFile.exists() ? getString(R.string.image_ready) : getString(R.string.image_none)).append("\n");

        File imageDir = new File(imagePath);
        if (imageDir.exists()) {
            File[] images = imageDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg");
            });
            message.append("\n").append(getString(R.string.statistics_title)).append("\n");
            message.append(getString(R.string.images_count, images != null ? images.length : 0)).append("\n");
        }

        File dataDir = new File(dataPath);
        if (dataDir.exists()) {
            File[] jsonFiles = dataDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            message.append(getString(R.string.data_files_count, jsonFiles != null ? jsonFiles.length : 0)).append("\n");
        }

        File documentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (documentsDir != null && documentsDir.exists()) {
            File[] pdfs = documentsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf") && name.startsWith("SFC_Report_"));
            message.append(getString(R.string.pdf_reports_count, pdfs != null ? pdfs.length : 0)).append("\n");
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.system_alert_title)
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton(R.string.settings, (dialog, which) -> {
                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
