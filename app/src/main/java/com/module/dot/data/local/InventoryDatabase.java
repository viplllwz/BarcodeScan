package com.module.dot.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.module.dot.model.InventoryItem;
import com.module.dot.model.InventorySession;

import java.util.ArrayList;

public class InventoryDatabase extends MyDatabaseManager {

    private static final String TABLE_ITEMS = "inventory_items";
    private static final String TABLE_SESSIONS = "inventory_sessions";

    // Columns for inventory_items
    private static final String COL_ID = "_id";
    private static final String COL_SESSION_ID = "session_id";
    private static final String COL_BARCODE = "barcode";
    private static final String COL_QUANTITY = "quantity";
    private static final String COL_TIMESTAMP = "timestamp";

    // Columns for inventory_sessions
    private static final String COL_S_NAME = "name";
    private static final String COL_S_DATE_TIME = "date_time";
    private static final String COL_S_ITEM_COUNT = "item_count";
    private static final String COL_S_TOTAL_QTY = "total_qty";
    private static final String COL_S_EXPORTED = "exported";

    // Current active session ID (0 = no active session)
    private static final long ACTIVE_SESSION_ID = 1;

    public InventoryDatabase(@Nullable Context context) {
        super(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        createTables(db);
    }

    private void createTables(SQLiteDatabase db) {
        String createItems = "CREATE TABLE IF NOT EXISTS " + TABLE_ITEMS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SESSION_ID + " INTEGER NOT NULL, " +
                COL_BARCODE + " TEXT NOT NULL, " +
                COL_QUANTITY + " INTEGER NOT NULL DEFAULT 1, " +
                COL_TIMESTAMP + " INTEGER NOT NULL)";
        db.execSQL(createItems);

        String createSessions = "CREATE TABLE IF NOT EXISTS " + TABLE_SESSIONS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_S_NAME + " TEXT NOT NULL DEFAULT '', " +
                COL_S_DATE_TIME + " TEXT NOT NULL, " +
                COL_S_ITEM_COUNT + " INTEGER NOT NULL DEFAULT 0, " +
                COL_S_TOTAL_QTY + " INTEGER NOT NULL DEFAULT 0, " +
                COL_S_EXPORTED + " INTEGER NOT NULL DEFAULT 0)";
        db.execSQL(createSessions);

        // Migration: add name column if upgrading from older schema
        try {
            db.execSQL("ALTER TABLE " + TABLE_SESSIONS + " ADD COLUMN " + COL_S_NAME + " TEXT NOT NULL DEFAULT ''");
        } catch (Exception ignored) {
            // Column already exists
        }

        // Ensure active session row exists
        Cursor cursor = db.rawQuery("SELECT " + COL_ID + " FROM " + TABLE_SESSIONS +
                " WHERE " + COL_ID + " = " + ACTIVE_SESSION_ID, null);
        if (!cursor.moveToFirst()) {
            ContentValues cv = new ContentValues();
            cv.put(COL_ID, ACTIVE_SESSION_ID);
            cv.put(COL_S_NAME, "");
            cv.put(COL_S_DATE_TIME, "");
            cv.put(COL_S_ITEM_COUNT, 0);
            cv.put(COL_S_TOTAL_QTY, 0);
            cv.put(COL_S_EXPORTED, 0);
            db.insert(TABLE_SESSIONS, null, cv);
        }
        cursor.close();
    }

    /**
     * Insert or update item in current session. If barcode exists, increment quantity.
     */
    public void upsertItem(String barcode, int addQty) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            Cursor cursor = db.rawQuery("SELECT " + COL_ID + ", " + COL_QUANTITY +
                    " FROM " + TABLE_ITEMS +
                    " WHERE " + COL_SESSION_ID + " = " + ACTIVE_SESSION_ID +
                    " AND " + COL_BARCODE + " = ?", new String[]{barcode});

            if (cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                int oldQty = cursor.getInt(1);
                ContentValues cv = new ContentValues();
                cv.put(COL_QUANTITY, oldQty + addQty);
                cv.put(COL_TIMESTAMP, System.currentTimeMillis());
                db.update(TABLE_ITEMS, cv, COL_ID + " = ?", new String[]{String.valueOf(id)});
            } else {
                ContentValues cv = new ContentValues();
                cv.put(COL_SESSION_ID, ACTIVE_SESSION_ID);
                cv.put(COL_BARCODE, barcode);
                cv.put(COL_QUANTITY, addQty);
                cv.put(COL_TIMESTAMP, System.currentTimeMillis());
                db.insert(TABLE_ITEMS, null, cv);
            }
            cursor.close();
        }
    }

    /**
     * Get current quantity of a barcode in active session, or 0 if not present.
     */
    public int getItemQuantity(String barcode) {
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            Cursor cursor = db.rawQuery("SELECT " + COL_QUANTITY +
                    " FROM " + TABLE_ITEMS +
                    " WHERE " + COL_SESSION_ID + " = " + ACTIVE_SESSION_ID +
                    " AND " + COL_BARCODE + " = ?", new String[]{barcode});
            int qty = 0;
            if (cursor.moveToFirst()) {
                qty = cursor.getInt(0);
            }
            cursor.close();
            return qty;
        }
    }

    /**
     * Get all items in current session, ordered by most recent first.
     */
    public ArrayList<InventoryItem> getCurrentItems() {
        ArrayList<InventoryItem> list = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            Cursor cursor = db.rawQuery("SELECT " + COL_BARCODE + ", " + COL_QUANTITY +
                    ", " + COL_TIMESTAMP + " FROM " + TABLE_ITEMS +
                    " WHERE " + COL_SESSION_ID + " = " + ACTIVE_SESSION_ID +
                    " ORDER BY " + COL_TIMESTAMP + " DESC", null);
            while (cursor.moveToNext()) {
                list.add(new InventoryItem(
                        cursor.getString(0),
                        cursor.getInt(1),
                        cursor.getLong(2)
                ));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Get count of distinct items and total quantity in current session.
     */
    public int[] getCurrentStats() {
        int[] stats = new int[]{0, 0}; // [itemCount, totalQty]
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            Cursor cursor = db.rawQuery("SELECT COUNT(*), SUM(" + COL_QUANTITY + ") FROM " +
                    TABLE_ITEMS + " WHERE " + COL_SESSION_ID + " = " + ACTIVE_SESSION_ID, null);
            if (cursor.moveToFirst()) {
                stats[0] = cursor.getInt(0);
                stats[1] = cursor.getInt(1);
            }
            cursor.close();
        }
        return stats;
    }

    /**
     * Delete a single item by barcode from current session.
     */
    public void deleteItem(String barcode) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            db.delete(TABLE_ITEMS,
                    COL_SESSION_ID + " = ? AND " + COL_BARCODE + " = ?",
                    new String[]{String.valueOf(ACTIVE_SESSION_ID), barcode});
        }
    }

    /**
     * Clear all items from current session without creating a history record.
     */
    public void clearCurrentSession() {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            db.delete(TABLE_ITEMS, COL_SESSION_ID + " = ?",
                    new String[]{String.valueOf(ACTIVE_SESSION_ID)});
        }
    }

    /**
     * Finalize current session: move items to history, update session metadata.
     */
    public long finishSession(String name, String dateTime, int itemCount, int totalQty) {
        long newSessionId;
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put(COL_S_NAME, name != null ? name : "");
            cv.put(COL_S_DATE_TIME, dateTime);
            cv.put(COL_S_ITEM_COUNT, itemCount);
            cv.put(COL_S_TOTAL_QTY, totalQty);
            cv.put(COL_S_EXPORTED, 1);
            newSessionId = db.insert(TABLE_SESSIONS, null, cv);

            ContentValues updateCv = new ContentValues();
            updateCv.put(COL_SESSION_ID, newSessionId);
            db.update(TABLE_ITEMS, updateCv, COL_SESSION_ID + " = ?",
                    new String[]{String.valueOf(ACTIVE_SESSION_ID)});
        }
        return newSessionId;
    }

    /**
     * Get all completed sessions (exclude active session), most recent first.
     */
    public ArrayList<InventorySession> getSessions() {
        ArrayList<InventorySession> list = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            Cursor cursor = db.rawQuery("SELECT " + COL_ID + ", " + COL_S_NAME + ", " +
                    COL_S_DATE_TIME + ", " + COL_S_ITEM_COUNT + ", " + COL_S_TOTAL_QTY +
                    ", " + COL_S_EXPORTED +
                    " FROM " + TABLE_SESSIONS +
                    " WHERE " + COL_ID + " != " + ACTIVE_SESSION_ID +
                    " ORDER BY " + COL_ID + " DESC", null);
            while (cursor.moveToNext()) {
                list.add(new InventorySession(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        cursor.getInt(4),
                        cursor.getInt(5) == 1
                ));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Get items for a specific session.
     */
    public ArrayList<InventoryItem> getSessionItems(long sessionId) {
        ArrayList<InventoryItem> list = new ArrayList<>();
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            Cursor cursor = db.rawQuery("SELECT " + COL_BARCODE + ", " + COL_QUANTITY +
                    ", " + COL_TIMESTAMP + " FROM " + TABLE_ITEMS +
                    " WHERE " + COL_SESSION_ID + " = ?" +
                    " ORDER BY " + COL_TIMESTAMP + " DESC",
                    new String[]{String.valueOf(sessionId)});
            while (cursor.moveToNext()) {
                list.add(new InventoryItem(
                        cursor.getString(0),
                        cursor.getInt(1),
                        cursor.getLong(2)
                ));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Resume a historical session: copy its items back to the active session
     * and delete the history record. Current active items are discarded.
     */
    public void resumeSession(long sessionId) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            // Clear current active session
            db.delete(TABLE_ITEMS, COL_SESSION_ID + " = ?",
                    new String[]{String.valueOf(ACTIVE_SESSION_ID)});

            // Move items from history back to active session
            ContentValues cv = new ContentValues();
            cv.put(COL_SESSION_ID, ACTIVE_SESSION_ID);
            db.update(TABLE_ITEMS, cv, COL_SESSION_ID + " = ?",
                    new String[]{String.valueOf(sessionId)});

            // Delete the history session record
            db.delete(TABLE_SESSIONS, COL_ID + " = ?",
                    new String[]{String.valueOf(sessionId)});
        }
    }

    /**
     * Delete a history session and all its items permanently.
     */
    public void deleteSession(long sessionId) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            db.delete(TABLE_ITEMS, COL_SESSION_ID + " = ?",
                    new String[]{String.valueOf(sessionId)});
            db.delete(TABLE_SESSIONS, COL_ID + " = ?",
                    new String[]{String.valueOf(sessionId)});
        }
    }

    /**
     * Ensure tables exist (call on app startup).
     */
    public void ensureTables() {
        SQLiteDatabase db = this.getWritableDatabase();
        createTables(db);
    }
}
