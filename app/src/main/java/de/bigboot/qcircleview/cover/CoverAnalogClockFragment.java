package de.bigboot.qcircleview.cover;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.ViewById;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import de.bigboot.qcircleview.R;
import de.bigboot.qcircleview.config.Clock;

/*
 * Created by Marco Kirchner.
 */
@EFragment(R.layout.clock_analog)
public class CoverAnalogClockFragment extends Fragment {
    @FragmentArg
    protected Clock clock;

    @ViewById(R.id.content)
    protected View content;
    @ViewById(R.id.analogClock)
    protected AnalogClock analogClockView;
    @ViewById(R.id.background)
    protected ImageView backgroundView;
    @ViewById(R.id.date_text)
    protected TextView dateTextView;
    @ViewById(R.id.date_background)
    protected ImageView dateBackgroundView;

    private SimpleDateFormat dateFormat = null;

    @AfterViews
    protected void initView() {
        if (analogClockView != null) {
            backgroundView.setImageDrawable(clock.getBackgroundDrawable(getActivity()));
            analogClockView.setHourHand(clock.getHourDrawable(getActivity()));
            analogClockView.setMinuteHand(clock.getMinuteDrawable(getActivity()));
            analogClockView.setSecondHand(clock.getSecondDrawable(getActivity()));
        }

        if(clock.getOptions().containsKey("date-display")) {
            ((View)dateTextView.getParent()).setVisibility(View.VISIBLE);
            HashMap<String, String> dateDisplay = clock.getOptions().get("date-display");
            dateFormat = new SimpleDateFormat(dateDisplay.containsKey("format") ? dateDisplay.get("format") : "EEE d");


            float x = dateDisplay.containsKey("x") ? Float.parseFloat(dateDisplay.get("x")) : 0f;
            float y = dateDisplay.containsKey("y") ? Float.parseFloat(dateDisplay.get("y")) : 0f;
            float width = getResources().getDimension(R.dimen.config_circle_diameter);
            float height = width;
            float imageWidth = 0;
            float imageHeight = 0;

            Drawable dayBg = clock.getDayBackgroundDrawable(getActivity());
            if (dayBg != null) {
                imageWidth = dayBg.getIntrinsicWidth();
                imageHeight = dayBg.getIntrinsicHeight();

                dateBackgroundView.setImageDrawable(dayBg);

                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) dateBackgroundView.getLayoutParams();
                layoutParams.setMargins((int) x, (int) y, 0, 0);
            }


            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) dateTextView.getLayoutParams();
            int dx = (int) (x - width/2 + imageWidth/2);
            int dy = (int) (y - height/2 + imageHeight/2);
            layoutParams.setMargins(dx, dy, -dx, -dy);

            int textSize = dateDisplay.containsKey("text-size") ? Integer.parseInt(dateDisplay.get("text-size")) : 44;
            dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            int textColor = dateDisplay.containsKey("text-color") ? Color.parseColor(dateDisplay.get("text-color")) : Color.BLACK;
            dateTextView.setTextColor(textColor);



            if(dateDisplay.containsKey("text-font")) {
                Typeface font = Typeface.createFromFile(clock.getFile(getActivity(), dateDisplay.get("text-font")));
                dateTextView.setTypeface(font);
            }

            onDateChanged();
        }
    }

    @Receiver(actions = Intent.ACTION_DATE_CHANGED, registerAt = Receiver.RegisterAt.OnAttachOnDetach)
    public void onDateChanged() {
        if (dateTextView != null && dateFormat != null) {
            String timeStamp = dateFormat.format(new Date());
            dateTextView.setText(timeStamp);
        }
    }
}
