package de.bigboot.qcircleview.cover;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.ViewById;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.bigboot.qcircleview.R;
import de.bigboot.qcircleview.config.Clock;

/*
 * Created by Marco Kirchner.
 */
@EFragment(R.layout.clock_digital)
public class CoverDigitalClockFragment extends Fragment {
    @ViewById(R.id.date)
    protected TextView dateView;
    @ViewById(R.id.content)
    protected View content;


    @AfterViews
    protected void initView() {
        onDateChanged();
        if ( content != null) {
            Drawable backgroundDrawable = Clock.DIGITAL_CLOCK.getBackgroundDrawable(getActivity());
            if(backgroundDrawable == null) {
                backgroundDrawable = getResources().getDrawable(R.drawable.digital_clock_default_bg);
            }
            content.setBackground(backgroundDrawable);
        }
    }

    @Receiver(actions = Intent.ACTION_DATE_CHANGED, registerAt = Receiver.RegisterAt.OnAttachOnDetach)
    public void onDateChanged() {
        if (dateView != null) {
            String timeStamp = SimpleDateFormat.getDateInstance().format(new Date());
            dateView.setText(timeStamp);
        }
    }
}
