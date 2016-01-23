package de.bigboot.qcircleview.cover;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.ViewsById;

import java.util.List;

import de.bigboot.qcircleview.Preferences;
import de.bigboot.qcircleview.R;
import de.bigboot.qcircleview.utils.AppsLoader;

@EFragment(R.layout.fragment_launcher_page)
public class LauncherPageFragment extends Fragment {
    @FragmentArg
    protected AppsLoader.AppEntry[] items;

    @ViewsById({R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6, R.id.item7, R.id.item8, R.id.item9})
    List<LinearLayout> itemViews;


    @AfterViews
    protected void init() {
        for (int i = 0; i < items.length; i++) {
            LinearLayout view = itemViews.get(i);
            view.setVisibility(View.VISIBLE);
            AppsLoader.AppEntry item = items[i];
            ImageView iconView = (ImageView) view.findViewById(R.id.icon);
            TextView titleView = (TextView) view.findViewById(R.id.title);
            iconView.setImageDrawable(item.loadIcon(getActivity()));
            titleView.setText(item.getName());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Preferences prefs = new Preferences(getActivity());
        boolean hideLabels = prefs.getBoolean(Preferences.BooleanSettings.HideLauncherLabels);

        for (int i = 0; i < items.length; i++) {
            LinearLayout view = itemViews.get(i);
            TextView titleView = (TextView) view.findViewById(R.id.title);
            if (hideLabels)
                titleView.setVisibility(View.INVISIBLE);
            else
                titleView.setVisibility(View.VISIBLE);
        }
    }

    @Click({R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6, R.id.item7, R.id.item8, R.id.item9})
    protected void onClick(View view) {
        int i = itemViews.indexOf(view);
        AppsLoader.AppEntry item = items[i];
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(item.getActivity()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
