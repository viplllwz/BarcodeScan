package com.module.dot;

import android.app.Application;

import com.module.dot.data.local.InventoryDatabase;

public class DotApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try (InventoryDatabase db = new InventoryDatabase(this)) {
            db.ensureTables();
        }
    }
}
