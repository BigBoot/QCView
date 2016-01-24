package de.bigboot.qcircleview.config;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.FrameLayout;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.Receiver;

import java.util.Arrays;

import de.bigboot.qcircleview.NotificationService;
import de.bigboot.qcircleview.Preferences;
import de.bigboot.qcircleview.R;
import de.bigboot.qcircleview.RootTools;
import de.bigboot.qcircleview.updater.UpdateManager;

@EActivity
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setId(R.id.root);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment fragment = SettingsActivity_.SettingsFragment_.builder().build();
        fragmentTransaction.add(root.getId(), fragment);
        fragmentTransaction.commit();

        setContentView(root);
    }

    @EFragment
    public static class SettingsFragment extends PreferenceFragment {
        private Preferences preferences;
        private CheckBoxPreference notificationServicePrefs;
        private EditTextPreference weatherLocationPrefs;
        private ListPreference weatherUnitPrefs;
        private ListPreference weatherRefreshPrefs;


        @AfterViews
        protected void init() {
            preferences = new Preferences(getActivity());
            final CheckBoxPreference rootAccessPrefs = (CheckBoxPreference) findPreference("pref_root_checkbox");
            notificationServicePrefs = (CheckBoxPreference) findPreference("pref_notification_checkbox");
            weatherUnitPrefs = (ListPreference) findPreference("pref_weather_units");
            weatherLocationPrefs = (EditTextPreference) findPreference("pref_weather_location");
            weatherRefreshPrefs = (ListPreference) findPreference("pref_weather_refresh_interval");


            loadSwitchPreference(Preferences.BooleanSettings.DT2WFix);
            loadSwitchPreference(Preferences.BooleanSettings.AutoUnlock);
            loadSwitchPreference(Preferences.BooleanSettings.CallUi);
            loadSwitchPreference(Preferences.BooleanSettings.AlternativeCall);
            loadSwitchPreference(Preferences.BooleanSettings.WeatherUsingGPS);
            loadSwitchPreference(Preferences.BooleanSettings.AutoUpdate);

            rootAccessPrefs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    boolean hasRoot = RootTools.hasRootAccess();
                    preference.setSummary(hasRoot ? R.string.pref_has_root_access : R.string.pref_has_no_root_access);
                    if ((boolean) o != hasRoot) {
                        rootAccessPrefs.setChecked(hasRoot);
                        return false;
                    }
                    return true;
                }
            });
            boolean hasRoot = RootTools.hasRootAccess();
            rootAccessPrefs.setSummary(hasRoot ? R.string.pref_has_root_access : R.string.pref_has_no_root_access);
            rootAccessPrefs.setChecked(hasRoot);

            notificationServicePrefs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    return false;
                }
            });
            notificationServicePrefs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    return true;
                }
            });
            getActivity().sendBroadcast(new Intent(NotificationService.ACTION_COMMAND).putExtra(
                    NotificationService.EXTRA_COMMAND, NotificationService.COMMAND_IS_CONNECTED
            ));

            weatherLocationPrefs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preferences.setWeatherLocation((String) newValue);
                    weatherLocationPrefs.setSummary((String) newValue);
                    preferences.forceWeatherRefresh();
                    return true;
                }
            });
            weatherLocationPrefs.getOnPreferenceChangeListener().onPreferenceChange(weatherLocationPrefs, preferences.getWeatherLocation());
            weatherUnitPrefs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preferences.setWeatherUnit((String) newValue);
                    weatherUnitPrefs.setSummary(getString(R.string.pref_weather_unit_current) + " " + getWeatherUnitName((String) newValue));
                    preferences.forceWeatherRefresh();
                    return true;
                }
            });
            weatherUnitPrefs.getOnPreferenceChangeListener().onPreferenceChange(weatherUnitPrefs, preferences.getWeatherUnit());

            weatherRefreshPrefs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    long parsed = Long.parseLong((String) newValue);
                    preferences.setWeatherRefreshInterval(parsed);
                    CharSequence text = weatherRefreshPrefs.getEntries()[weatherRefreshPrefs.findIndexOfValue((String) newValue)];
                    weatherRefreshPrefs.setSummary(getString(R.string.pref_weather_refresh_interval_current) + " " + text);
                    preferences.forceWeatherRefresh();
                    return true;
                }
            });
            weatherRefreshPrefs.getOnPreferenceChangeListener().onPreferenceChange(weatherRefreshPrefs, String.valueOf(preferences.getWeatherRefreshInterval()));

            findPreference("pref_check_for_update").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new UpdateManager(getActivity()).checkForUpdate();
                    return false;
                }
            });
        }

        private String getWeatherUnitName(String weatherUnit) {
            return getResources().getStringArray(R.array.pref_weather_units)[Arrays.asList(getResources().getStringArray(R.array.pref_weather_units_values)).indexOf(weatherUnit)];
        }

        private SwitchPreference loadSwitchPreference(final Preferences.BooleanSettings setting) {
            SwitchPreference pref = (SwitchPreference) findPreference("pref_" + setting.key);
            pref.setChecked(preferences.getBoolean(setting));
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preferences.putBoolean(setting, (Boolean) o);
                    return true;
                }
            });
            return pref;
        }


        @Receiver(actions = NotificationService.ACTION_IS_CONNECTED)
        protected void onNotificationServiceIsConnected(
                @Receiver.Extra(NotificationService.EXTRA_IS_CONNECTED)boolean isConnected) {
            notificationServicePrefs.setChecked(isConnected);
            notificationServicePrefs.setSummary(isConnected ? R.string.pref_has_notification_access : R.string.pref_has_no_notification_access);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }
}
