package com.example.imageanalysis;

import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Data model representing a history record of an image match
 */
public class HistoryItem {
    
    private String id;
    private String capturedImagePath;
    private String matchedImagePath;
    private String matchedBaseName;
    private JsonObject matchedData;
    private double similarityScore;
    private long timestamp;
    private String matchStatus;

    public HistoryItem() {
        this.id = generateId();
        this.timestamp = System.currentTimeMillis();
        this.matchStatus = "success";
    }

    public HistoryItem(String capturedImagePath, String matchedImagePath, 
                       String matchedBaseName, JsonObject matchedData, 
                       double similarityScore) {
        this();
        this.capturedImagePath = capturedImagePath;
        this.matchedImagePath = matchedImagePath;
        this.matchedBaseName = matchedBaseName;
        this.matchedData = matchedData;
        this.similarityScore = similarityScore;
    }

    private String generateId() {
        return "HIST_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getCapturedImagePath() {
        return capturedImagePath;
    }

    public String getMatchedImagePath() {
        return matchedImagePath;
    }

    public String getMatchedBaseName() {
        return matchedBaseName;
    }

    public JsonObject getMatchedData() {
        return matchedData;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setCapturedImagePath(String capturedImagePath) {
        this.capturedImagePath = capturedImagePath;
    }

    public void setMatchedImagePath(String matchedImagePath) {
        this.matchedImagePath = matchedImagePath;
    }

    public void setMatchedBaseName(String matchedBaseName) {
        this.matchedBaseName = matchedBaseName;
    }

    public void setMatchedData(JsonObject matchedData) {
        this.matchedData = matchedData;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    // Helper methods
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getFormattedSimilarity() {
        return String.format(Locale.getDefault(), "%.1f%%", similarityScore * 100);
    }

    public String getMatchedName() {
        if (matchedData != null && matchedData.has("name")) {
            return matchedData.get("name").getAsString();
        }
        return matchedBaseName;
    }

    public String getMatchedId() {
        if (matchedData != null && matchedData.has("id")) {
            return matchedData.get("id").getAsString();
        }
        return "";
    }

    public String getMatchedDepartment() {
        if (matchedData != null && matchedData.has("department")) {
            return matchedData.get("department").getAsString();
        }
        return "";
    }

    @Override
    public String toString() {
        return "HistoryItem{" +
                "id='" + id + '\'' +
                ", matchedBaseName='" + matchedBaseName + '\'' +
                ", similarityScore=" + similarityScore +
                ", timestamp=" + getFormattedDate() +
                '}';
    }
}
