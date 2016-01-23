package de.bigboot.qcircleview.cover;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;

import de.bigboot.qcircleview.Preferences;
import de.bigboot.qcircleview.R;
import de.bigboot.qcircleview.RootTools;
import de.bigboot.qcircleview.SmartcoverService;
import de.bigboot.qcircleview.utils.CallManager;

import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

/**
 * Created by Marco Kirchner.
 */
public class CallHandler implements CallManager.CallListener {

    private Context context;
    private View view;
    WindowManager windowManager;
    WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT | TYPE_SYSTEM_OVERLAY,
            FLAG_NOT_TOUCH_MODAL | FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSPARENT);

    TextView callerNameView;
    TextView callerNumberView;
    ImageView callerPhotoView;
    CircleButton answerCallView;
    TextView answerCallTextView;
    CircleButton declineCallView;
    TextView declineCallTextView;
    CircleButton hangUpCallView;
    TextView hangUpCallTextView;
    TextView incomingCallTextView;
    TextView textView;
    private String state = "";
    private boolean inCall = false;
    private CoverReceiver coverReceiver;

    public CallHandler(Context context) {
        this.context = context;

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.format = PixelFormat.TRANSLUCENT;

        layoutParams.gravity = Gravity.TOP;

        LayoutInflater inflater = LayoutInflater.from(context);
        this.view = inflater.inflate(R.layout.incoming_page, null);

        callerNameView = (TextView) view.findViewById(R.id.caller_name);
        callerNumberView = (TextView) view.findViewById(R.id.caller_number);
        callerPhotoView = (ImageView) view.findViewById(R.id.caller_photo);
        answerCallView = (CircleButton) view.findViewById(R.id.answer_call);
        answerCallTextView = (TextView) view.findViewById(R.id.answer_text);
        declineCallView = (CircleButton) view.findViewById(R.id.decline_call);
        declineCallTextView = (TextView) view.findViewById(R.id.decline_text);
        hangUpCallView = (CircleButton) view.findViewById(R.id.hang_up_call);
        hangUpCallTextView = (TextView) view.findViewById(R.id.hang_up_text);
        incomingCallTextView = (TextView) view.findViewById(R.id.incoming_call);
        textView = (TextView) view.findViewById(R.id.text);

        answerCallView.setListener(new CircleButton.Listener() {
            @Override
            public void onTrigger(CircleButton button) {
                onAnswer();
            }
        });
        declineCallView.setListener(new CircleButton.Listener() {
            @Override
            public void onTrigger(CircleButton button) {
                onDecline();
            }
        });
        hangUpCallView.setListener(new CircleButton.Listener() {
            @Override
            public void onTrigger(CircleButton button) {
                onDecline();
            }
        });

        IntentFilter filter = new IntentFilter(SmartcoverService.ACTION_COVER_STATE);
        coverReceiver = new CoverReceiver();
        context.registerReceiver(coverReceiver, filter);
    }

    public void onDestroy() {
        context.unregisterReceiver(coverReceiver);
    }

    public void loadCallerData(String callerNumber) {
        String callerName = CallManager.getContactName(context, callerNumber);
        Uri callerPhoto = CallManager.getContactPhoto(context, callerNumber);
        setCallerData(callerName, callerPhoto);
    }

    protected void setCallerData(String callerName, Uri callerPhoto) {
        if (callerName != null) {
            callerNameView.setText(callerName);
        }
        if (callerPhoto != null) {
            callerPhotoView.setImageURI(callerPhoto);
        }
    }

    private void showInCallUi() {
        answerCallView.setVisibility(View.INVISIBLE);
        answerCallTextView.setVisibility(View.INVISIBLE);
        declineCallView.setVisibility(View.INVISIBLE);
        declineCallTextView.setVisibility(View.INVISIBLE);
        hangUpCallView.setVisibility(View.VISIBLE);
        hangUpCallTextView.setVisibility(View.VISIBLE);
        textView.setText("00:00:00");
    }

    private void hideInCallUi() {
        answerCallView.setVisibility(View.VISIBLE);
        answerCallTextView.setVisibility(View.VISIBLE);
        declineCallView.setVisibility(View.VISIBLE);
        declineCallTextView.setVisibility(View.VISIBLE);
        hangUpCallView.setVisibility(View.INVISIBLE);
        hangUpCallTextView.setVisibility(View.INVISIBLE);
        textView.setText(R.string.call_swipe_icon);
    }

    private void showView() {
        if ("Closed".equals(state)) {
            try {
                windowManager.addView(view, layoutParams);
                inCall = true;
            } catch (Exception ignore) {
            }
        }
    }

    private void hideView() {
        try {
            windowManager.removeView(view);
            inCall = false;
        } catch (Exception ignore) {
        }
    }

    public boolean isInCall() {
        return inCall;
    }

    private void onAnswer() {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(100);

        Preferences prefs = new Preferences(context);
        if (prefs.getBoolean(Preferences.BooleanSettings.AlternativeCall))
            RootTools.acceptCallHeadsetEvent();
        else
            RootTools.sendPhoneCommand(RootTools.PHONE_COMMAND_ANSWER_CALL);
        showInCallUi();
    }

    private void onDecline() {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(100);

        Preferences prefs = new Preferences(context);
        if (prefs.getBoolean(Preferences.BooleanSettings.AlternativeCall))
            RootTools.dismissCallHeadsetEvent();
        else
            RootTools.sendPhoneCommand(RootTools.PHONE_COMMAND_END_CALL);
        hideInCallUi();
        hideView();
    }

    private void initCall(String number) {
        callerNameView.setText("Unknown number");
        callerNumberView.setText(null);
        callerPhotoView.setImageDrawable(null);
        if (number != null) {
            callerNumberView.setText(number);
            loadCallerData(number);
        }
    }

    @Override
    public void onIncomingCallStarted(Context ctx, String number, Date start) {
        hideInCallUi();
        initCall(number);
        incomingCallTextView.setText(R.string.call_incoming);
        showView();
    }

    @Override
    public void onOutgoingCallStarted(Context ctx, String number, Date start) {
        initCall(number);
        incomingCallTextView.setText(R.string.call_outgoing);
        showView();
    }

    @Override
    public void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
        hideView();
    }

    @Override
    public void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
        hideView();
    }

    @Override
    public void onMissedCall(Context ctx, String number, Date start) {
        hideView();
    }

    private class CoverReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SmartcoverService.ACTION_COVER_STATE.equals(intent.getAction())) {
                state = intent.getStringExtra(SmartcoverService.EXTRA_SCREEN_STATE);
            }
        }
    }
}
