package com.android.cooper.lib.blelogic.utils;

import android.app.Instrumentation;
import android.view.KeyEvent;

public class RcUtils {
    private static final Instrumentation sInstrumentation = new Instrumentation();

    private RcUtils() {
    }

    public static synchronized void dispatchKeyEvent(int newKeyCode) {
        switch (newKeyCode) {
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                sInstrumentation.sendKeyDownUpSync(newKeyCode);
                return;
            case KeyEvent.KEYCODE_POWER:
                sInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_DOWN, newKeyCode));
                break;
        }
    }
}
