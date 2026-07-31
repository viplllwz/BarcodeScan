package com.module.dot.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.module.dot.R;
import com.module.dot.data.local.InventoryDatabase;
import com.module.dot.model.InventoryItem;
import com.module.dot.model.InventorySession;
import com.module.dot.utils.CsvExporter;
import com.module.dot.view.adapters.HistoryAdapter;

import java.io.File;
import java.util.ArrayList;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private InventoryDatabase db;
    private ArrayList<InventorySession> sessionList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new InventoryDatabase(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        recyclerView = view.findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        HistoryAdapter adapter = new HistoryAdapter(sessionList, session -> {
            // Re-export this session
            ArrayList<InventoryItem> items = db.getSessionItems(session.getId());
            if (items.isEmpty()) {
                Toast.makeText(getContext(), "该会话没有数据", Toast.LENGTH_SHORT).show();
                return;
            }
            File file = CsvExporter.export(items, requireContext());
            if (file != null) {
                Toast.makeText(getContext(), "导出成功: " + file.getName(),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "导出失败", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        sessionList.clear();
        sessionList.addAll(db.getSessions());
        if (recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (db != null) db.close();
    }
}
