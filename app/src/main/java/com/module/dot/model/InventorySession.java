package com.module.dot.model;

public class InventorySession {
    private long id;
    private String dateTime;
    private int itemCount;
    private int totalQty;
    private boolean exported;

    public InventorySession() {}

    public InventorySession(long id, String dateTime, int itemCount, int totalQty, boolean exported) {
        this.id = id;
        this.dateTime = dateTime;
        this.itemCount = itemCount;
        this.totalQty = totalQty;
        this.exported = exported;
    }

    public InventorySession(String dateTime, int itemCount, int totalQty, boolean exported) {
        this.dateTime = dateTime;
        this.itemCount = itemCount;
        this.totalQty = totalQty;
        this.exported = exported;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public int getTotalQty() { return totalQty; }
    public void setTotalQty(int totalQty) { this.totalQty = totalQty; }

    public boolean isExported() { return exported; }
    public void setExported(boolean exported) { this.exported = exported; }
}
