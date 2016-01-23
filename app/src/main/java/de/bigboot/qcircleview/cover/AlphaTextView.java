package de.bigboot.qcircleview.cover;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by Marco Kirchner.
 */
public class AlphaTextView extends TextView {
    public AlphaTextView(Context context) {
        super(context);
    }

    public AlphaTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AlphaTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AlphaTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onSetAlpha(int alpha)
    {
        setTextColor(getTextColors().withAlpha(alpha));
        setHintTextColor(getHintTextColors().withAlpha(alpha));
        setLinkTextColor(getLinkTextColors().withAlpha(alpha));
        if(getBackground() != null)
            getBackground().setAlpha(alpha);
        return true;
    }
}
