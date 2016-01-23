package de.bigboot.qcircleview.config;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toolbar;

import com.joanzapata.iconify.widget.IconButton;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.nhaarman.listviewanimations.itemmanipulation.dragdrop.TouchViewDraggableManager;
import com.nhaarman.listviewanimations.util.Swappable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.CheckedChange;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.bigboot.qcircleview.Preferences;
import de.bigboot.qcircleview.R;
import de.bigboot.qcircleview.utils.AppsLoader;
import de.bigboot.qcircleview.utils.AppsLoader_;

@EActivity(R.layout.activity_launcher_settings)
public class LauncherSettingsActivity extends Activity {
    @ViewById(R.id.toolbar)
    protected Toolbar toolbar;

    @FragmentById(R.id.content)
    protected LauncherSettingsFragment content;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_launcher_settings, menu);
        MenuItem item = menu.findItem(R.id.enabled);
        Switch enabledSwitch = (Switch) item.getActionView().findViewById(R.id.switchForActionBar);
        enabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                onEnabledChanged(b);
            }
        });
        Preferences preferences = new Preferences(this);
        enabledSwitch.setChecked(preferences.getBoolean(Preferences.BooleanSettings.EnableLauncher));
        return true;
    }

    @AfterViews
    protected void init() {
        setActionBar(toolbar);
        Preferences preferences = new Preferences(this);
        content.setEnabled(preferences.getBoolean(Preferences.BooleanSettings.EnableLauncher));
    }

    protected void onEnabledChanged(boolean b) {
        content.setEnabled(b);
    }

    @EFragment(R.layout.fragment_launcher_settings)
    public static class LauncherSettingsFragment extends Fragment {
        @ViewById(R.id.hide_labels)
        protected Switch hideLabelsSwitch;
        @ViewById(R.id.app_list)
        protected DynamicListView appListView;
        @ViewById(R.id.sort)
        protected IconButton sortButton;
        @ViewById(R.id.sort_label)
        protected TextView sortLabel;

        protected Adapter adapter;

        @AfterViews
        protected void init() {
            adapter = new Adapter(getActivity());
            appListView.setAdapter(adapter);
            appListView.enableDragAndDrop();
            appListView.setDraggableManager(new TouchViewDraggableManager(R.id.icon));
        }

        public void setEnabled(boolean enabled) {
            Preferences prefs = new Preferences(getActivity());
            prefs.putBoolean(Preferences.BooleanSettings.EnableLauncher, enabled);

            appListView.setEnabled(enabled);
            sortLabel.setEnabled(enabled);
            sortButton.setEnabled(enabled);
            hideLabelsSwitch.setEnabled(enabled);
        }

        @CheckedChange(R.id.hide_labels)
        protected void hideLabelsChanged() {
            Preferences prefs = new Preferences(getActivity());
            prefs.putBoolean(Preferences.BooleanSettings.HideLauncherLabels, hideLabelsSwitch.isChecked());
        }

        @Click(R.id.sort)
        protected void onSort() {
            adapter.sort(new Comparator<AppsLoader.AppEntry>() {
                @Override
                public int compare(AppsLoader.AppEntry appEntry, AppsLoader.AppEntry t1) {
                    return appEntry.getName().compareTo(t1.getName());
                }
            });
        }
    }

    protected static class Adapter extends BaseAdapter implements AppsLoader.Callback, Swappable {
        private static int HEADER = 0;
        private static int ITEM = 1;

        private final Context mContext;
        private AppsLoader appsLoader;
        private int headerIndex;
        private boolean loaded = false;
        private ArrayList<AppsLoader.AppEntry> items = new ArrayList<>();

        public Adapter(final Context context) {
            mContext = context;
            appsLoader = new AppsLoader_();
            appsLoader.callback = this;
            appsLoader.loadApps(context);
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            if (!loaded)
                return;
            Preferences preferences = new Preferences(mContext);
            ArrayList<String> enabledAppActivities = new ArrayList<String>(headerIndex);
            ArrayList<String> disabledAppActivities = new ArrayList<String>(items.size() - headerIndex);
            for (int i = 0; i < items.size(); i++) {
                if (i < headerIndex)
                    enabledAppActivities.add(items.get(i).getActivity());
                else
                    disabledAppActivities.add(items.get(i).getActivity());
            }
            preferences.setEnabledApps(enabledAppActivities);
            preferences.setDisabledApps(disabledAppActivities);
        }

        @Override
        public int getCount() {
            return items.size() + 1;
        }

        @Override
        public AppsLoader.AppEntry getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int position) {
            if (position == headerIndex)
                return "HEADER".hashCode();
            else
                if (position > headerIndex)
                    position--;
                return getItem(position).hashCode();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, final View convertView, final ViewGroup parent) {
            if(position == headerIndex) {
                View view = convertView;
                if (view == null) {
                    view = LayoutInflater.from(mContext).inflate(R.layout.launcher_settings_header, parent, false);
                }
                return view;
            } else {
                if (position > headerIndex) {
                    position--;
                }
                LinearLayout view = (LinearLayout) convertView;
                if (view == null) {
                    view = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.launcher_settings_item, parent, false);
                }

                TextView textView = (TextView) view.findViewById(R.id.text);
                textView.setText(getItem(position).getName());
                ImageView iconView = (ImageView) view.findViewById(R.id.icon);
                iconView.setImageDrawable(getItem(position).loadIcon(mContext));

                return view;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if(position == headerIndex) {
                return HEADER;
            } else {
                return ITEM;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public void appsLoaded(List<AppsLoader.AppEntry> enabledApps, List<AppsLoader.AppEntry> disabledApps) {
            clear();
            addAll(enabledApps);
            addAll(disabledApps);
            headerIndex = enabledApps.size();
            loaded = true;
            notifyDataSetChanged();
        }

        public void move(int from, int to) {
//            Collections.rotate(items.subList(from, to + 1), -1);
        }

        public boolean add(AppsLoader.AppEntry object) {
            boolean result = items.add(object);
            notifyDataSetChanged();
            return result;
        }

        public AppsLoader.AppEntry remove(int index) {
            AppsLoader.AppEntry result = items.remove(index);
            notifyDataSetChanged();
            return result;
        }

        public boolean remove(AppsLoader.AppEntry object) {
            boolean result = items.remove(object);
            notifyDataSetChanged();
            return result;
        }

        public AppsLoader.AppEntry get(int index) {
            return items.get(index);
        }

        public boolean addAll(int index, Collection<? extends AppsLoader.AppEntry> collection) {
            boolean result = items.addAll(index, collection);
            notifyDataSetChanged();
            return result;
        }

        public void clear() {
            items.clear();
            notifyDataSetChanged();
        }

        public AppsLoader.AppEntry set(int index, AppsLoader.AppEntry object) {
            AppsLoader.AppEntry result = items.set(index, object);
            notifyDataSetChanged();
            return result;
        }

        public void add(int index, AppsLoader.AppEntry object) {
            items.add(index, object);
            notifyDataSetChanged();
        }

        public boolean addAll(Collection<? extends AppsLoader.AppEntry> collection) {
            boolean result = items.addAll(collection);
            notifyDataSetChanged();
            return result;
        }

        public void sort(Comparator<AppsLoader.AppEntry> comparator) {
            List<AppsLoader.AppEntry> enabled = new ArrayList<>(items.subList(0, headerIndex));
            List<AppsLoader.AppEntry> disabled = new ArrayList<>(items.subList(headerIndex, items.size()));
            Collections.sort(enabled, comparator);
            Collections.sort(disabled, comparator);
            items.clear();
            items.addAll(enabled);
            items.addAll(disabled);
            notifyDataSetChanged();
        }

        @Override
        public void swapItems(int i, int j) {
            if (j == headerIndex) {
                headerIndex = i;
            } else if (i == headerIndex) {
                headerIndex = j;
            } else {
                if (i > headerIndex)
                    i--;
                if (j > headerIndex)
                    j--;
                Collections.swap(items, i, j);
            }
        }

//        @Override
//        public View getHeaderView(final int position, final View convertView, final ViewGroup parent) {
//            TextView view = (TextView) convertView;
//            if (view == null) {
//                view = (TextView) LayoutInflater.from(mContext).inflate(R.layout.list_header, parent, false);
//            }
//
//            view.setText(mContext.getString(R.string.header, getHeaderId(position)));
//
//            return view;
//        }
    }
}
