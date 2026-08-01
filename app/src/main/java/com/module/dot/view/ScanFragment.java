package com.module.dot.view;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
    private Button newButton, modeBtn, confirmBtn;
    private FloatingActionButton clearFab, exportFab;
    private EditText barcodeInput;
    private ImageButton scanBtn;

    private InventoryDatabase db;
    private boolean autoMode = true;
    private ArrayList<InventoryItem> itemList = new ArrayList<>();

    private final ActivityResultLauncher<ScanOptions> scannerLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                String barcode = result.getContents();
                if (barcode != null && !barcode.isEmpty()) {
                    onBarcodeScanned(barcode);
                    // Auto mode: continuous scanning; manual mode: return to input
                    if (autoMode) {
                        startScan();
                    }
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
        newButton = view.findViewById(R.id.newButton);
        clearFab = view.findViewById(R.id.clearFab);
        exportFab = view.findViewById(R.id.exportFab);
        barcodeInput = view.findViewById(R.id.barcodeInput);
        scanBtn = view.findViewById(R.id.scanBtn);
        modeBtn = view.findViewById(R.id.modeBtn);
        confirmBtn = view.findViewById(R.id.confirmBtn);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InventoryAdapter(itemList, item -> showEditQuantityDialog(item));
        recyclerView.setAdapter(adapter);

        // Swipe to delete
        setupSwipeToDelete();

        updateModeButtonUI();

        // Mode toggle: auto +1 ↔ manual quantity dialog
        modeBtn.setOnClickListener(v -> {
            autoMode = !autoMode;
            updateModeButtonUI();
            Toast.makeText(getContext(), autoMode ? "自动累计模式" : "手动输入模式", Toast.LENGTH_SHORT).show();
        });

        // Camera scan button
        scanBtn.setOnClickListener(v -> startScan());

        // Confirm button: submit typed barcode
        confirmBtn.setOnClickListener(v -> submitBarcode());

        // Enter key submits barcode
        barcodeInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                submitBarcode();
                return true;
            }
            return false;
        });

        newButton.setOnClickListener(v -> showNewSessionDialog());
        clearFab.setOnClickListener(v -> showClearDialog());
        exportFab.setOnClickListener(v -> exportCsv());
        exportFab.setOnLongClickListener(v -> {
            showExportPathDialog();
            return true;
        });

        loadData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
        // Auto-focus input and show keyboard when returning to this fragment
        barcodeInput.requestFocus();
        showKeyboard(barcodeInput);
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

    private void updateModeButtonUI() {
        modeBtn.setText(autoMode ? "自动累计" : "手动输入");
        modeBtn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(),
                autoMode ? R.color.green : R.color.orange));
    }

    // Submit typed barcode from EditText
    private void submitBarcode() {
        String barcode = barcodeInput.getText().toString().trim();
        if (!barcode.isEmpty()) {
            onBarcodeScanned(barcode);
            barcodeInput.setText("");
        }
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

        // Echo barcode in input field so user sees what was scanned
        barcodeInput.setText(barcode);
        barcodeInput.selectAll();

        int existingQty = db.getItemQuantity(barcode);

        if (autoMode) {
            db.upsertItem(barcode, 1);
            loadData();
            if (existingQty > 0) {
                Toast.makeText(getContext(),
                        barcode + " → 累计 " + (existingQty + 1) + " 件",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            showQuantityInputDialog(barcode, existingQty);
        }
    }

    // === 新建盘点 ===
    private void showNewSessionDialog() {
        if (itemList.isEmpty()) {
            Toast.makeText(getContext(), "当前已是空盘点", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 16);

        TextView label = new TextView(getContext());
        label.setText("当前有 " + itemList.size() + " 条记录");
        label.setTextSize(14);
        label.setTextColor(Color.DKGRAY);
        layout.addView(label);

        final EditText nameInput = new EditText(getContext());
        nameInput.setHint("盘点名称（可选）");
        nameInput.setSingleLine();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 16;
        nameInput.setLayoutParams(params);
        layout.addView(nameInput);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("新建盘点")
                .setView(layout)
                .setPositiveButton("保存并新建", (d, which) -> {
                    String name = nameInput.getText().toString().trim();
                    int[] stats = db.getCurrentStats();
                    db.finishSession(name, LocalFormat.getCurrentDateTimeString(), stats[0], stats[1]);
                    db.clearCurrentSession();
                    loadData();
                    Toast.makeText(getContext(), "已保存" + (name.isEmpty() ? "" : "「" + name + "」") + "，开始新盘点", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("不保存直接清空", (d, which) -> {
                    db.clearCurrentSession();
                    loadData();
                    Toast.makeText(getContext(), "已清空，开始新盘点", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();
        showKeyboard(nameInput);
    }

    // === 手动输入数量 ===
    private void showQuantityInputDialog(String barcode, int currentQty) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 16);

        TextView label = new TextView(getContext());
        label.setText("条码: " + barcode);
        label.setTextSize(14);
        label.setTextColor(Color.DKGRAY);
        layout.addView(label);

        if (currentQty > 0) {
            TextView hint = new TextView(getContext());
            hint.setText("当前已盘点: " + currentQty + " 件");
            hint.setTextSize(13);
            hint.setTextColor(Color.parseColor("#E6A23C"));
            hint.setPadding(0, 4, 0, 0);
            layout.addView(hint);
        }

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("数量");
        input.setText(String.valueOf(currentQty > 0 ? currentQty : 1));
        input.selectAll();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 16;
        input.setLayoutParams(params);
        layout.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("输入数量")
                .setView(layout)
                .setPositiveButton("确认", (d, which) -> {
                    String text = input.getText().toString();
                    int qty = text.isEmpty() ? 1 : Integer.parseInt(text);
                    if (qty > 0) {
                        db.upsertItem(barcode, qty);
                        loadData();
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();
        showKeyboard(input);
    }

    // === 点击编辑条码和数量 ===
    private void showEditQuantityDialog(InventoryItem item) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 16);

        final EditText barcodeEdit = new EditText(getContext());
        barcodeEdit.setText(item.getBarcode());
        barcodeEdit.setSingleLine();
        LinearLayout.LayoutParams bcParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        barcodeEdit.setLayoutParams(bcParams);
        layout.addView(barcodeEdit);

        final EditText qtyEdit = new EditText(getContext());
        qtyEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        qtyEdit.setText(String.valueOf(item.getQuantity()));
        qtyEdit.selectAll();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 16;
        qtyEdit.setLayoutParams(params);
        layout.addView(qtyEdit);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("编辑条码与数量")
                .setView(layout)
                .setPositiveButton("确认", (d, which) -> {
                    String newBarcode = barcodeEdit.getText().toString().trim();
                    String qtyText = qtyEdit.getText().toString();
                    int newQty = qtyText.isEmpty() ? 0 : Integer.parseInt(qtyText);
                    if (newBarcode.isEmpty()) return;

                    // If barcode changed, delete old entry and add new one
                    if (!newBarcode.equals(item.getBarcode())) {
                        db.deleteItem(item.getBarcode());
                        if (newQty > 0) {
                            db.upsertItem(newBarcode, newQty);
                        }
                    } else if (newQty >= 0) {
                        db.upsertItem(item.getBarcode(), newQty - item.getQuantity());
                    }
                    loadData();
                })
                .setNegativeButton("取消", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();
        showKeyboard(qtyEdit);
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

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 16);

        TextView label = new TextView(getContext());
        label.setText("当前路径:");
        label.setTextSize(14);
        label.setTextColor(Color.DKGRAY);
        layout.addView(label);

        final EditText input = new EditText(getContext());
        input.setText(currentPath);
        input.setHint("/storage/emulated/0/Download");
        input.setSingleLine();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 16;
        input.setLayoutParams(params);
        layout.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("导出路径")
                .setView(layout)
                .setPositiveButton("保存", (d, which) -> {
                    String path = input.getText().toString().trim();
                    if (!path.isEmpty()) {
                        CsvExporter.setExportDir(requireContext(), path);
                        Toast.makeText(getContext(), "已设置: " + path, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("恢复默认", (d, which) -> {
                    CsvExporter.resetExportDir(requireContext());
                    Toast.makeText(getContext(), "已恢复默认路径 (Downloads)", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();
        showKeyboard(input);
    }

    // === 键盘控制 ===
    private void showKeyboard(EditText editText) {
        editText.requestFocus();
        editText.post(() -> {
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
            }
        });
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
            db.finishSession("", dateTime, stats[0], stats[1]);

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
}
