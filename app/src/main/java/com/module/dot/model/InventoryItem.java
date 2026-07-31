package com.module.dot.model;

public class InventoryItem {
    private String barcode;
    private int quantity;
    private long timestamp;

    public InventoryItem() {}

    public InventoryItem(String barcode, int quantity, long timestamp) {
        this.barcode = barcode;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
