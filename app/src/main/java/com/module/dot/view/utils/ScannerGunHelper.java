package com.module.dot.view.utils;

import android.view.KeyEvent;

/**
 * Helper to intercept barcode scanner gun input.
 * Scanner guns emulate a keyboard: rapid key events followed by KEYCODE_ENTER.
 */
public class ScannerGunHelper {

    private final StringBuilder buffer = new StringBuilder();
    private long lastEventTime = 0;
    private boolean scanning = false;

    private static final long SCAN_TIMEOUT_MS = 100; // Max gap between keystrokes

    /**
     * Process a KeyEvent from dispatchKeyEvent.
     * Returns the scanned barcode when a complete scan is detected, null otherwise.
     */
    public String processKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return null;
        }

        int keyCode = event.getKeyCode();
        long now = System.currentTimeMillis();

        // Check for timeout between keystrokes
        if (now - lastEventTime > SCAN_TIMEOUT_MS) {
            buffer.setLength(0);
            scanning = false;
        }
        lastEventTime = now;

        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (buffer.length() > 0) {
                String result = buffer.toString();
                buffer.setLength(0);
                scanning = false;
                return result;
            }
            return null;
        }

        // Handle printable characters
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            scanning = true;
            buffer.append((char) ('0' + (keyCode - KeyEvent.KEYCODE_0)));
        } else if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            scanning = true;
            // Check shift for case
            if (event.isShiftPressed()) {
                buffer.append((char) ('A' + (keyCode - KeyEvent.KEYCODE_A)));
            } else {
                buffer.append((char) ('a' + (keyCode - KeyEvent.KEYCODE_A)));
            }
        } else if (keyCode == KeyEvent.KEYCODE_MINUS) {
            scanning = true;
            buffer.append('-');
        } else if (keyCode == KeyEvent.KEYCODE_PERIOD) {
            scanning = true;
            buffer.append('.');
        } else if (keyCode == KeyEvent.KEYCODE_SLASH) {
            scanning = true;
            buffer.append('/');
        } else if (keyCode == KeyEvent.KEYCODE_SPACE) {
            scanning = true;
            buffer.append(' ');
        } else if (keyCode == KeyEvent.KEYCODE_NUMPAD_0 && scanning) {
            buffer.append('0');
        } else if (keyCode >= KeyEvent.KEYCODE_NUMPAD_1 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9 && scanning) {
            buffer.append((char) ('0' + (keyCode - KeyEvent.KEYCODE_NUMPAD_1 + 1)));
        } else if (scanning && event.getUnicodeChar() > 0) {
            // Fallback: use unicode char for other printable chars
            buffer.append((char) event.getUnicodeChar());
        }

        return null;
    }

    public void reset() {
        buffer.setLength(0);
        scanning = false;
    }
}
