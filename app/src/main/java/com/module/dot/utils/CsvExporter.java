package com.module.dot.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;

import androidx.core.content.FileProvider;

import com.module.dot.model.InventoryItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class CsvExporter {

    private static final String PREFS_NAME = "inventory_prefs";
    private static final String KEY_EXPORT_DIR = "export_dir";

    /**
     * Get the export directory. Uses user-defined path if set, otherwise Downloads.
     */
    public static File getExportDir(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String customPath = prefs.getString(KEY_EXPORT_DIR, "");
        if (!customPath.isEmpty()) {
            File dir = new File(customPath);
            if (dir.exists() || dir.mkdirs()) {
                return dir;
            }
        }
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    /**
     * Save custom export directory path.
     */
    public static void setExportDir(Context context, String path) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_EXPORT_DIR, path).apply();
    }

    /**
     * Reset export directory to default (Downloads).
     */
    public static void resetExportDir(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_EXPORT_DIR).apply();
    }

    /**
     * Export inventory items to CSV.
     *
     * @return The exported file, or null on failure.
     */
    public static File export(ArrayList<InventoryItem> items, Context context) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format(new Date());
        String fileName = "Inventory_" + timestamp + ".csv";

        File dir = getExportDir(context);
        File file = new File(dir, fileName);

        try (FileWriter writer = new FileWriter(file)) {
            // BOM for Excel UTF-8 compatibility
            writer.write('﻿');
            // Header
            writer.write("条码,数量,扫描时间\n");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            for (InventoryItem item : items) {
                writer.write(String.format(Locale.getDefault(),
                        "%s,%d,%s\n",
                        item.getBarcode(),
                        item.getQuantity(),
                        sdf.format(new Date(item.getTimestamp()))
                ));
            }
            writer.flush();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Share file via Intent.
     */
    public static void shareFile(File file, Context context) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(context,
                        context.getPackageName() + ".fileprovider", file));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "分享盘点记录"));
    }
}
