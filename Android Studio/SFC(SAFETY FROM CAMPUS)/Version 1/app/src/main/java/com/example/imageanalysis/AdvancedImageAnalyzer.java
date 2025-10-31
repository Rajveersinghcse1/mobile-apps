package com.example.imageanalysis;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Advanced image analyzer combining multiple ML techniques
 */
public class AdvancedImageAnalyzer {

    private static final String TAG = "AdvancedImageAnalyzer";
    private final ImageLabeler imageLabeler;

    public AdvancedImageAnalyzer() {
        ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build();
        this.imageLabeler = ImageLabeling.getClient(options);
    }

    /**
     * Perform comprehensive image analysis
     */
    public void analyzeImage(Bitmap bitmap, AnalysisCallback callback) {
        if (bitmap == null) {
            callback.onError("Bitmap is null");
            return;
        }

        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            imageLabeler.process(image)
                    .addOnSuccessListener(labels -> {
                        AnalysisResult result = processLabels(labels, bitmap);
                        callback.onSuccess(result);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Analysis failed: " + e.getMessage());
                        callback.onError(e.getMessage());
                    });

        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Process ML Kit labels and create comprehensive analysis
     */
    private AnalysisResult processLabels(List<ImageLabel> labels, Bitmap bitmap) {
        AnalysisResult result = new AnalysisResult();

        // Extract labels with confidence scores
        for (ImageLabel label : labels) {
            result.addLabel(label.getText(), label.getConfidence());
        }

        // Analyze image properties
        result.width = bitmap.getWidth();
        result.height = bitmap.getHeight();
        result.aspectRatio = (double) bitmap.getWidth() / bitmap.getHeight();

        // Analyze brightness
        result.brightness = calculateBrightness(bitmap);

        // Detect dominant colors
        result.dominantColors = detectDominantColors(bitmap);

        // Categorize image content
        result.category = categorizeImage(labels);

        // Calculate quality score
        result.qualityScore = calculateQualityScore(bitmap, labels);

        return result;
    }

    /**
     * Calculate average brightness of image
     */
    private double calculateBrightness(Bitmap bitmap) {
        long totalBrightness = 0;
        int sampleSize = 10; // Sample every 10th pixel for performance
        int sampleCount = 0;

        for (int y = 0; y < bitmap.getHeight(); y += sampleSize) {
            for (int x = 0; x < bitmap.getWidth(); x += sampleSize) {
                int pixel = bitmap.getPixel(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                totalBrightness += (r + g + b) / 3;
                sampleCount++;
            }
        }

        return sampleCount > 0 ? (double) totalBrightness / sampleCount / 255.0 : 0.5;
    }

    /**
     * Detect dominant colors in image
     */
    private List<String> detectDominantColors(Bitmap bitmap) {
        Map<String, Integer> colorCounts = new HashMap<>();
        int sampleSize = 20;

        for (int y = 0; y < bitmap.getHeight(); y += sampleSize) {
            for (int x = 0; x < bitmap.getWidth(); x += sampleSize) {
                int pixel = bitmap.getPixel(x, y);
                String colorName = getColorName(pixel);
                colorCounts.put(colorName, colorCounts.getOrDefault(colorName, 0) + 1);
            }
        }

        // Get top 3 colors
        List<String> dominantColors = new ArrayList<>();
        colorCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .forEach(e -> dominantColors.add(e.getKey()));

        return dominantColors;
    }

    /**
     * Get basic color name from pixel
     */
    private String getColorName(int pixel) {
        int r = (pixel >> 16) & 0xff;
        int g = (pixel >> 8) & 0xff;
        int b = pixel & 0xff;

        // Simple color categorization
        if (r > 200 && g > 200 && b > 200) return "White";
        if (r < 50 && g < 50 && b < 50) return "Black";
        if (r > g && r > b) return "Red";
        if (g > r && g > b) return "Green";
        if (b > r && b > g) return "Blue";
        if (r > 150 && g > 150 && b < 100) return "Yellow";
        if ((r + g + b) / 3 > 128) return "Light";
        return "Dark";
    }

    /**
     * Categorize image based on detected labels
     */
    private String categorizeImage(List<ImageLabel> labels) {
        if (labels.isEmpty()) return "Unknown";

        List<String> texts = new ArrayList<>();
        for (ImageLabel label : labels) {
            texts.add(label.getText().toLowerCase());
        }

        // Check for common categories
        if (containsAny(texts, "person", "face", "people", "human")) return "Person/People";
        if (containsAny(texts, "animal", "dog", "cat", "bird")) return "Animal";
        if (containsAny(texts, "plant", "flower", "tree", "leaf")) return "Nature";
        if (containsAny(texts, "building", "architecture", "house")) return "Architecture";
        if (containsAny(texts, "food", "meal", "dish")) return "Food";
        if (containsAny(texts, "vehicle", "car", "bike", "transport")) return "Vehicle";
        if (containsAny(texts, "document", "text", "paper")) return "Document";

        return labels.get(0).getText(); // Use top label as category
    }

    /**
     * Check if list contains any of the given strings
     */
    private boolean containsAny(List<String> list, String... items) {
        for (String item : items) {
            if (list.contains(item)) return true;
        }
        return false;
    }

    /**
     * Calculate image quality score
     */
    private double calculateQualityScore(Bitmap bitmap, List<ImageLabel> labels) {
        double score = 0.5; // Base score

        // Resolution factor
        int pixels = bitmap.getWidth() * bitmap.getHeight();
        if (pixels > 1000000) score += 0.2; // High resolution
        else if (pixels > 500000) score += 0.1; // Medium resolution

        // Confidence factor
        if (!labels.isEmpty()) {
            double avgConfidence = 0;
            for (ImageLabel label : labels) {
                avgConfidence += label.getConfidence();
            }
            avgConfidence /= labels.size();
            score += avgConfidence * 0.3;
        }

        return Math.min(1.0, score);
    }

    /**
     * Analysis result class
     */
    public static class AnalysisResult {
        public Map<String, Float> labels = new HashMap<>();
        public int width;
        public int height;
        public double aspectRatio;
        public double brightness;
        public List<String> dominantColors = new ArrayList<>();
        public String category;
        public double qualityScore;

        public void addLabel(String text, float confidence) {
            labels.put(text, confidence);
        }

        public String getFormattedReport() {
            StringBuilder report = new StringBuilder();
            report.append("═══ Advanced Analysis Report ═══\n\n");

            report.append("📊 Image Properties:\n");
            report.append("  • Resolution: ").append(width).append("x").append(height).append("\n");
            report.append("  • Aspect Ratio: ").append(String.format("%.2f", aspectRatio)).append("\n");
            report.append("  • Brightness: ").append(String.format("%.0f%%", brightness * 100)).append("\n");
            report.append("  • Quality Score: ").append(String.format("%.0f%%", qualityScore * 100)).append("\n\n");

            report.append("🎨 Dominant Colors:\n");
            for (String color : dominantColors) {
                report.append("  • ").append(color).append("\n");
            }
            report.append("\n");

            report.append("🏷️ Category: ").append(category).append("\n\n");

            report.append("🔍 Detected Objects:\n");
            labels.entrySet().stream()
                    .sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
                    .limit(7)
                    .forEach(entry -> {
                        int confidence = Math.round(entry.getValue() * 100);
                        report.append("  • ").append(entry.getKey())
                                .append(" (").append(confidence).append("%)\n");
                    });

            return report.toString();
        }
    }

    /**
     * Callback interface for analysis results
     */
    public interface AnalysisCallback {
        void onSuccess(AnalysisResult result);
        void onError(String error);
    }
}
