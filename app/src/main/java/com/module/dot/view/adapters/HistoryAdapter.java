package com.module.dot.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.module.dot.R;
import com.module.dot.model.InventorySession;

import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<InventorySession> sessions;
    private final OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onSessionClick(InventorySession session);
    }

    public HistoryAdapter(List<InventorySession> sessions, OnSessionClickListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventorySession session = sessions.get(position);

        holder.dateTimeText.setText(session.getDateTime());
        holder.statsText.setText(String.format(Locale.getDefault(),
                "%d 种商品 · %d 件", session.getItemCount(), session.getTotalQty()));
        holder.exportedText.setText(session.isExported() ? "已导出" : "未导出");

        holder.itemView.setOnClickListener(v -> listener.onSessionClick(session));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTimeText;
        TextView statsText;
        TextView exportedText;

        ViewHolder(View itemView) {
            super(itemView);
            dateTimeText = itemView.findViewById(R.id.sessionDateTime);
            statsText = itemView.findViewById(R.id.sessionStats);
            exportedText = itemView.findViewById(R.id.sessionExportStatus);
        }
    }
}
