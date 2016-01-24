package de.bigboot.qcircleview.cover;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import org.androidannotations.annotations.EView;

/**
 * Created by Marco Kirchner.
 */
@EView
public class CircleAnimationView extends View {
    private static final int CIRCLE_WIDTH = 32;
    private static final long ANIMATION_DURATION = 1000L;
    private static final long ANIMATION_DELAY = 500L;
    private static final int GLOW_SIZE = 256;

    private float progress;
    private Rect animationClip = new Rect();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Interpolator interpolator = new AccelerateDecelerateInterpolator();
    private ObjectAnimator objectAnimator;

    public CircleAnimationView(Context context) {
        super(context);
        init();
    }

    public CircleAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CircleAnimationView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    protected void setProgress(float progress) {
        this.progress = progress;
        calucateAnimation(getWidth(), getHeight());
        postInvalidate();
    }



    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calucateAnimation(w, h);
        postInvalidate();
    }

    private void calucateAnimation(int w, int h) {
        h*=1.05f;
        w*=1.05f;
        animationClip.set(0, (int) (h *1.05f*progress-GLOW_SIZE), w, (int) (h*progress+GLOW_SIZE));
        paint.setShader(new LinearGradient(0, progress*h, 0, progress*h+GLOW_SIZE, Color.WHITE, Color.TRANSPARENT, Shader.TileMode.MIRROR));
    }

    protected void init() {
        progress = 0f;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(CIRCLE_WIDTH);
        paint.setColor(Color.WHITE);

        calucateAnimation(getWidth(), getHeight());

        objectAnimator = ObjectAnimator.ofFloat(this, "progress", 0f, 1f);
        objectAnimator.setInterpolator(interpolator);
        objectAnimator.setStartDelay(ANIMATION_DELAY);
        objectAnimator.setDuration(ANIMATION_DURATION);

        startAnimation();
    }

    public void startAnimation() {
        objectAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (progress == 0 || progress == 1)
            return;

        int widht = (int) (getWidth() * 1.05f);
        int height = (int) (getHeight() * 1.05f);
        float radius = Math.min(widht, height)/2 - CIRCLE_WIDTH /2;

        canvas.clipRect(animationClip);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius, paint);
    }
}
