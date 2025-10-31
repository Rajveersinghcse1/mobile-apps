package com.example.imageanalysis;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager class to handle history records storage and retrieval
 */
public class HistoryManager {
    
    private static final String TAG = "HistoryManager";
    private static final String PREFS_NAME = "SFCHistory";
    private static final String KEY_HISTORY = "history_records";
    private static final int MAX_HISTORY_RECORDS = 100;

    private SharedPreferences prefs;
    private Gson gson;

    public HistoryManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Add a new history record
     */
    public boolean addHistory(HistoryItem item) {
        try {
            List<HistoryItem> historyList = getAllHistory();
            
            // Add new item at the beginning
            historyList.add(0, item);
            
            // Limit to max records
            if (historyList.size() > MAX_HISTORY_RECORDS) {
                historyList = historyList.subList(0, MAX_HISTORY_RECORDS);
            }
            
            saveHistoryList(historyList);
            Log.d(TAG, "History record added: " + item.getId());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding history: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get all history records
     */
    public List<HistoryItem> getAllHistory() {
        try {
            String json = prefs.getString(KEY_HISTORY, "[]");
            JsonArray jsonArray = gson.fromJson(json, JsonArray.class);
            List<HistoryItem> historyList = new ArrayList<>();
            
            for (JsonElement element : jsonArray) {
                try {
                    HistoryItem item = gson.fromJson(element, HistoryItem.class);
                    historyList.add(item);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing history item: " + e.getMessage());
                }
            }
            
            return historyList;
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading history: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Delete a specific history record
     */
    public boolean deleteHistory(String id) {
        try {
            List<HistoryItem> historyList = getAllHistory();
            boolean removed = historyList.removeIf(item -> item.getId().equals(id));
            
            if (removed) {
                saveHistoryList(historyList);
                Log.d(TAG, "History record deleted: " + id);
            }
            
            return removed;
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting history: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Clear all history
     */
    public boolean clearAllHistory() {
        try {
            prefs.edit().remove(KEY_HISTORY).apply();
            Log.d(TAG, "All history cleared");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing history: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get history count
     */
    public int getHistoryCount() {
        return getAllHistory().size();
    }

    /**
     * Search history by name or ID
     */
    public List<HistoryItem> searchHistory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllHistory();
        }
        
        List<HistoryItem> allHistory = getAllHistory();
        List<HistoryItem> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (HistoryItem item : allHistory) {
            if (item.getMatchedBaseName().toLowerCase().contains(lowerQuery) ||
                item.getMatchedName().toLowerCase().contains(lowerQuery) ||
                item.getMatchedId().toLowerCase().contains(lowerQuery) ||
                item.getMatchedDepartment().toLowerCase().contains(lowerQuery)) {
                filtered.add(item);
            }
        }
        
        return filtered;
    }

    /**
     * Get history by date range
     */
    public List<HistoryItem> getHistoryByDateRange(long startTime, long endTime) {
        List<HistoryItem> allHistory = getAllHistory();
        List<HistoryItem> filtered = new ArrayList<>();
        
        for (HistoryItem item : allHistory) {
            if (item.getTimestamp() >= startTime && item.getTimestamp() <= endTime) {
                filtered.add(item);
            }
        }
        
        return filtered;
    }

    /**
     * Get history with minimum similarity score
     */
    public List<HistoryItem> getHistoryBySimilarity(double minScore) {
        List<HistoryItem> allHistory = getAllHistory();
        List<HistoryItem> filtered = new ArrayList<>();
        
        for (HistoryItem item : allHistory) {
            if (item.getSimilarityScore() >= minScore) {
                filtered.add(item);
            }
        }
        
        return filtered;
    }

    /**
     * Save history list to SharedPreferences
     */
    private void saveHistoryList(List<HistoryItem> historyList) {
        try {
            String json = gson.toJson(historyList);
            prefs.edit().putString(KEY_HISTORY, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving history list: " + e.getMessage(), e);
        }
    }

    /**
     * Get statistics
     */
    public HistoryStats getStatistics() {
        List<HistoryItem> allHistory = getAllHistory();
        HistoryStats stats = new HistoryStats();
        
        stats.totalRecords = allHistory.size();
        
        if (!allHistory.isEmpty()) {
            double totalSimilarity = 0;
            double maxSimilarity = 0;
            double minSimilarity = 1.0;
            
            for (HistoryItem item : allHistory) {
                double score = item.getSimilarityScore();
                totalSimilarity += score;
                if (score > maxSimilarity) maxSimilarity = score;
                if (score < minSimilarity) minSimilarity = score;
            }
            
            stats.averageSimilarity = totalSimilarity / allHistory.size();
            stats.maxSimilarity = maxSimilarity;
            stats.minSimilarity = minSimilarity;
        }
        
        return stats;
    }

    /**
     * Statistics data class
     */
    public static class HistoryStats {
        public int totalRecords = 0;
        public double averageSimilarity = 0;
        public double maxSimilarity = 0;
        public double minSimilarity = 0;
    }
}
