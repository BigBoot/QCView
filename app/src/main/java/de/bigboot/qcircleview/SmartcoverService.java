package de.bigboot.qcircleview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.FileObserver;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import de.bigboot.qcirclelib.QCircleActivity;
import de.bigboot.qcircleview.cover.CallHandler;
import de.bigboot.qcircleview.updater.UpdateManager;
import de.bigboot.qcircleview.utils.CallManager;
import de.bigboot.qcircleview.utils.HardwareNotFoundException;
import de.bigboot.qcircleview.utils.SmartcoverObserver;
import de.bigboot.qcircleview.utils.UEventStateChangeHandler;

/**
 * Created by Marco Kirchner.
 */
@EService
public class SmartcoverService extends Service {
    private static final int SMARTCOVER_OPEN = 0;
    private static final int SMATCOVER_CLOSED = 1;

    public static final String ACTION_COMMAND = QCircleActivity.SMARTCOVER_ACTION_COMMAND;
    public static final String ACTION_COVER_STATE = QCircleActivity.SMARTCOVER_ACTION_COVER_STATE;

    public static final String EXTRA_COMMAND = QCircleActivity.SMARTCOVER_EXTRA_COMMAND;
    public static final String EXTRA_SCREEN_STATE = QCircleActivity.SMARTCOVER_EXTRA_SCREEN_STATE;
    public static final String EXTRA_STATE_CHANGED = QCircleActivity.SMARTCOVER_EXTRA_STATE_CHANGED;

    public static final String COMMAND_POLL_STATE = QCircleActivity.SMARTCOVER_COMMAND_POLL_STATE;
    public static final String COMMAND_SCREEN_OFF = QCircleActivity.SMARTCOVER_COMMAND_SCREEN_OFF;
    public static final String COMMAND_ENABLE_AIRPLANE_MODE = QCircleActivity.SMARTCOVER_COMMAND_ENABLE_AIRPLANE_MODE;
    public static final String COMMAND_DISABLE_AIRPLANE_MODE = QCircleActivity.SMARTCOVER_COMMAND_DISABLE_AIRPLANE_MODE;

    protected static final int LG_EXTRA_ACCESSORY_COVER_OPENED = 0;
    protected static final int LG_EXTRA_ACCESSORY_COVER_CLOSED = 1;
    protected static final String LG_EXTRA_ACCESSORY_COVER_STATE = "com.lge.intent.extra.ACCESSORY_COVER_STATE";
    protected static final String LG_ACTION_ACCESSORY_COVER_EVENT = "com.lge.android.intent.action.ACCESSORY_COVER_EVENT";

    private static final String PROXIMITY_ON_WAKE = "proximity_on_wake";

    private static long LAST_UPDATE_CHECK = 0l;
    private static final long UPDATE_INTERVAL = 1000l * 60l * 60 * 6l;  // 6 Hours in ms

    private String TAG = this.getClass().getSimpleName();
    private static BroadcastReceiver screenReceiver;
    private int proximity_on_wake;
    KeyguardManager.KeyguardLock lock;
    private Preferences prefs;


    private FileObserver observer;
    private SmartcoverObserver smartcoverObserver;
    private CallHandler callHandler;

    private enum CoverState {
        Open,
        Closed,
        Unknown;

        public static CoverState fromInt(int value) {
            switch (value) {
                case 0:
                    return Open;
                case 1:
                    return Closed;
                default:
                    return Unknown;
            }
        }
    }

    private CoverState state = CoverState.Unknown;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            if (ACTION_COMMAND.equals(intent.getAction())) {
                String command = intent.getStringExtra(EXTRA_COMMAND);
                if (COMMAND_SCREEN_OFF.equals(command)) {
                    if (callHandler != null && !callHandler.isInCall())
                        RootTools.screenOff(this);
                } else if (COMMAND_POLL_STATE.equals(command)) {
                    Intent i = new Intent(ACTION_COVER_STATE);
                    i.putExtra(EXTRA_SCREEN_STATE, state.toString());
                    i.putExtra(EXTRA_STATE_CHANGED, false);
                    sendBroadcast(i);
                } else if (COMMAND_ENABLE_AIRPLANE_MODE.equals(command)) {
                    RootTools.setAirplaneMode(true);
                } else if (COMMAND_DISABLE_AIRPLANE_MODE.equals(command)) {
                    RootTools.setAirplaneMode(false);
                }
            }
        }
        if(prefs.getBoolean(Preferences.BooleanSettings.DT2WFix)) {
            RootTools.enableDT2W();
        }
        CallManager.removeListener(callHandler);
        if(prefs.getBoolean(Preferences.BooleanSettings.CallUi)) {
            CallManager.addListener(callHandler);
        }
        return START_STICKY;
    }

    @Background
    protected void setKeyguardEnabled(boolean enabled) {
        if (!prefs.getBoolean(Preferences.BooleanSettings.AutoUnlock))
            return;
        if(enabled) {
            // Add a small delay, so we can unlock the screen before reenabling the keyguard
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.reenableKeyguard();
        } else {
            lock.disableKeyguard();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        RootTools.enableDT2W();
        RootTools.insertLGSmartcoverSettings();

        callHandler = new CallHandler(this);

        prefs = new Preferences(this);

        proximity_on_wake = Settings.System.getInt(getContentResolver(), PROXIMITY_ON_WAKE, 0);

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        lock = keyguardManager.newKeyguardLock(Activity.KEYGUARD_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        screenReceiver = new ScreenReceiver();
        registerReceiver(screenReceiver, filter);
        startWatching();

        Notification notification=new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.service_running))
                .setPriority(Notification.PRIORITY_MIN).build();

        startForeground(1337, notification);
    }

    private void refreshStatus() {
        try {
            Scanner sc = new Scanner(new File("/sys/devices/virtual/switch/smartcover/state"));
            int result = Integer.parseInt(sc.nextLine());
            CoverState cs = CoverState.fromInt(result);
            sc.close();

            if(cs != state) {
                updateStatus(cs);
            }
        } catch (FileNotFoundException e) {
            Log.e(getString(R.string.app_name), "Hall effect sensor device file not found!");
        }
    }

    @Override
    public void onDestroy() {
        // Ensure keyguard is enabled
        lock.reenableKeyguard();
        CallManager.removeListener(callHandler);
        callHandler.onDestroy();
        unregisterReceiver(screenReceiver);
        super.onDestroy();
    }

    private void updateStatus(CoverState newState) {
        state = newState;
        Intent intent = new Intent(ACTION_COVER_STATE);
        intent.putExtra(EXTRA_SCREEN_STATE, newState.toString());
        intent.putExtra(EXTRA_STATE_CHANGED, true);
        Intent lgIntent = new Intent(LG_ACTION_ACCESSORY_COVER_EVENT);
        if (newState == CoverState.Closed) {
            intent.putExtra(LG_EXTRA_ACCESSORY_COVER_STATE, LG_EXTRA_ACCESSORY_COVER_CLOSED);
            // Disable Keyguard
            setKeyguardEnabled(false);
            Settings.System.putInt(getContentResolver(), PROXIMITY_ON_WAKE, 0);
            onCoverClosed();
        } else if (newState == CoverState.Open) {
            intent.putExtra(LG_EXTRA_ACCESSORY_COVER_STATE, LG_EXTRA_ACCESSORY_COVER_OPENED);
            setKeyguardEnabled(true);
            Settings.System.putInt(getContentResolver(), PROXIMITY_ON_WAKE, proximity_on_wake);
            onCoverOpened();
        }
        sendBroadcast(intent);
        sendBroadcast(lgIntent);
    }

    private void onCoverOpened() {
        if (new Preferences(this).getBoolean(Preferences.BooleanSettings.AutoUpdate) &&
                LAST_UPDATE_CHECK + UPDATE_INTERVAL <= System.currentTimeMillis()) {
            LAST_UPDATE_CHECK = System.currentTimeMillis();
            new UpdateManager(this).checkForUpdate();
        }
    }

    private void onCoverClosed() {
    }

    protected void startWatching () {
        if (RootTools.getSELinuxMode() != RootTools.SELinuxMode.Permissive) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.selinux_dialog_title);
            builder.setMessage(R.string.selinux_dialog_content);

            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    RootTools.setSELinuxMode(RootTools.SELinuxMode.Permissive);
                    dialog.dismiss();
                    SmartcoverService_.intent(SmartcoverService.this).start();
                }

            });

            builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing
                    dialog.dismiss();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.show();
            SmartcoverService_.intent(this).stop();
            return;
        }
        try {
            smartcoverObserver = new SmartcoverObserver();
            smartcoverObserver.setOnUEventChangeHandler(new UEventStateChangeHandler() {
                @Override
                public void OnUEventStateChange(String NewState) {
                    refreshStatus();
                }
            });
            smartcoverObserver.start();
        } catch (HardwareNotFoundException e) {
            e.printStackTrace();
        }

        refreshStatus();
    }

    public class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)
                    || intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                refreshStatus();
            }
        }
    }

    public static class SmartcoverReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(ACTION_COMMAND.equals(intent.getAction())) {
                SmartcoverService_.intent(context)
                        .action(ACTION_COMMAND)
                        .extra(EXTRA_COMMAND, intent.getStringExtra(EXTRA_COMMAND))
                        .start();
            } else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                SmartcoverService_.intent(context).start();
            }
        }
    }
}

