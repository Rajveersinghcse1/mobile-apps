package com.example.imageanalysis;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StoreItemAdapter extends RecyclerView.Adapter<StoreItemAdapter.ViewHolder> {

    public static class StoreItem {
        public String baseName;
        public File imageFile;
        public File jsonFile;
        public JsonObject jsonData;
        public boolean hasImage;
        public boolean hasData;

        public StoreItem(String baseName) {
            this.baseName = baseName;
        }

        public boolean isComplete() {
            return hasImage && hasData;
        }
    }

    private List<StoreItem> items;
    private List<StoreItem> itemsFiltered;
    private OnItemClickListener listener;
    private OnItemActionListener actionListener;

    public interface OnItemClickListener {
        void onItemClick(StoreItem item);
    }

    public interface OnItemActionListener {
        void onEditClick(StoreItem item, int position);
        void onDeleteClick(StoreItem item, int position);
    }

    public StoreItemAdapter(List<StoreItem> items, OnItemClickListener listener) {
        this.items = items;
        this.itemsFiltered = new ArrayList<>(items);
        this.listener = listener;
    }

    public void setActionListener(OnItemActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_store, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StoreItem item = itemsFiltered.get(position);
        holder.bind(item, listener, actionListener, position);
    }

    @Override
    public int getItemCount() {
        return itemsFiltered.size();
    }

    public void filter(String query) {
        itemsFiltered.clear();
        if (query.isEmpty()) {
            itemsFiltered.addAll(items);
        } else {
            String lowerQuery = query.toLowerCase();
            for (StoreItem item : items) {
                if (item.baseName.toLowerCase().contains(lowerQuery)) {
                    itemsFiltered.add(item);
                } else if (item.jsonData != null && item.jsonData.has("name")) {
                    String name = item.jsonData.get("name").getAsString().toLowerCase();
                    if (name.contains(lowerQuery)) {
                        itemsFiltered.add(item);
                    }
                } else if (item.jsonData != null && item.jsonData.has("id")) {
                    String id = item.jsonData.get("id").getAsString().toLowerCase();
                    if (id.contains(lowerQuery)) {
                        itemsFiltered.add(item);
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateItems(List<StoreItem> newItems) {
        this.items = newItems;
        this.itemsFiltered = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvFileName;
        TextView tvName;
        TextView tvDetails;
        TextView tvStatus;
        Button btnEdit;
        Button btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvName = itemView.findViewById(R.id.tvName);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(StoreItem item, OnItemClickListener listener, OnItemActionListener actionListener, int position) {
            tvFileName.setText(item.baseName);

            // Load thumbnail
            if (item.hasImage && item.imageFile != null) {
                try {
                    Bitmap thumbnail = BitmapFactory.decodeFile(item.imageFile.getAbsolutePath());
                    if (thumbnail != null) {
                        ivThumbnail.setImageBitmap(thumbnail);
                    } else {
                        ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                } catch (Exception e) {
                    ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Show name and details from JSON
            if (item.hasData && item.jsonData != null) {
                if (item.jsonData.has("name")) {
                    tvName.setText(item.jsonData.get("name").getAsString());
                } else {
                    tvName.setText("No name");
                }

                StringBuilder details = new StringBuilder();
                if (item.jsonData.has("id")) {
                    details.append("ID: ").append(item.jsonData.get("id").getAsString());
                }
                if (item.jsonData.has("department")) {
                    if (details.length() > 0) details.append(" | ");
                    details.append(item.jsonData.get("department").getAsString());
                }
                if (item.jsonData.has("email")) {
                    if (details.length() > 0) details.append("\n");
                    details.append(item.jsonData.get("email").getAsString());
                }

                tvDetails.setText(details.length() > 0 ? details.toString() : "No additional details");
            } else {
                tvName.setText("No data");
                tvDetails.setText("JSON file missing");
            }

            // Status
            if (item.isComplete()) {
                tvStatus.setText("✓ Complete");
                tvStatus.setTextColor(0xFF4CAF50);
            } else if (item.hasImage && !item.hasData) {
                tvStatus.setText("⚠ Missing JSON");
                tvStatus.setTextColor(0xFFFF9800);
            } else if (!item.hasImage && item.hasData) {
                tvStatus.setText("⚠ Missing Image");
                tvStatus.setTextColor(0xFFFF9800);
            } else {
                tvStatus.setText("❌ Incomplete");
                tvStatus.setTextColor(0xFFF44336);
            }

            // Click listener for preview
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });

            // Edit button click listener
            btnEdit.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onEditClick(item, position);
                }
            });

            // Delete button click listener
            btnDelete.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDeleteClick(item, position);
                }
            });
        }
    }
}
