package de.bigboot.qcirclelib;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

public abstract class ScreenUtils {
    public static boolean isScreenOn(Context context) {
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        for (Display display : dm.getDisplays()) {
            if (display.getState() != Display.STATE_OFF) {
                return true;
            }
        }
        return false;
    }
}
