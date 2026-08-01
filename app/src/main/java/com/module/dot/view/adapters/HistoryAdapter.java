package com.module.dot.view.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.module.dot.R;
import com.module.dot.model.InventorySession;

import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<InventorySession> sessions;
    private final OnSessionActionListener listener;

    public interface OnSessionActionListener {
        void onExport(InventorySession session);
        void onResume(InventorySession session);
        void onDelete(InventorySession session);
    }

    public HistoryAdapter(List<InventorySession> sessions, OnSessionActionListener listener) {
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

        // Show name if set, otherwise show date only
        if (!TextUtils.isEmpty(session.getName())) {
            holder.nameText.setText(session.getName());
            holder.nameText.setVisibility(View.VISIBLE);
        } else {
            holder.nameText.setVisibility(View.GONE);
        }
        holder.dateTimeText.setText(session.getDateTime());
        holder.statsText.setText(String.format(Locale.getDefault(),
                "%d 种商品 · %d 件", session.getItemCount(), session.getTotalQty()));
        holder.exportedText.setText(session.isExported() ? "已导出" : "未导出");

        holder.exportBtn.setOnClickListener(v -> listener.onExport(session));
        holder.resumeBtn.setOnClickListener(v -> listener.onResume(session));
        holder.deleteBtn.setOnClickListener(v -> listener.onDelete(session));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView dateTimeText;
        TextView statsText;
        TextView exportedText;
        Button exportBtn;
        Button resumeBtn;
        Button deleteBtn;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.sessionName);
            dateTimeText = itemView.findViewById(R.id.sessionDateTime);
            statsText = itemView.findViewById(R.id.sessionStats);
            exportedText = itemView.findViewById(R.id.sessionExportStatus);
            exportBtn = itemView.findViewById(R.id.exportBtn);
            resumeBtn = itemView.findViewById(R.id.resumeBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }
    }
}
