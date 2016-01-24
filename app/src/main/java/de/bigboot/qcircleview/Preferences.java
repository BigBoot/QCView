package de.bigboot.qcircleview;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.bigboot.qcircleview.config.Clock;

/**
 * Created by Marco Kirchner
 */
public class Preferences {
    private SharedPreferences sharedPrefs;
    private static final String CLOCKS = "clocks";
    private static final String ACTIVE_CLOCK = "active_clock";
    private static final String NOTIFICATION_LISTENER = "notification_listener";
    private static final String KNOCK_CODE = "knock_code";
    private static final String APP_LIST_ENABLED = "app_list_enabled";
    private static final String APP_LIST_DISABLED = "app_list_disabled";
    private static final String WEATHER_LOCATION = "weather_location";
    private static final String WEATHER_UNIT = "weather_unit";
    private static final String WEATHER_REFRESH_INTERVAL = "weather_refresh_interval";
    private static final String WEATHER_FORCE_REFRESH = "weather_force_refresh";

    public enum BooleanSettings {
        FirstStart("first_start", true),
        HasRoot("has_root", false),
        DT2WFix("dt2w_fix", true),
        KnockCodeEnabled("knock_enabled", false),
        AutoUnlock("auto_unlock", true),
        CallUi("call_ui", false),
        AlternativeCall("alternative_call", false),
        HideLauncherLabels("hide_launcher_labels", false),
        EnableLauncher("enable_launcher", true),
        WeatherUsingGPS("weather_gps", true),
        AutoUpdate("auto_update", true);

        public final String key;
        public final boolean defaultValue;

        private BooleanSettings(String key, boolean defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }
    }

    public Preferences (Context context) {
        sharedPrefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    public Set<Clock> getClocks() {
        Set<Clock> clocks = new HashSet<Clock>();
        Collections.addAll(clocks, Clock.STATIC_CLOCKS);
        for(String s : sharedPrefs.getStringSet(CLOCKS, new HashSet<String>())) {
            Clock c = null;
            try {
                c = Clock.fromXML(s);
                if(c != null)
                    clocks.add(c);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return clocks;
    }

    public Clock getActiveClock() {
        try {
            return Clock.fromXML(sharedPrefs.getString(ACTIVE_CLOCK, ""));
        } catch (IOException e) {
            return null;
        }
    }

    public void addClock(Clock clock) {
        Set<Clock> clocks = getClocks();
        clocks.add(clock);

        Set<String> set = new HashSet<String>(clocks.size());
        for(Clock c : clocks) {
            set.add(c.toXML());
        }
        sharedPrefs.edit().putStringSet(CLOCKS, set).apply();
    }

    public void removeClock(Clock clock) {
        Set<Clock> clocks = getClocks();
        clocks.remove(clock);

        Set<String> set = new HashSet<String>(clocks.size());
        for(Clock c : clocks) {
            set.add(c.toXML());
        }
        sharedPrefs.edit().putStringSet(CLOCKS, set).apply();

        if(clock != null && clock.equals(getActiveClock()))
            setActiveClock(null);
    }

    public void setActiveClock(Clock activeClock) {
        if(activeClock == null)
            sharedPrefs.edit().putString(ACTIVE_CLOCK, null).apply();
        else
            sharedPrefs.edit().putString(ACTIVE_CLOCK, activeClock.toXML()).apply();
    }

    public boolean isNotificationListener() {
        return sharedPrefs.getBoolean(NOTIFICATION_LISTENER, false);
    }

    public void setNotificationListener(boolean value) {
        sharedPrefs.edit().putBoolean(NOTIFICATION_LISTENER, value).apply();
    }

    public void setKnockCode(int[] knockSequence) {
        sharedPrefs.edit().putString(KNOCK_CODE, new JSONArray(Arrays.asList(knockSequence)).toString()).apply();
    }

    public int[] getKnockCode() {
        String json = sharedPrefs.getString(KNOCK_CODE, "");
        try {
            JSONArray array = new JSONArray(json).getJSONArray(0);
            int[] result = new int[array.length()];
            for (int i = 0; i < array.length(); i++) {
                result[i] = array.getInt(i);
            }
            return result;
        } catch (JSONException ignored) {}
        return new int[0];
    }

    public boolean getBoolean(BooleanSettings settings) {
        return sharedPrefs.getBoolean(settings.key, settings.defaultValue);
    }

    public void putBoolean(BooleanSettings settings, boolean value) {
        sharedPrefs.edit().putBoolean(settings.key, value).apply();
    }


    public void setEnabledApps(List<String> enabledApps) {
        sharedPrefs.edit().putString(APP_LIST_ENABLED, TextUtils.join(";", enabledApps)).apply();
    }

    public void setDisabledApps(List<String> disabledApps) {
        sharedPrefs.edit().putString(APP_LIST_DISABLED, TextUtils.join(";", disabledApps)).apply();
    }

    public List<String> getEnabledApps() {
        return Arrays.asList(sharedPrefs.getString(APP_LIST_ENABLED, "").split(";"));
    }

    public List<String> getDisbledApps() {
        return Arrays.asList(sharedPrefs.getString(APP_LIST_DISABLED, "").split(";"));
    }

    public String getWeatherLocation() {
        return sharedPrefs.getString(WEATHER_LOCATION, "");
    }

    public void setWeatherLocation(String location) {
        sharedPrefs.edit().putString(WEATHER_LOCATION, location).apply();
    }

    public String getWeatherUnit() {
        return sharedPrefs.getString(WEATHER_UNIT, "C");
    }

    public void setWeatherUnit(String unit) {
        sharedPrefs.edit().putString(WEATHER_UNIT, unit).apply();
    }

    public long getWeatherRefreshInterval() {
        return sharedPrefs.getLong(WEATHER_REFRESH_INTERVAL, 600);
    }

    public void setWeatherRefreshInterval(long Interval) {
        sharedPrefs.edit().putLong(WEATHER_REFRESH_INTERVAL, Interval).apply();
    }

    public void forceWeatherRefresh() {
        sharedPrefs.edit().putBoolean(WEATHER_FORCE_REFRESH, true).apply();
    }

    public boolean weatherCheckForceRefresh() {
        boolean value = sharedPrefs.getBoolean(WEATHER_FORCE_REFRESH, false);
        sharedPrefs.edit().putBoolean(WEATHER_FORCE_REFRESH, false);
        return value;
    }
}