package de.bigboot.qcirclelib;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.support.annotation.LayoutRes;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

/**
 * Base activity to create QuickView apps
 *
 * Usage:
 * Create Activity extending QCircleActivity.
 * Don't call setContentView but create our view in {@link #onCreateView(de.bigboot.qcirclelib.QCircleActivity.CircleViewBuilder)}
 *
 */
public abstract class QCircleActivity extends Activity {
    /**
     * Screen timeout
     */
    private static final long SCREEN_TIMEOUT = 5000L;
    private View contentView = null;

    /**
     * Action used send commands to SmartCover service
     * Must at lease include a {@link #SMARTCOVER_EXTRA_COMMAND}
     */
    public static final String SMARTCOVER_ACTION_COMMAND = "de.bigboot.qcircleview.SmartCoverService.ACTION_COMMAND";
    /**
     * Action used, when Smartcover state changed.
     * Always includes {@link #SMARTCOVER_EXTRA_SCREEN_STATE} and {@link #SMARTCOVER_EXTRA_STATE_CHANGED}
     */
    public static final String SMARTCOVER_ACTION_COVER_STATE = "de.bigboot.qcircleview.SmartCoverService.ACTION_COVER_STATE";

    /**
     * String extra describing the command.
     * To be used with {@link #SMARTCOVER_ACTION_COMMAND}
     * @see #SMARTCOVER_COMMAND_DISABLE_AIRPLANE_MODE
     * @see #SMARTCOVER_COMMAND_ENABLE_AIRPLANE_MODE
     * @see #SMARTCOVER_COMMAND_POLL_STATE
     * @see #SMARTCOVER_COMMAND_ENABLE_AIRPLANE_MODE
     * @see #SMARTCOVER_COMMAND_DISABLE_AIRPLANE_MODE
     */
    public static final String SMARTCOVER_EXTRA_COMMAND = "de.bigboot.qcircleview.SmartCoverService.EXTRA_COMMAND";
    /**
     * String extra containing the current Smartcover state.
     * Can be "Open", "Closed" or "Unknown"
     * @see #SMARTCOVER_ACTION_COVER_STATE
     */
    public static final String SMARTCOVER_EXTRA_SCREEN_STATE = "de.bigboot.qcircleview.SmartCoverService.EXTRA_SCREEN_STATE";
    /**
     * boolean extra indicating whether smartcover status has changed or not
     * @see #SMARTCOVER_ACTION_COVER_STATE
     * @see #SMARTCOVER_COMMAND_POLL_STATE
     */
    public static final String SMARTCOVER_EXTRA_STATE_CHANGED = "de.bigboot.qcircleview.SmartCoverService.EXTRA_STATE_CHANGED";

    /**
     * Command to poll cover state. As a result the SmartCover service will Broadcast a {@link #SMARTCOVER_ACTION_COVER_STATE}
     * with a {@link #SMARTCOVER_EXTRA_STATE_CHANGED} of false
     */
    public static final String SMARTCOVER_COMMAND_POLL_STATE = "de.bigboot.qcircleview.SmartCoverService.COMMAND_POLL_STATE";

    @Override
    public View findViewById(@LayoutRes int id) {
        View view = null;
        if (contentView != null)
            view = contentView.findViewById(id);
        if (view == null) {
            view = super.findViewById(id);
        }
        return view;
    }

    /**
     * Command to turn the screen off
     * @see #SMARTCOVER_ACTION_COMMAND
     * @see #SMARTCOVER_EXTRA_COMMAND
     * @see #sendCommand(String)
     */
    public static final String SMARTCOVER_COMMAND_SCREEN_OFF = "de.bigboot.qcircleview.SmartCoverService.COMMAND_SCREEN_OFF";
    /**
     * Command to enable airplane mode
     * @see #SMARTCOVER_ACTION_COMMAND
     * @see #SMARTCOVER_EXTRA_COMMAND
     * @see #sendCommand(String)
     */
    public static final String SMARTCOVER_COMMAND_ENABLE_AIRPLANE_MODE = "de.bigboot.qcircleview.SmartCoverService.SMARTCOVER_COMMAND_ENABLE_AIRPLANE_MODE";
    /**
     * Command to disable airplane mode
     * @see #SMARTCOVER_ACTION_COMMAND
     * @see #SMARTCOVER_EXTRA_COMMAND
     * @see #sendCommand(String)
     */
    public static final String SMARTCOVER_COMMAND_DISABLE_AIRPLANE_MODE = "de.bigboot.qcircleview.SmartCoverService.SMARTCOVER_COMMAND_DISABLE_AIRPLANE_MODE";

    private GestureDetector tapGestureDetector;
    private CoverBroadcastReceiver coverBroadcastReceiver;
    private ScreenBroadcastReceiver screenBroadcastReceiver;
    private Handler handler;
    private ScreenOffTimer screenOffTimer = new ScreenOffTimer();
    private SmartcoverPollTimer smartcoverPollTimer = new SmartcoverPollTimer();
    private long screenTimeout = SCREEN_TIMEOUT;
    private boolean dt2sleep = true;

    private boolean allowSetContent = false;

    /**
     * Default constructor
     */
    public QCircleActivity() {
        coverBroadcastReceiver = new CoverBroadcastReceiver();
        screenBroadcastReceiver = new ScreenBroadcastReceiver();
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Don't use. Create your view in {@link #onCreateView(de.bigboot.qcirclelib.QCircleActivity.CircleViewBuilder)}
     */
    @Override
    @Deprecated
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if(view != null && !allowSetContent) {
            throw new UnsupportedOperationException("QCircleActivity subclass aren't supposed to call setContentView(), but instead should create their view in onCreateView()");
        }
        super.setContentView(view, params);
        contentView = super.findViewById(R.id.qc_content);
    }

    /**
     * Don't use. Create your view in {@link #onCreateView(de.bigboot.qcirclelib.QCircleActivity.CircleViewBuilder)}
     */
    @Override
    @Deprecated
    public void setContentView(View view) {
        if(view != null && !allowSetContent) {
            throw new UnsupportedOperationException("QCircleActivity subclass aren't supposed to call setContentView(), but instead should create their view in onCreateView()");
        }
        super.setContentView(view);
        contentView = super.findViewById(R.id.qc_content);
    }

    /**
     * Don't use. Create your view in {@link #onCreateView(de.bigboot.qcirclelib.QCircleActivity.CircleViewBuilder)}
     */
    @Override
    @Deprecated
    public void setContentView(int layoutResID) {
        if (!allowSetContent) {
            throw new UnsupportedOperationException("QCircleActivity subclass aren't supposed to call setContentView(), but instead should create their view in onCreateView()");
        }
        super.setContentView(layoutResID);
        contentView = super.findViewById(R.id.qc_content);
    }

    private void createView() {
        CircleView view = onCreateView(new CircleViewBuilder(getLayoutInflater()));
        View mask = view.getView();

        allowSetContent = true;
        setContentView(mask);
        allowSetContent = false;
        afterView();
    }

    /**
     * Called after the view has been setup
     */
    protected void afterView() {

    }

    /**
     * Create your view and build it using {@code builder}
     * @param builder the builder used to contruct the view
     * @return result of {@link de.bigboot.qcirclelib.QCircleActivity.CircleViewBuilder}.build()
     */
    protected abstract CircleView onCreateView(CircleViewBuilder builder);

    /**
     * Called when the back back button is pressed
     * @return true to finish activity, false otherwise
     */
    protected boolean onBackButtonPressed() {
        return true;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        tapGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                screenOff();
                return false;
            }
        });
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        );
        createView();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(dt2sleep)
            tapGestureDetector.onTouchEvent(ev);
        handler.removeCallbacks(screenOffTimer);
        if(screenTimeout>0) {
            handler.postDelayed(screenOffTimer, SCREEN_TIMEOUT);
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Method to override default behaviour of going to sleep on double tap
     * @param enabled
     */
    protected void setDoubleTap2SleepEnabled(boolean enabled) {
        this.dt2sleep = enabled;
    }

    /**
     * Set the screen timeout
     * Set to 0 to disable screen timeout
     * Set to a negative value to restore default timeout
     * @param timeout timeout in milliseconds.
     */
    protected void setScreenTimeout(long timeout ) {
        this.screenTimeout = timeout;
        handler.removeCallbacks(screenOffTimer);
        if (screenTimeout < 0) {
            screenTimeout = SCREEN_TIMEOUT;
        }else if(screenTimeout > 0) {
            handler.postDelayed(screenOffTimer, screenTimeout);
        }
    }

    /**
     * Called when the cover is opened, default implementation finishes the activity
     */
    protected void onCoverOpened() {
        this.finish();
    }

    /**
     * Convenience method to send {@link #SMARTCOVER_COMMAND_SCREEN_OFF} to SmartCover service
     */
    protected void screenOff() {
        sendCommand(SMARTCOVER_COMMAND_SCREEN_OFF);
    }

    /**
     * Convenience method to send a command to SmartCover service
     * @param Command Command to send
     * @see #SMARTCOVER_COMMAND_DISABLE_AIRPLANE_MODE
     * @see #SMARTCOVER_COMMAND_ENABLE_AIRPLANE_MODE
     * @see #SMARTCOVER_COMMAND_POLL_STATE
     * @see #SMARTCOVER_COMMAND_ENABLE_AIRPLANE_MODE
     * @see #SMARTCOVER_COMMAND_DISABLE_AIRPLANE_MODE
     */
    protected void sendCommand(String Command) {
        sendBroadcast(new Intent(SMARTCOVER_ACTION_COMMAND).putExtra(SMARTCOVER_EXTRA_COMMAND, Command));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(coverBroadcastReceiver, new IntentFilter(SMARTCOVER_ACTION_COVER_STATE));
        registerReceiver(screenBroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        handler.postDelayed(screenOffTimer, SCREEN_TIMEOUT);
        handler.post(smartcoverPollTimer);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(coverBroadcastReceiver);
        unregisterReceiver(screenBroadcastReceiver);
        handler.removeCallbacks(screenOffTimer);
        handler.removeCallbacks(smartcoverPollTimer);
    }

    /**
     * Convenience method to get circle diameter
     * @return circle diameter in pixel
     */
    protected int getCircleDiameter() {
        return getResources().getDimensionPixelSize(R.dimen.config_circle_diameter);
    }

    /**
     * Convenience method to get circle window height
     * @return circle window height in pixel
     */
    protected int getCircleWindowHeight() {
        return getResources().getDimensionPixelSize(R.dimen.config_circle_window_height);
    }

    /**
     * Convenience method to get circle window x offset
     * @return the x offset of the circle window in relation to the left of the screen in pixel
     */
    protected int getCircleWindowXPos() {
        return getResources().getDimensionPixelSize(R.dimen.config_circle_window_x_pos);
    }

    /**
     * Convenience method to get circle window y offset
     * @return the y offset of the circle window in relation to the top of the screen in pixel
     */
    protected int getCircleWindowYPos() {
        return getResources().getDimensionPixelSize(R.dimen.config_circle_window_y_pos);
    }

    private class CoverBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(SMARTCOVER_EXTRA_SCREEN_STATE);
            if("Open".equals(state)) {
                QCircleActivity.this.onCoverOpened();
            }
        }
    }

    private class ScreenBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.post(smartcoverPollTimer);
        }
    }

    private class ScreenOffTimer implements Runnable {
        @Override
        public void run() {
            sendCommand(SMARTCOVER_COMMAND_SCREEN_OFF);
        }
    }

    private class SmartcoverPollTimer implements Runnable {
        @Override
        public void run() {
            sendCommand(SMARTCOVER_COMMAND_POLL_STATE);
            if (ScreenUtils.isScreenOn(QCircleActivity.this))
                handler.postDelayed(this, 100);
        }
    }

    /**
     * Used to build the View
     * @see #onCreateView(de.bigboot.qcirclelib.QCircleActivity.CircleViewBuilder)
     */
    public class CircleViewBuilder {
        private LayoutInflater inflater;
        private boolean hasBuilt = false;
        private boolean useMask = true;
        private boolean showReturnButton = true;
        private boolean expandLayoutBelowReturnButton = false;

        private ViewGroup content = null;

        private CircleViewBuilder(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        private void ensureNotBuild() {
            if(hasBuilt)
                throw new IllegalStateException("It's not allowed to edit ViewBuilder settings after build");
        }

        /**
         * Wether the view will use the complete screen space, or only the visible circle.
         * @return true will limit the view to the visible circle.
         *         false will allow full screen usage.
         */
        public boolean useMask() {
            return useMask;
        }

        /**
         * @param useMask Setter for {@link #useMask()}
         */
        public void setUseMask(boolean useMask) {
            ensureNotBuild();
            this.useMask = useMask;
        }

        /**
         * Wether or not to show a default return button.
         * @return true if a default return button will be shown, false otherwise.
         */
        public boolean showReturnButton() {
            return showReturnButton;
        }

        /**
         * @param showReturnButton Setter for {@link #showReturnButton()}
         */
        public void setShowReturnButton(boolean showReturnButton) {
            ensureNotBuild();
            this.showReturnButton = showReturnButton;
        }

        /**
         * Wether or net the content layout can expand under the return button
         * @return true if layout can expand under return button, false otherwise
         */
        public boolean canExpandLayoutBelowReturnButton() {
            return expandLayoutBelowReturnButton;
        }

        /**
         *
         * @param expandLayoutBelowReturnButton Setter for {@link #canExpandLayoutBelowReturnButton()}
         */
        public void setExpandLayoutBelowReturnButton(boolean expandLayoutBelowReturnButton) {
            this.expandLayoutBelowReturnButton = expandLayoutBelowReturnButton;
        }

        private View buildMask() {
            if(!useMask) {
                return null;
            }
            View mask = inflater.inflate(R.layout.mask, null);
            content = (FrameLayout) mask.findViewById(R.id.qc_content);
            if (showReturnButton()) {
                View back = mask.findViewById(R.id.qc_back_button);
                back.setVisibility(View.VISIBLE);
                if (expandLayoutBelowReturnButton) {
                    RelativeLayout.LayoutParams contentLayoutParams = (RelativeLayout.LayoutParams)  content.getLayoutParams();
                    contentLayoutParams.removeRule(RelativeLayout.ABOVE);
                    contentLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                }
                back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(onBackButtonPressed()) {
                            finish();
                        }
                    }
                });
            }
            return mask;
        }

        /**
         * Builds the {@link de.bigboot.qcirclelib.QCircleActivity.CircleView} using the defined settings
         * @param layout resource ID for the layout to be used
         * @return {@link de.bigboot.qcirclelib.QCircleActivity.CircleView} to be returned in {@link #onCreateView(de.bigboot.qcirclelib.QCircleActivity.CircleViewBuilder)}
         */
        public CircleView build(@LayoutRes int layout) {
            View mask = buildMask();
            View view = inflater.inflate(layout, content);
            return new CircleView(useMask?mask:view, view);
        }


        /**
         * Builds the {@link de.bigboot.qcirclelib.QCircleActivity.CircleView} using the defined settings
         * @param layout the view to be used as the layout
         * @return {@link de.bigboot.qcirclelib.QCircleActivity.CircleView} to be returned in {@link #onCreateView(de.bigboot.qcirclelib.QCircleActivity.CircleViewBuilder)}
         */
        public CircleView build(View layout) {
            View mask = buildMask();
            if (useMask) {
                content.addView(layout);
                return new CircleView(mask, layout);
            } else {
                return new CircleView(layout, layout);
            }
        }


        /**
         * Builds the {@link de.bigboot.qcirclelib.QCircleActivity.CircleView} using the defined settings
         * @param layout the view to be used as the layout
         * @param layoutParams the layout parameters to set on the child
         * @return {@link de.bigboot.qcirclelib.QCircleActivity.CircleView} to be returned in {@link #onCreateView(de.bigboot.qcirclelib.QCircleActivity.CircleViewBuilder)}
         */
        public CircleView build(View layout, ViewGroup.LayoutParams layoutParams) {
            View mask = buildMask();
            if (useMask) {
                content.addView(layout, layoutParams);
                return new CircleView(mask, layout);
            } else {
                return new CircleView(layout, layout);
            }
        }
        /**
         * Builds the {@link de.bigboot.qcirclelib.QCircleActivity.CircleView} using the defined settings
         * @param layout the view to be used as the layout
         * @return {@link de.bigboot.qcirclelib.QCircleActivity.CircleView} to be returned in {@link #onCreateView(de.bigboot.qcirclelib.QCircleActivity.CircleViewBuilder)}
         */
        public CircleView build(View layout, int width, int height) {
            View mask = buildMask();
            if (useMask) {
                content.addView(layout, width, height);
                return new CircleView(mask, layout);
            } else {
                return new CircleView(layout, layout);
            }
        }
    }

    public static class CircleView {
        private View view;
        private View contentView;

        private CircleView(View view, View contentView) {
            this.view = view;
            this.contentView = contentView;
        }

        protected View getView() {
            return view;
        }
        public View getContentView() { return contentView; }
    }

}
