package de.bigboot.qcircleview.updater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.bigboot.qcircleview.BuildConfig;
import de.bigboot.qcircleview.R;

public class UpdateManager {
    private static final int NOTIFICATION_ID = 0x42;
    private static final int INTENT_ID = 42;
    private static final String API_ENDPOINT = "https://qcthemer.net/api/v1/update";

    private Context context;
    private NotificationManager notificationManager;
    Notification.Builder notificationBuilder;

    public UpdateManager(Context context) {
        this.context = context.getApplicationContext();
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new Notification.Builder(context);
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(Notification.PRIORITY_LOW)
                .setContentTitle(context.getString(R.string.app_name));
    }

    public void checkForUpdate(int currentVersion) {
        String url = API_ENDPOINT + "?version=" + currentVersion;
        Ion.with(context)
                .load(url)
                .noCache()
                .asJsonArray()
                .setCallback(new FutureCallback<JsonArray>() {
                    @Override
                    public void onCompleted(Exception e, JsonArray result) {
                        if (e == null) {
                            showUpdateNotification(parseUpdates(result));
                        } else {
                            notificationBuilder.setContentText(context.getString(R.string.update_network_error));
                            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                        }
                    }
                });
    }

    public void checkForUpdate() {
        checkForUpdate(BuildConfig.VERSION_CODE);
    }

    private Update[] parseUpdates(JsonArray result) {
        ArrayList<Update> updates = new ArrayList<>(result.size());

        for (JsonElement elem : result) {
            try {
                JsonObject obj = elem.getAsJsonObject();

                Update.Builder updateBuilder = new Update.Builder();

                updateBuilder.versionName(obj.get("version").getAsString());
                updateBuilder.downloadUrl(obj.get("download").getAsString());
                updateBuilder.versionCode(obj.get("version_code").getAsInt());

                for (JsonElement changeElem : obj.get("change").getAsJsonArray()) {
                    updateBuilder.addChange(changeElem.getAsString());
                }

                updates.add(updateBuilder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        return updates.toArray(new Update[updates.size()]);
    }

    private void showUpdateNotification(final Update[] updates) {
        if (updates.length == 0)
            return;

        notificationBuilder.setContentIntent(
                PendingIntent.getActivity(context, INTENT_ID,
                        UpdateDialogActivity_.intent(context).flags(Intent.FLAG_ACTIVITY_NEW_TASK).updates(updates).get(),
                        PendingIntent.FLAG_ONE_SHOT)
        );
        notificationBuilder.setContentText(context.getString(R.string.update_found));
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        notificationBuilder.setContentIntent(null);
    }

    public void downloadUpdate(Update update) {
        notificationBuilder.setContentText(context.getString(R.string.update_download));
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        try {
            Ion.with(context)
                    .load(update.getDownloadUrl())
                    .progress(new ProgressCallback() {
                        @Override
                        public void onProgress(long downloaded, long total) {
                            notificationBuilder.setProgress((int) total, (int) downloaded, false);
                            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                        }
                    })
                    .write(File.createTempFile("update_", ".apk", context.getCacheDir()))
                    .setCallback(new FutureCallback<File>() {
                        @Override
                        public void onCompleted(Exception e, File result) {
                            if (e == null) {
                                result.setReadable(true, false);
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(result), "application/vnd.android.package-archive");
                                intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            } else {
                                notificationBuilder.setContentText(context.getString(R.string.update_download_error));
                                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                                e.printStackTrace();
                                result.delete();
                            }
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
