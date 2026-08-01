package com.module.dot.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

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

        HistoryAdapter adapter = new HistoryAdapter(sessionList, new HistoryAdapter.OnSessionActionListener() {
            @Override
            public void onExport(InventorySession session) {
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
            }

            @Override
            public void onResume(InventorySession session) {
                int[] currentStats = db.getCurrentStats();
                String msg = currentStats[0] > 0
                        ? "当前盘点有 " + currentStats[0] + " 种 " + currentStats[1] + " 件商品。\n\n继续历史盘点将清空当前记录，确定？"
                        : "确定要恢复此历史盘点？";

                new AlertDialog.Builder(requireContext())
                        .setTitle("继续盘点")
                        .setMessage(msg)
                        .setPositiveButton("确定", (d, w) -> {
                            String sessionName = session.getName();
                            db.resumeSession(session.getId());
                            loadData();
                            Toast.makeText(getContext(),
                                    "已恢复" + (sessionName.isEmpty() ? "" : "「" + sessionName + "」"),
                                    Toast.LENGTH_SHORT).show();
                            ViewPager2 pager = getActivity().findViewById(R.id.viewPager);
                            if (pager != null) pager.setCurrentItem(0, true);
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }

            @Override
            public void onDelete(InventorySession session) {
                String title = session.getName().isEmpty() ? session.getDateTime() : session.getName();
                new AlertDialog.Builder(requireContext())
                        .setTitle("删除历史记录")
                        .setMessage("确定要删除「" + title + "」吗？\n\n" +
                                session.getItemCount() + " 种商品 · " + session.getTotalQty() + " 件")
                        .setPositiveButton("删除", (d1, w1) -> {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("⚠ 再次确认")
                                    .setMessage("此操作不可恢复！\n\n确认删除「" + title + "」？")
                                    .setPositiveButton("确认删除", (d2, w2) -> {
                                        db.deleteSession(session.getId());
                                        loadData();
                                        Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
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
