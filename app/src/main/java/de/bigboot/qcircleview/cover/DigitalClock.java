package de.bigboot.qcircleview.cover;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextClock;

import org.androidannotations.annotations.EView;

/**
 * Created by Marco Kirchner.
 */
@EView
public class DigitalClock extends TextClock {
    public DigitalClock(Context context) {
        super(context);
        tick(true);
    }

    public DigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        tick(true);
    }

    public DigitalClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        tick(true);
    }

    public DigitalClock(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        tick(true);
    }

    protected void tick(final boolean tick) {
        final boolean current = (System.currentTimeMillis() / 1000) % 2 == 1;
        if (tick != current)
            if(tick)
                setFormat24Hour(getFormat24Hour().toString().replaceFirst(":", " "));
            else
                setFormat24Hour(getFormat24Hour().toString().replaceFirst(" ", ":"));

        postDelayed(new Runnable() {
            @Override
            public void run() {
                tick(current);
            }
        }, 200);
    }
}
