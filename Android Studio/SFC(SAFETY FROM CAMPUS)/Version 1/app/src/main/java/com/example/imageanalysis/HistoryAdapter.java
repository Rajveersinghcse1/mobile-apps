package com.example.imageanalysis;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> items;
    private List<HistoryItem> itemsFiltered;
    private OnHistoryItemListener listener;

    public interface OnHistoryItemListener {
        void onItemClick(HistoryItem item);
        void onDeleteClick(HistoryItem item);
    }

    public HistoryAdapter(List<HistoryItem> items, OnHistoryItemListener listener) {
        this.items = items;
        this.itemsFiltered = new ArrayList<>(items);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = itemsFiltered.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return itemsFiltered.size();
    }

    public void filter(String query) {
        itemsFiltered.clear();
        if (query == null || query.trim().isEmpty()) {
            itemsFiltered.addAll(items);
        } else {
            String lowerQuery = query.toLowerCase();
            for (HistoryItem item : items) {
                if (item.getMatchedBaseName().toLowerCase().contains(lowerQuery) ||
                    item.getMatchedName().toLowerCase().contains(lowerQuery) ||
                    item.getMatchedId().toLowerCase().contains(lowerQuery) ||
                    item.getMatchedDepartment().toLowerCase().contains(lowerQuery)) {
                    itemsFiltered.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateItems(List<HistoryItem> newItems) {
        this.items = newItems;
        this.itemsFiltered = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCapturedThumbnail;
        ImageView ivMatchedThumbnail;
        ImageView btnDelete;
        TextView tvMatchName;
        TextView tvMatchDetails;
        TextView tvMatchTime;
        TextView tvSimilarity;

        ViewHolder(View itemView) {
            super(itemView);
            ivCapturedThumbnail = itemView.findViewById(R.id.ivCapturedThumbnail);
            ivMatchedThumbnail = itemView.findViewById(R.id.ivMatchedThumbnail);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            tvMatchName = itemView.findViewById(R.id.tvMatchName);
            tvMatchDetails = itemView.findViewById(R.id.tvMatchDetails);
            tvMatchTime = itemView.findViewById(R.id.tvMatchTime);
            tvSimilarity = itemView.findViewById(R.id.tvSimilarity);
        }

        void bind(HistoryItem item, OnHistoryItemListener listener) {
            // Load captured image thumbnail
            if (item.getCapturedImagePath() != null) {
                File capturedFile = new File(item.getCapturedImagePath());
                if (capturedFile.exists()) {
                    try {
                        Bitmap capturedBitmap = BitmapFactory.decodeFile(capturedFile.getAbsolutePath());
                        if (capturedBitmap != null) {
                            ivCapturedThumbnail.setImageBitmap(capturedBitmap);
                        } else {
                            ivCapturedThumbnail.setImageResource(android.R.drawable.ic_menu_camera);
                        }
                    } catch (Exception e) {
                        ivCapturedThumbnail.setImageResource(android.R.drawable.ic_menu_camera);
                    }
                } else {
                    ivCapturedThumbnail.setImageResource(android.R.drawable.ic_menu_camera);
                }
            }

            // Load matched image thumbnail
            if (item.getMatchedImagePath() != null) {
                File matchedFile = new File(item.getMatchedImagePath());
                if (matchedFile.exists()) {
                    try {
                        Bitmap matchedBitmap = BitmapFactory.decodeFile(matchedFile.getAbsolutePath());
                        if (matchedBitmap != null) {
                            ivMatchedThumbnail.setImageBitmap(matchedBitmap);
                        } else {
                            ivMatchedThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
                        }
                    } catch (Exception e) {
                        ivMatchedThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                } else {
                    ivMatchedThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }

            // Set match name
            tvMatchName.setText(item.getMatchedName());

            // Set details
            String details = "";
            if (!item.getMatchedId().isEmpty()) {
                details = "ID: " + item.getMatchedId();
            }
            if (!item.getMatchedDepartment().isEmpty()) {
                if (!details.isEmpty()) details += " | ";
                details += item.getMatchedDepartment();
            }
            if (details.isEmpty()) {
                details = item.getMatchedBaseName();
            }
            tvMatchDetails.setText(details);

            // Set time
            tvMatchTime.setText(item.getFormattedDate());

            // Set similarity
            tvSimilarity.setText("✓ " + item.getFormattedSimilarity());

            // Click listener for item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });

            // Click listener for delete button
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(item);
                }
            });
        }
    }
}
