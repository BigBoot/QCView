package de.bigboot.qcircleview.cover;

import android.app.Fragment;
import android.content.Context;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ProgressBar;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

import de.bigboot.qcircleview.Preferences;
import de.bigboot.qcircleview.R;
import de.bigboot.qcircleview.utils.AppsLoader;
import de.bigboot.qcircleview.utils.AppsLoader_;


@EFragment(R.layout.menu)
public class Menu extends Fragment implements AppsLoader.Callback {
    private Preferences prefs;
    private ArrayList<AppsLoader.AppEntry> items = new ArrayList<>();
    private AppsLoader appsLoader;


    @ViewById(R.id.viewpager)
    protected ViewPager viewPager;

    @Override
    public void onResume() {
        super.onResume();
        appsLoader.registerReceiver(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        appsLoader.unregisterReceiver(getActivity());
    }

    private Adapter adapter;

    @ViewById(R.id.loading)
    protected ProgressBar loading;

    @AfterViews
    public void init() {
        prefs = new Preferences(getActivity());
        adapter = new Adapter();
        viewPager.setAdapter(adapter);
        appsLoader = new AppsLoader_();
        appsLoader.callback = this;
        loadApps();
    }

    protected void loadApps() {
        final Context context = getActivity();
        appsLoader.loadApps(context);
        adapter.notifyDataSetChanged();
        loading.setVisibility(View.VISIBLE);
    }

    @Override
    @UiThread
    public void appsLoaded(List<AppsLoader.AppEntry> enabledApps, List<AppsLoader.AppEntry> disabledApps) {
        items.clear();
        items.addAll(enabledApps);
        try {
            adapter.notifyDataSetChanged();
        } catch (Exception ignore) {}
        loading.setVisibility(View.GONE);
    }

    private class Adapter extends FragmentStatePagerAdapter {

        public Adapter() {
            super(getChildFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            int start = position*9;
            int end = Math.min(start+9, items.size());
            AppsLoader.AppEntry[] entries = new AppsLoader.AppEntry[end-start];
            items.subList(start, end).toArray(entries);
            return LauncherPageFragment_.builder().items(entries).build();
        }

        @Override
        public int getCount() {
            return (items.size()-1)/9 + 1;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }
}
