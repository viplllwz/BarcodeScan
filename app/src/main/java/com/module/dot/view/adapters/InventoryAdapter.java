package com.module.dot.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.module.dot.R;
import com.module.dot.model.InventoryItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private final List<InventoryItem> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(InventoryItem item);
    }

    public InventoryAdapter(List<InventoryItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryItem item = items.get(position);

        holder.barcodeText.setText(item.getBarcode());
        holder.quantityText.setText(String.format(Locale.getDefault(), "× %d", item.getQuantity()));

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        holder.timeText.setText(sdf.format(new Date(item.getTimestamp())));

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView barcodeText;
        TextView quantityText;
        TextView timeText;

        ViewHolder(View itemView) {
            super(itemView);
            barcodeText = itemView.findViewById(R.id.itemBarcode);
            quantityText = itemView.findViewById(R.id.itemQuantity);
            timeText = itemView.findViewById(R.id.itemTime);
        }
    }
}
