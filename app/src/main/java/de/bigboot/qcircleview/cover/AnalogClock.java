package de.bigboot.qcircleview.cover;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RemoteViews.RemoteView;

import java.util.TimeZone;

import de.bigboot.qcircleview.R;

/**
 * This widget display an analogic clock with two hands for hours and
 * minutes.
 */
@RemoteView
public class AnalogClock extends View {
    private static final BitmapDrawable PLACEHOLDER_DRAWABLE = new BitmapDrawable(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
    public AnalogClock(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    private Time mCalendar;

    private Drawable mHourHand;
    private Drawable mMinuteHand;
    private Drawable mSecondHand;
    private Drawable mDial;

    private int mDialWidth;
    private int mDialHeight;

    private boolean mAttached;

    private final Handler mHandler = new Handler();
    private float mMinutes;
    private float mHour;
    private boolean mChanged;


    Context mContext;

    public AnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClock(Context context, AttributeSet attrs,
                       int defStyle) {
        super(context, attrs, defStyle);
        Resources r = context.getResources();
        TypedArray a =
                context.obtainStyledAttributes(
                        attrs, R.styleable.AnalogClock, defStyle, 0);
        mContext = context;
        mDial = a.getDrawable(R.styleable.AnalogClock_dial);
        if (mDial == null) {
            mDial = PLACEHOLDER_DRAWABLE;
        }

        mHourHand = a.getDrawable(R.styleable.AnalogClock_hand_hour);
        if (mHourHand == null) {
            mHourHand = PLACEHOLDER_DRAWABLE;
        }

        mMinuteHand = a.getDrawable(R.styleable.AnalogClock_hand_minute);
        if (mMinuteHand == null) {
            mMinuteHand = PLACEHOLDER_DRAWABLE;
        }

        mSecondHand = a.getDrawable(R.styleable.AnalogClock_hand_second);
        if (mSecondHand == null) {
            mSecondHand = PLACEHOLDER_DRAWABLE;
        }

        a.recycle();

        mCalendar = new Time();

        mDialWidth = mDial.getIntrinsicWidth();
        mDialHeight = mDial.getIntrinsicHeight();
    }

    public void setHourHand(Drawable mHourHand) {
        this.mHourHand = mHourHand;
    }

    public void setMinuteHand(Drawable mMinuteHand) {
        this.mMinuteHand = mMinuteHand;
    }

    public void setSecondHand(Drawable mSecondHand) {
        this.mSecondHand = mSecondHand;
    }

    public void setDial(Drawable mDial) {
        this.mDial = mDial;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            getContext().registerReceiver(mIntentReceiver, filter, null, mHandler);
        }

        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = new Time();

        // Make sure we update to the current time
        onTimeChanged();
        counter.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            counter.cancel();
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
            hScale = (float) widthSize / (float) mDialWidth;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
            vScale = (float) heightSize / (float) mDialHeight;
        }

        float scale = Math.min(hScale, vScale);

        setMeasuredDimension(resolveSize((int) (mDialWidth * scale), widthMeasureSpec),
                resolveSize((int) (mDialHeight * scale), heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mChanged = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean changed = mChanged;
        if (changed) {
            mChanged = false;
        }
        boolean seconds = mSeconds;
        if (seconds) {
            mSeconds = false;
        }
        int availableWidth = getWidth();
        int availableHeight = getHeight();

        int x = availableWidth / 2;
        int y = availableHeight / 2;

        int h = availableHeight;
        int w = availableWidth;
        boolean scaled = false;

        if (mDial != null) {
            final Drawable dial = mDial;
            w = dial.getIntrinsicWidth();
            h = dial.getIntrinsicHeight();

            if (availableWidth < w || availableHeight < h) {
                scaled = true;
                float scale = Math.min((float) availableWidth / (float) w,
                        (float) availableHeight / (float) h);
                canvas.save();
                canvas.scale(scale, scale, x, y);
            }

            if (changed) {
                dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
            }
            dial.draw(canvas);
        }

        if (mHourHand != null) {
            canvas.save();
            canvas.rotate(mHour / 12.0f * 360.0f, x, y);
            final Drawable hourHand = mHourHand;
            if (changed) {
                w = hourHand.getIntrinsicWidth();
                h = hourHand.getIntrinsicHeight();
                hourHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
            }
            hourHand.draw(canvas);
            canvas.restore();
        }

        if (mMinuteHand != null) {
            canvas.save();
            canvas.rotate(mMinutes / 60.0f * 360.0f, x, y);
            //canvas.rotate(mSecond, x, y);
            final Drawable minuteHand = mMinuteHand;
            if (changed) {
                w = minuteHand.getIntrinsicWidth();
                h = minuteHand.getIntrinsicHeight();
                minuteHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
            }
            minuteHand.draw(canvas);
            canvas.restore();
        }

        if (mSecondHand != null) {
            canvas.save();
            canvas.rotate(mSecond, x, y);

            //minuteHand = mMinuteHand;
            if (seconds) {
                w = mSecondHand.getIntrinsicWidth();
                h = mSecondHand.getIntrinsicHeight();
                mSecondHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
            }
            mSecondHand.draw(canvas);
            canvas.restore();
        }
        if (scaled) {
            canvas.restore();
        }
    }

    MyCount counter = new MyCount(10000, 1000);

    public class MyCount extends CountDownTimer {
        public MyCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            counter.start();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mCalendar.setToNow();

            int hour = mCalendar.hour;
            int minute = mCalendar.minute;
            int second = mCalendar.second;

            mSecond = 6.0f * second;
            mSeconds = true;
            //mChanged = true;
            AnalogClock.this.invalidate();
            //Toast.makeText(mContext, "text", Toast.LENGTH_LONG).show();
        }
    }

    boolean mSeconds = false;
    float mSecond = 0;

    private void onTimeChanged() {
        mCalendar.setToNow();

        int hour = mCalendar.hour;
        int minute = mCalendar.minute;
        int second = mCalendar.second;

        mMinutes = minute + second / 60.0f;
        mHour = hour + mMinutes / 60.0f;
        mChanged = true;
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
            }

            onTimeChanged();

            invalidate();
        }
    };
}