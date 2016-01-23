package de.bigboot.qcircleview.utils;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.androidannotations.annotations.EReceiver;
import org.androidannotations.annotations.ReceiverAction;
import org.androidannotations.api.support.content.AbstractBroadcastReceiver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.bigboot.qcircleview.Preferences;

@EReceiver
public class AppsLoader extends AbstractBroadcastReceiver {
    private static String LG_CIRCLE_APP_INTENT = "com.lge.quickcover";
    private static String CIRCLE_APP_INTENT = "de.bigboot.qcircleview.qcircleapp";

    public Callback callback = null;

    public void registerReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        context.registerReceiver(this, intentFilter);
    }

    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(this);
    }

    public void loadApps(final Context context) {
        new AsyncTask<Void, Void, ArrayList<AppEntry>>() {

            @Override
            protected void onPostExecute(final ArrayList<AppEntry> appEntries) {
                Preferences prefs = new Preferences(context);
                final List<String> enabledAppActivities = new ArrayList<String>(prefs.getEnabledApps());
                final List<String> disabledAppActivities = prefs.getDisbledApps();
                ArrayList<AppEntry> enabledEntries = new ArrayList<AppEntry>(appEntries.size());
                ArrayList<AppEntry> disabledEntries = new ArrayList<AppEntry>(appEntries.size());
                ArrayList<AppEntry> newEntries = new ArrayList<AppEntry>(appEntries.size());
                for(AppEntry entry : appEntries) {
                    if (enabledAppActivities.contains(entry.getActivity()))
                        enabledEntries.add(entry);
                    else if (disabledAppActivities.contains(entry.getActivity()))
                        disabledEntries.add(entry);
                    else
                        newEntries.add(entry);
                }
                Collections.sort(enabledEntries, new Comparator<AppEntry>() {
                    @Override
                    public int compare(AppEntry lhs, AppEntry rhs) {
                        int di = enabledAppActivities.indexOf(lhs.getActivity()) - enabledAppActivities.indexOf(rhs.getActivity());
                        return di < 0 ? -1 : di > 0 ? 1 : 0;
                    }
                });
                Collections.sort(disabledEntries, new Comparator<AppEntry>() {
                    @Override
                    public int compare(AppEntry lhs, AppEntry rhs) {
                        int di = disabledAppActivities.indexOf(lhs.getActivity()) - disabledAppActivities.indexOf(rhs.getActivity());
                        return di < 0 ? -1 : di > 0 ? 1 : 0;
                    }
                });
                for (AppEntry newEntry : newEntries) {
                    enabledEntries.add(newEntry);
                    enabledAppActivities.add(newEntry.getActivity());
                }
                if (newEntries.size() > 0) {
                    prefs.setEnabledApps(enabledAppActivities);
                }
                if (callback != null)
                    callback.appsLoaded(enabledEntries, disabledEntries);
            }

            @Override
            protected ArrayList<AppEntry> doInBackground(Void... voids) {
                ArrayList<AppEntry> items = getApps(context);
                Collections.sort(items, new Comparator<AppEntry>() {
                    @Override
                    public int compare(AppEntry appEntry, AppEntry appEntry2) {
                        return appEntry.getName().compareTo(appEntry2.getName());
                    }
                });
                return items;
            }
        }.execute();
    }

    public static ArrayList<AppEntry> getApps(Context context) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(new Intent(CIRCLE_APP_INTENT), 0);
        resInfoList.addAll(context.getPackageManager().queryIntentActivities(new Intent(LG_CIRCLE_APP_INTENT), 0));
        ArrayList<AppEntry> items = new ArrayList<>(resInfoList.size());
        for (ResolveInfo info : resInfoList) {
            AppEntry item = AppEntry.create(info, context);
            items.add(item);
        }
        return items;
    }

    @ReceiverAction(value = {Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED}, dataSchemes = "package")
    protected void onPackageListChanged(Context context, Intent intent) {
        Bundle b = intent.getExtras();
        int uid = b.getInt(Intent.EXTRA_UID);
        if (isQuickApp(context, uid)) {
            loadApps(context);
        }
    }

    private static boolean isQuickApp(Context context, int uid) {
        String[] packages = context.getPackageManager().getPackagesForUid(uid);
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(new Intent(CIRCLE_APP_INTENT), 0);
        resInfoList.addAll(context.getPackageManager().queryIntentActivities(new Intent(LG_CIRCLE_APP_INTENT), 0));
        ArrayList<String> quickcircleList = new ArrayList<>(resInfoList.size());
        for (ResolveInfo info : resInfoList) {
            quickcircleList.add(new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name).flattenToString());
        }
        for (String pkg : packages) {
            PackageInfo info;
            try {
                info = context.getPackageManager().getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            for (ActivityInfo aInfo : info.activities) {
                ComponentName name = new ComponentName(aInfo.applicationInfo.packageName, aInfo.name);
                if (quickcircleList.contains(name.flattenToString())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class AppEntry implements Parcelable {
        private ResolveInfo resolveInfo;
        private String name;
        private String activity;

        static AppEntry create(ResolveInfo info, Context context) {
            AppEntry item = new AppEntry();
            item.name = info.loadLabel(context.getPackageManager()).toString();
            String packageName = info.activityInfo.applicationInfo.packageName;
            item.activity = new ComponentName(packageName, info.activityInfo.name).flattenToString();
            item.resolveInfo = info;
            return item;
        }

        private AppEntry() {
        }

        public String getActivity() {
            return activity;
        }

        public String getName() {
            return name;
        }

        public Drawable loadIcon(Context context) {
            return resolveInfo.loadIcon(context.getPackageManager());
        }

        @Override
        public int hashCode() {
            return activity.hashCode() + 17;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() == this.getClass()) {
                AppEntry item = (AppEntry) obj;
                if (this.activity.equals(item.activity)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(this.resolveInfo, 0);
            dest.writeString(this.name);
            dest.writeString(this.activity);
        }

        private AppEntry(Parcel in) {
            this.resolveInfo = in.readParcelable(ResolveInfo.class.getClassLoader());
            this.name = in.readString();
            this.activity = in.readString();
        }

        public static final Parcelable.Creator<AppEntry> CREATOR = new Parcelable.Creator<AppEntry>() {
            public AppEntry createFromParcel(Parcel source) {
                return new AppEntry(source);
            }

            public AppEntry[] newArray(int size) {
                return new AppEntry[size];
            }
        };
    }

    public interface Callback {
        void appsLoaded(List<AppEntry> enabledApps, List<AppEntry> disabledApps);
    }
}
