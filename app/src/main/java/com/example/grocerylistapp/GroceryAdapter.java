package com.example.grocerylistapp; // Ensure this is correct

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Using Button as per your XML
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class GroceryAdapter extends RecyclerView.Adapter<GroceryAdapter.GroceryViewHolder> {

    public interface OnItemActionListener {
        void onEdit(GroceryItem item);
        void onDelete(GroceryItem item);
    }

    private List<GroceryItem> groceryItemsList;
    private OnItemActionListener actionListener;

    public GroceryAdapter(List<GroceryItem> initialItems, OnItemActionListener listener) {
        this.groceryItemsList = new ArrayList<>(); // Initialize to avoid null
        if (initialItems != null) {
            this.groceryItemsList.addAll(initialItems);
        }
        this.actionListener = listener;
    }

    public void setItems(List<GroceryItem> newItems) {
        this.groceryItemsList.clear();
        if (newItems != null) {
            this.groceryItemsList.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroceryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grocery, parent, false);
        return new GroceryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroceryViewHolder holder, int position) {
        if (groceryItemsList == null || position < 0 || position >= groceryItemsList.size()) {
            Log.e("GroceryAdapter", "Invalid position or list state: " + position);
            return;
        }
        GroceryItem currentItem = groceryItemsList.get(position);
        holder.bind(currentItem, actionListener);
    }

    @Override
    public int getItemCount() {
        return groceryItemsList == null ? 0 : groceryItemsList.size();
    }

    static class GroceryViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName;
        TextView tvItemQuantity;
        Button btnEdit;    // Changed to Button
        Button btnDelete;  // Changed to Button

        GroceryViewHolder(View itemView) {
            super(itemView);
            // These IDs MUST match what's in item_grocery.xml
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemQuantity = itemView.findViewById(R.id.tvItemQuantity);
            btnEdit = itemView.findViewById(R.id.btnEditItem);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
        }

        void bind(final GroceryItem item, final OnItemActionListener listener) {
            if (item == null) {
                Log.e("GroceryViewHolder", "Item is null in bind");
                tvItemName.setText("Error");
                tvItemQuantity.setText("");
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
                return;
            }

            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);

            tvItemName.setText(item.getName());
            tvItemQuantity.setText("Qty: " + item.getQuantity());

            if (listener != null) {
                btnEdit.setOnClickListener(v -> listener.onEdit(item));
                btnDelete.setOnClickListener(v -> listener.onDelete(item));
            } else {
                btnEdit.setOnClickListener(null);
                btnDelete.setOnClickListener(null);
            }
        }
    }
}