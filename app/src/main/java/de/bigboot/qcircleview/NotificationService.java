package de.bigboot.qcircleview;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.Receiver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.app.Notification.EXTRA_TITLE;

@EService
public class NotificationService extends NotificationListenerService {
    public static final String ACTION_COMMAND = "de.bigboot.qcircleview.NotificationService.ACTION_COMMAND";
    public static final String ACTION_NOTIFICATION_ADDED = "de.bigboot.qcircleview.NotificationService.ACTION_NOTIFICATION_ADDED";
    public static final String ACTION_NOTIFICATION_REMOVED = "de.bigboot.qcircleview.NotificationService.ACTION_NOTIFICATION_REMOVED";
    public static final String ACTION_IS_CONNECTED = "de.bigboot.qcircleview.NotificationService.ACTION_IS_CONNECTED";

    public static final String EXTRA_NOTIFCATION = "de.bigboot.qcircleview.NotificationService.EXTRA_NOTIFCATION";
    public static final String EXTRA_IS_CONNECTED = "de.bigboot.qcircleview.NotificationService.EXTRA_IS_CONNECTED";
    public static final String EXTRA_INDEX = "de.bigboot.qcircleview.NotificationService.EXTRA_INDEX";
    public static final String EXTRA_COMMAND = "de.bigboot.qcircleview.NotificationService.EXTRA_COMMAND";

    public static final String COMMAND_LIST = "de.bigboot.qcircleview.NotificationService.COMMAND_LIST";
    public static final String COMMAND_IS_CONNECTED = "de.bigboot.qcircleview.NotificationService.COMMAND_IS_CONNECTED";
    public static final String COMMAND_DELETE_NOTIFICATION = "de.bigboot.qcircleview.NotificationService.COMMAND_DELETE_NOTIFICATION";


    private final ArrayList<Notification> notifications = new ArrayList<>();
    private boolean connected = false;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        connected = true;
        init();
        sendBroadcast(new Intent(ACTION_IS_CONNECTED).putExtra(EXTRA_IS_CONNECTED, true));
    }

    protected void init() {
        synchronized (notifications) {
            for (StatusBarNotification sbn2 : this.getActiveNotifications()) {
                Notification notification = Notification.parse(sbn2);
                if (notification == null)
                    continue;
                notifications.add(notification);
            }
            for (Notification notification : notifications) {
                Intent i = new Intent(ACTION_NOTIFICATION_ADDED);
                i.putExtra(EXTRA_NOTIFCATION, notification);
                sendBroadcast(i);
            }
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = Notification.parse(sbn);
        if(notification == null)
            return;
        synchronized (notifications) {
            Iterator<Notification> it = notifications.iterator();
            while (it.hasNext()) {
                if (it.next().getKey().equals(notification.getKey())) {
                    it.remove();
                }
            }
            notifications.add(notification);
        }
        Intent i = new Intent(ACTION_NOTIFICATION_ADDED);
        i.putExtra(EXTRA_NOTIFCATION, notification);
        sendBroadcast(i);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        int index = 0;
        synchronized (notifications) {
            Iterator<Notification> it = notifications.iterator();
            while (it.hasNext()) {
                Notification notification = it.next();
                if (notification.getKey().equals(sbn.getKey())) {
                    NotificationCache.INSTANCE.removeNotficationImage(notification);
                    it.remove();
                    Intent i = new Intent(ACTION_NOTIFICATION_REMOVED);
                    i.putExtra(EXTRA_NOTIFCATION, notification);
                    i.putExtra(EXTRA_INDEX, index);
                    sendBroadcast(i);
                }
                index++;
            }
        }
    }

    @Receiver(actions = ACTION_COMMAND)
    protected void onCommand(@Receiver.Extra(EXTRA_COMMAND)String command, Intent intent) {
        if(command == null)
            return;
        switch (command) {
            case COMMAND_LIST:
                synchronized (notifications) {
                    for (Notification notification : notifications) {
                        Intent i = new Intent(ACTION_NOTIFICATION_ADDED);
                        i.putExtra(EXTRA_NOTIFCATION, notification);
                        sendBroadcast(i);
                    }
                }
                break;
            case COMMAND_IS_CONNECTED:
                sendBroadcast(new Intent(ACTION_IS_CONNECTED).putExtra(EXTRA_IS_CONNECTED, connected));
            case COMMAND_DELETE_NOTIFICATION:
                Notification notification = intent.getParcelableExtra(EXTRA_NOTIFCATION);
                if (notification != null) {
                    cancelNotification(notification.getKey());
                }
        }
    }

    public static class Notification implements Parcelable {
        private String key;
        private String text;
        private String title;
        private String packageName;
        private String template;
        private PendingIntent contentIntent;
        private boolean isClearable;
        private List<Action> actions;

        @Nullable
        private static Notification parse(StatusBarNotification statusBarNotification) {
            try {
                Notification.Builder builder = new Notification.Builder();
                android.app.Notification notification = statusBarNotification.getNotification();

                builder.setPackageName(statusBarNotification.getPackageName())
                        .setKey(statusBarNotification.getKey())
                        .setClearable(statusBarNotification.isClearable())
                        .setContentIntent(notification.contentIntent);

                if (notification.extras.getCharSequence(EXTRA_TITLE) != null) {
                    builder.setTitle(notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE).toString());
                }

                if (notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT) != null) {
                    builder.setText(
                            notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString());
                } else {
                    if (notification.tickerText != null) {
                        builder.setText(notification.tickerText.toString());
                    }
                }
                if (notification.extras.getString(android.app.Notification.EXTRA_TEMPLATE) != null) {
                    builder.setTemplate(notification.extras.getString(android.app.Notification.EXTRA_TEMPLATE));
                }
                if (notification.actions != null) {
                    ArrayList<Action> actions = new ArrayList(notification.actions.length);
                    for(android.app.Notification.Action action : notification.actions) {
                        actions.add(new Action(action.title.toString(), action.actionIntent));
                    }
                    builder.setActions(actions);
                }
                Notification ret = builder.createNotification();
                NotificationCache.INSTANCE.addNotificationImage(ret, notification.largeIcon);
                return ret;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }


        Notification(String key,
                     String text,
                     String title,
                     String packageName,
                     String template,
                     PendingIntent contentIntent,
                     boolean isClearable,
                     List<Action> actions) {
            this.key = key;
            this.text = text;
            this.title = title;
            this.packageName = packageName;
            this.template = template;
            this.contentIntent = contentIntent;
            this.isClearable = isClearable;
            this.actions = actions;
        }

        public String getKey() { return key; }

        public String getText() {
            return text;
        }

        public String getTitle() {
            return title;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getTemplate() {
            return template;
        }

        public PendingIntent getContentIntent() {
            return contentIntent;
        }

        public boolean isClearable() { return isClearable; }

        public List<Action> getActions() {
            return actions;
        }

        Notification(Parcel in) {
            key = in.readString();
            text = in.readString();
            title = in.readString();
            packageName = in.readString();
            template = in.readString();
            contentIntent = in.readParcelable(PendingIntent.class.getClassLoader());
            isClearable = in.readInt() == 1;
            int actionsCount = in.readInt();
            actions = new ArrayList<>(actionsCount);
            for(int i = 0; i < actionsCount; i++) {
                actions.add(in.<Action>readParcelable(Action.class.getClassLoader()));
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(key);
            dest.writeString(text);
            dest.writeString(title);
            dest.writeString(packageName);
            dest.writeString(template);
            dest.writeParcelable(contentIntent, flags);
            dest.writeInt(isClearable?1:0);
            dest.writeInt(actions.size());
            for(Action action : actions)
                dest.writeParcelable(action, flags);
        }

        public static class Builder {
            private String key;
            private String text = "";
            private String title = "";
            private String packageName;
            private String template;
            private PendingIntent contentIntent = null;
            private boolean isClearable;
            private List<Action> actions = new ArrayList<>();

            public Builder setKey(String key) {
                this.key = key;
                return this;
            }

            public Builder setText(String text) {
                this.text = text;
                return this;
            }

            public Builder setTitle(String title) {
                this.title = title;
                return this;
            }

            public Builder setPackageName(String packageName) {
                this.packageName = packageName;
                return this;
            }

            public Builder setTemplate(String template) {
                this.template = template;
                return this;
            }

            public Builder setContentIntent(PendingIntent contentIntent) {
                this.contentIntent = contentIntent;
                return this;
            }

            public Builder setClearable(boolean isClearable) {
                this.isClearable = isClearable;
                return this;
            }

            public Builder setActions(List<Action> actions) {
                this.actions = actions;
                return this;
            }

            public Notification createNotification() {
                return new Notification(key, text, title, packageName, template, contentIntent, isClearable, actions);
            }
        }

        @SuppressWarnings("unused")
        public static final Creator<Notification> CREATOR = new Creator<Notification>() {
            @Override
            public Notification createFromParcel(Parcel in) {
                return new Notification(in);
            }

            @Override
            public Notification[] newArray(int size) {
                return new Notification[size];
            }
        };
    }

    public static class Action implements Parcelable {
        private String title;
        private PendingIntent intent;

        protected Action(String title, PendingIntent intent) {
            this.title = title;
            this.intent = intent;
        }

        protected Action(Parcel in) {
            this.title = in.readString();
            this.intent = in.readParcelable(PendingIntent.class.getClassLoader());
        }

        public String getTitle() {
            return title;
        }

        public PendingIntent getIntent() {
            return intent;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(this.title);
            parcel.writeParcelable(intent, flags);
        }

        @SuppressWarnings("unused")
        public static final Creator<Action> CREATOR = new Creator<Action>() {
            @Override
            public Action createFromParcel(Parcel in) {
                return new Action(in);
            }

            @Override
            public Action[] newArray(int size) {
                return new Action[size];
            }
        };
    }
}