package de.bigboot.qcircleview;

import android.graphics.Bitmap;

import java.util.HashMap;

import static de.bigboot.qcircleview.NotificationService.Notification;

/**
 * Created by Marco Kirchner.
 */
public enum NotificationCache {
    INSTANCE;

    private HashMap<String, Bitmap> bitmaps = new HashMap<>();

    private String getNotificationKey(Notification notification) {
        return notification.getKey();
    }

    public void addNotificationImage(Notification notification, Bitmap bitmap) {
        String key = "notification_image_" + getNotificationKey(notification);
        bitmaps.put(key, bitmap);
    }

    public Bitmap getNotificationImage(Notification notification) {
        String key = "notification_image_" + getNotificationKey(notification);
        return bitmaps.get(key);
    }

    public void removeNotficationImage(Notification notification) {
        String key = "notification_image_" + getNotificationKey(notification);
        bitmaps.remove(key);
    }

    public void clear() {
        bitmaps.clear();
    }
}
