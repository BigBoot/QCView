package de.bigboot.qcircleview.cover;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.androidannotations.annotations.EView;

import de.bigboot.qcircleview.R;

/**
 * Created by Marco Kirchner.
 */
@EView
public class KnockView extends View implements ValueAnimator.AnimatorUpdateListener {
    private Paint paint;
    private Paint outlinePaint;
    private Paint blinkPaint;
    private Paint disabledPaint;
    private int color;
    private int selectionColor;
    private int blinkColor;
    private ValueAnimator animators[];
    private ValueAnimator blinkAnimator;
    private float angle;
    private int[] knockCode;
    private int currentKnockPosition = 0;
    private UnlockListener unlockListener = null;
    private ClickListener clickListener = null;

    public int[] getKnockCode() {
        return knockCode;
    }

    public void setKnockCode(int[] knockCode) {
        this.knockCode = knockCode;
        currentKnockPosition = 0;
    }

    public void setUnlockListener(UnlockListener listener) {
        this.unlockListener = listener;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isInTouchMode() || !isEnabled())
            return false;
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x -= getWidth()/2;
                y -= getHeight()/2;
                float angle = (float) Math.toDegrees(Math.atan2(x, y)) + 180;
                int sector = animators.length - 1 - (int) (angle / this.angle);
                ValueAnimator animator = animators[sector];
                animator.setIntValues(color, selectionColor);
                animator.start();
                if (clickListener != null) {
                    clickListener.onClick(sector);
                }
                if (knockCode != null && knockCode.length > 0 && sector == knockCode[currentKnockPosition]) {
                    currentKnockPosition++;
                    if (currentKnockPosition == knockCode.length) {
                        currentKnockPosition = 0;
                        blinkAnimator.start();
                    }
                } else {
                    currentKnockPosition = 0;
                }
        }
        return true;
    }

    public KnockView(Context context, AttributeSet attrs) {
        super(context, attrs);

        color = Color.WHITE;
        selectionColor = Color.BLUE;
        int borderColor = Color.LTGRAY;
        blinkColor = Color.WHITE;
        int sectors = 4;
        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KnockView);
            color = a.getColor(R.styleable.KnockView_kv_background_color, color);
            selectionColor = a.getColor(R.styleable.KnockView_kv_selection_color, selectionColor);
            borderColor = a.getColor(R.styleable.KnockView_kv_border_color, borderColor);
            blinkColor = a.getColor(R.styleable.KnockView_kv_blink_color, blinkColor);
            sectors = a.getInt(R.styleable.KnockView_kv_sectors, sectors);
            a.recycle();
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);

        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setColor(borderColor);

        blinkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blinkPaint.setStyle(Paint.Style.FILL);
        blinkPaint.setColor(Color.TRANSPARENT);
        blinkAnimator = ObjectAnimator.ofArgb(blinkPaint, "color", Color.TRANSPARENT, blinkColor, Color.TRANSPARENT);
        blinkAnimator.setDuration(500);
        blinkAnimator.addUpdateListener(this);
        blinkAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (unlockListener != null) {
                    unlockListener.onUnlock();
                }
            }
        });

        disabledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        disabledPaint.setStyle(Paint.Style.FILL);
        disabledPaint.setColor(Color.parseColor("#10000000"));


        long animation_duration = 200L;

        animators = new ValueAnimator[sectors];

        for(int i = 0; i < sectors; i++) {
            ValueAnimator animator = ValueAnimator.ofArgb(color);
            animator.setDuration(animation_duration);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(1);
            animator.addUpdateListener(this);
            animators[i] = animator;
        }

        angle = 360f/sectors;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.rotate(-90, getWidth()/2, getHeight()/2);
        for (int i = 0; i < animators.length; i++) {
            int c = (int) animators[i].getAnimatedValue();
            if (c == 0)
                c = color;
            paint.setColor(c);
            canvas.drawArc(0, 0, getWidth(), getHeight(), i * angle, angle, true, paint);
            canvas.drawArc(0, 0, getWidth(), getHeight(), i * angle, angle, true, outlinePaint);
        }
        canvas.restore();
        canvas.drawArc(0, 0, getWidth(), getHeight(), 0, 360, true, blinkPaint);
        if(!isEnabled()) {
            canvas.drawArc(0, 0, getWidth(), getHeight(), 0, 360, true, disabledPaint);
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        invalidate();
    }

    public static interface UnlockListener {
        public void onUnlock();
    }

    public static interface ClickListener {
        public void onClick(int sector);
    }
}
