package de.bigboot.qcircleview;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.TimeoutException;

import de.bigboot.qcirclelib.ScreenUtils;

/**
 * Created by Marco Kirchner.
 */
public class RootTools {
    public static final String PHONE_COMMAND_END_CALL = "TRANSACTION_endCall";
    public static final String PHONE_COMMAND_ANSWER_CALL = "TRANSACTION_answerRingingCall";
    public static final String PHONE_COMMAND_SILENCE_RINGER = "TRANSACTION_silenceRinger";

    public enum SELinuxMode {
        Enforcing,
        Permissive,
        Undefined
    }

    public static boolean hasRootAccess() {
        return RootShell.isAccessGiven();
    }

    public static SELinuxMode getSELinuxMode() {
        class GetEnforceCommand extends Command {
            public SELinuxMode mode = SELinuxMode.Undefined;

            public GetEnforceCommand(int id) {
                super(id, false, "/system/bin/getenforce");
            }

            @Override
            public void commandOutput(int id, String line) {
                mode = line.contains("Enforcing") ? SELinuxMode.Enforcing
                        : line.contains("Permissive") ? SELinuxMode.Permissive
                        : SELinuxMode.Undefined;
                super.commandOutput(id, line);
            }
        }
        GetEnforceCommand command = new GetEnforceCommand(0);
        tryRun(command, true);
        waitForFinish(command);
        return command.mode;
    }

    public static boolean setSELinuxMode(SELinuxMode mode) {
        Command command = new Command(0, "/system/bin/setenforce "
                + (mode == SELinuxMode.Enforcing ? "1" : "0")) {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
            }

            @Override
            public void commandTerminated(int id, String reason) {
                super.commandTerminated(id, reason);
            }

            @Override
            public void commandCompleted(int id, int exitcode) {
                super.commandCompleted(id, exitcode);
            }
        };
        return tryRun(command, true);
    }

    public static boolean screenOff(Context context) {
        if (ScreenUtils.isScreenOn(context)) {
            Command command = new Command(0, "input keyevent 26");
            return tryRun(command, true);
        }
        return true;
    }

    public static boolean setAirplaneMode(boolean active) {
        Command command = new Command(0,
                "settings put global airplane_mode_on " + (active ? "1" : "0"),
                "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state " + (active ? "true" : "false"));
        return tryRun(command, true);
    }

    public static boolean enableDT2W() {
        Command command = new Command(0,
                "echo \"1 1 0 0\" > /sys/devices/virtual/input/lge_touch/lpwg_notify");
        return tryRun(command, true);
    }

    public static boolean insertLGSmartcoverSettings() {
        Command command = new Command(0,
                "settings put global quick_view_enable 1",
                "settings put global cover_type 3");
        return tryRun(command, true);
    }

    public static boolean chmod(File file, String permission) {
        return chmod(file, permission, false);
    }

    public static boolean chmod(File file, String permission, boolean useRoot) {
        Command command = new Command(0, "chmod " + permission + " " + file.getAbsolutePath());
        return tryRun(command, useRoot);
    }

    private static int getPhoneCommandId(String command) throws IllegalArgumentException {
        try {
            Class clazz = Class.forName("com.android.internal.telephony.ITelephony$Stub");
            Field field = clazz.getDeclaredField(command);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(command + " is not a valid phone command");
        }
    }

    public static boolean sendPhoneCommand(String command) {
        try {
            int commandId = getPhoneCommandId(command);
            return tryRun(new Command(0, "service call phone " + commandId), true);
        } catch (IllegalArgumentException ex) {
            Log.e(RootTools.class.getSimpleName(), ex.getMessage());
            return false;
        }
    }

    public static boolean acceptCallHeadsetEvent() {
        Command command = new Command(0, "input keyevent 79");
        return tryRun(command, true);
    }

    public static boolean dismissCallHeadsetEvent() {
        Command command = new Command(0, "input keyevent 6");
        return tryRun(command, true);
    }

    private static boolean tryRun(Command command, boolean useRoot) {
        try {
            try {
                RootShell.getShell(useRoot).add(command);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (RootDeniedException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void waitForFinish(Command command) {
        while (!command.isFinished()) {
            try {
                Thread.sleep(50);
            }
            catch (InterruptedException ignore)  {}
        }
    }
}
