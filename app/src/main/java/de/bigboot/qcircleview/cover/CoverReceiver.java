package de.bigboot.qcircleview.cover;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.androidannotations.annotations.EReceiver;
import org.androidannotations.annotations.ReceiverAction;

import de.bigboot.qcircleview.SmartcoverService;

/**
 * Created by Marco Kirchner.
 */
@EReceiver
public class CoverReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {}

    @ReceiverAction(SmartcoverService.ACTION_COVER_STATE)
    protected void onCoverState(@ReceiverAction.Extra(SmartcoverService.EXTRA_SCREEN_STATE)String state,
                                @ReceiverAction.Extra(SmartcoverService.EXTRA_STATE_CHANGED)boolean stateChanged,
                                Context context) {
        if (stateChanged && "Closed".equals(state)) {
            CoverActivity_.intent(context).
                    flags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .start();
        }
    }
}
