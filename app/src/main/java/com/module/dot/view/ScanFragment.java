package com.module.dot.view;

import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.module.dot.R;
import com.module.dot.data.local.InventoryDatabase;
import com.module.dot.model.InventoryItem;
import com.module.dot.utils.CsvExporter;
import com.module.dot.utils.LocalFormat;
import com.module.dot.view.adapters.InventoryAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class ScanFragment extends Fragment {

    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private TextView statsText;
    private Button modeButton, newButton;
    private FloatingActionButton scanFab, clearFab, exportFab;

    private InventoryDatabase db;
    private boolean autoMode = true;
    private ArrayList<InventoryItem> itemList = new ArrayList<>();

    private final ActivityResultLauncher<ScanOptions> scannerLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                String barcode = result.getContents();
                if (barcode != null && !barcode.isEmpty()) {
                    onBarcodeScanned(barcode);
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new InventoryDatabase(getContext());
        db.ensureTables();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        statsText = view.findViewById(R.id.statsText);
        modeButton = view.findViewById(R.id.modeButton);
        newButton = view.findViewById(R.id.newButton);
        scanFab = view.findViewById(R.id.scanFab);
        clearFab = view.findViewById(R.id.clearFab);
        exportFab = view.findViewById(R.id.exportFab);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InventoryAdapter(itemList, item -> showEditQuantityDialog(item));
        recyclerView.setAdapter(adapter);

        // Swipe to delete
        setupSwipeToDelete();

        newButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_gray));
        updateModeButtonText();

        modeButton.setOnClickListener(v -> {
            autoMode = !autoMode;
            updateModeButtonText();
            Toast.makeText(getContext(), autoMode ? "自动累加模式" : "手动输入模式", Toast.LENGTH_SHORT).show();
        });

        newButton.setOnClickListener(v -> showNewSessionDialog());
        scanFab.setOnClickListener(v -> startScan());
        clearFab.setOnClickListener(v -> showClearDialog());
        exportFab.setOnClickListener(v -> exportCsv());
        exportFab.setOnLongClickListener(v -> {
            showExportPathDialog();
            return true;
        });

        loadData();
        return view;
    }

    private void setupSwipeToDelete() {
        Drawable deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_remove_24);
        ColorDrawable redBg = new ColorDrawable(Color.parseColor("#E53935"));

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (pos >= 0 && pos < itemList.size()) {
                    InventoryItem item = itemList.get(pos);
                    db.deleteItem(item.getBarcode());
                    itemList.remove(pos);
                    adapter.notifyItemRemoved(pos);
                    updateStats();
                    Toast.makeText(getContext(), "已删除: " + item.getBarcode(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = vh.itemView;
                if (dX < 0) { // Swiping left
                    redBg.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                    redBg.draw(c);
                    if (deleteIcon != null) {
                        int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                        int iconRight = itemView.getRight() - iconMargin;
                        int iconLeft = iconRight - deleteIcon.getIntrinsicWidth();
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        deleteIcon.setTint(Color.WHITE);
                        deleteIcon.draw(c);
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
            }
        });
        helper.attachToRecyclerView(recyclerView);
    }

    private void updateModeButtonText() {
        modeButton.setText(autoMode ? "自动 +1" : "手动输入");
        modeButton.setBackgroundColor(ContextCompat.getColor(requireContext(),
                autoMode ? R.color.green : R.color.orange));
    }

    private void startScan() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("音量键开关闪光灯");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        scannerLauncher.launch(options);
    }

    public void onBarcodeScanned(String barcode) {
        if (barcode == null || barcode.isEmpty()) return;

        if (autoMode) {
            db.upsertItem(barcode, 1);
            loadData();
        } else {
            showQuantityInputDialog(barcode);
        }
    }

    // === 新建盘点 ===
    private void showNewSessionDialog() {
        if (itemList.isEmpty()) {
            Toast.makeText(getContext(), "当前已是空盘点", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("新建盘点")
                .setMessage("当前有 " + itemList.size() + " 条记录。\n\n保存为历史记录并清空？")
                .setPositiveButton("保存并新建", (dialog, which) -> {
                    int[] stats = db.getCurrentStats();
                    db.finishSession(LocalFormat.getCurrentDateTimeString(), stats[0], stats[1]);
                    db.clearCurrentSession();
                    loadData();
                    Toast.makeText(getContext(), "已保存历史，开始新盘点", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("不保存直接清空", (dialog, which) -> {
                    db.clearCurrentSession();
                    loadData();
                    Toast.makeText(getContext(), "已清空，开始新盘点", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // === 手动输入数量 ===
    private void showQuantityInputDialog(String barcode) {
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("数量");
        input.setText("1");

        new AlertDialog.Builder(requireContext())
                .setTitle("输入数量")
                .setMessage("条码: " + barcode)
                .setView(input)
                .setPositiveButton("确认", (dialog, which) -> {
                    String text = input.getText().toString();
                    int qty = text.isEmpty() ? 1 : Integer.parseInt(text);
                    if (qty > 0) {
                        db.upsertItem(barcode, qty);
                        loadData();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // === 点击编辑数量 ===
    private void showEditQuantityDialog(InventoryItem item) {
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(item.getQuantity()));

        new AlertDialog.Builder(requireContext())
                .setTitle("编辑")
                .setMessage("条码: " + item.getBarcode())
                .setView(input)
                .setPositiveButton("确认", (dialog, which) -> {
                    String text = input.getText().toString();
                    int qty = text.isEmpty() ? 0 : Integer.parseInt(text);
                    if (qty >= 0) {
                        db.upsertItem(item.getBarcode(), qty - item.getQuantity());
                        loadData();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // === 清空（二次验证） ===
    private void showClearDialog() {
        if (itemList.isEmpty()) {
            Toast.makeText(getContext(), "没有数据可清空", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("清空当前盘点")
                .setMessage("确定要清空全部 " + itemList.size() + " 条记录吗？")
                .setPositiveButton("确定", (d1, w1) -> {
                    // 第二次确认
                    new AlertDialog.Builder(requireContext())
                            .setTitle("⚠ 再次确认")
                            .setMessage("此操作不可恢复！\n\n确认清空全部数据？")
                            .setPositiveButton("确认清空", (d2, w2) -> {
                                db.clearCurrentSession();
                                loadData();
                                Toast.makeText(getContext(), "已清空", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // === 导出路径设置 ===
    private void showExportPathDialog() {
        String currentPath = CsvExporter.getExportDir(requireContext()).getAbsolutePath();

        final EditText input = new EditText(getContext());
        input.setText(currentPath);
        input.setHint("/storage/emulated/0/Download");
        input.setSingleLine();

        new AlertDialog.Builder(requireContext())
                .setTitle("导出路径")
                .setView(input)
                .setPositiveButton("保存", (dialog, which) -> {
                    String path = input.getText().toString().trim();
                    if (!path.isEmpty()) {
                        CsvExporter.setExportDir(requireContext(), path);
                        Toast.makeText(getContext(), "已设置: " + path, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("恢复默认", (dialog, which) -> {
                    CsvExporter.resetExportDir(requireContext());
                    Toast.makeText(getContext(), "已恢复默认路径 (Downloads)", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // === CSV导出 ===
    private void exportCsv() {
        if (itemList.isEmpty()) {
            Toast.makeText(getContext(), "没有数据可导出", Toast.LENGTH_SHORT).show();
            return;
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 100);
                return;
            }
        }

        File file = CsvExporter.export(itemList, requireContext());
        if (file != null) {
            int[] stats = db.getCurrentStats();
            String dateTime = LocalFormat.getCurrentDateTimeString();
            db.finishSession(dateTime, stats[0], stats[1]);

            Toast.makeText(getContext(),
                    "导出成功: " + file.getName() + "\n" + file.getParent(),
                    Toast.LENGTH_LONG).show();

            new AlertDialog.Builder(requireContext())
                    .setTitle("导出完成")
                    .setMessage("文件: " + file.getName() + "\n是否分享？")
                    .setPositiveButton("分享", (d, w) -> CsvExporter.shareFile(file, requireContext()))
                    .setNegativeButton("完成", (d, w) -> {
                        db.clearCurrentSession();
                        loadData();
                    })
                    .show();
        } else {
            Toast.makeText(getContext(), "导出失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadData() {
        itemList.clear();
        itemList.addAll(db.getCurrentItems());
        adapter.notifyDataSetChanged();
        updateStats();
    }

    private void updateStats() {
        int[] stats = db.getCurrentStats();
        statsText.setText(String.format(Locale.getDefault(),
                "共 %d 种商品  |  总计 %d 件", stats[0], stats[1]));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (db != null) db.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}
