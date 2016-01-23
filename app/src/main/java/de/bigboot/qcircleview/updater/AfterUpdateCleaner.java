package de.bigboot.qcircleview.updater;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FilenameFilter;

import de.bigboot.qcircleview.SmartcoverService_;
import de.bigboot.qcircleview.config.QuickcirclemodSettings_;

public class AfterUpdateCleaner extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        for(File file : context.getCacheDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.startsWith("update_") && s.endsWith(".apk");
            }
        })) {
            file.delete();
        }
        SmartcoverService_.intent(context).start();
        QuickcirclemodSettings_.intent(context).flags(Intent.FLAG_ACTIVITY_NEW_TASK).start();
    }
}
