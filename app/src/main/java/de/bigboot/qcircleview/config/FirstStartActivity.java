package de.bigboot.qcircleview.config;

import android.app.Activity;
import android.content.Intent;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.Toolbar;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.ViewById;

import de.bigboot.qcircleview.NotificationService;
import de.bigboot.qcircleview.Preferences;
import de.bigboot.qcircleview.R;
import de.bigboot.qcircleview.RootTools;

@EActivity(R.layout.activity_first_start)
public class FirstStartActivity extends Activity {
    @ViewById(R.id.root_access)
    CheckedTextView rootAccessView;

    @ViewById(R.id.notification_access)
    CheckedTextView notificationAccessView;

    @ViewById(R.id.continue_button)
    Button continueButton;

    @ViewById(R.id.toolbar)
    Toolbar toolbar;

    @ViewById(R.id.dt2wfix)
    Switch dt2wfix;

    @ViewById(R.id.scrollView)
    ScrollView scrollView;

    @AfterViews
    protected void init() {
        setActionBar(toolbar);
        dt2wfix.setChecked(new Preferences(this).getBoolean(Preferences.BooleanSettings.DT2WFix));
        if (!new Preferences(this).getBoolean(Preferences.BooleanSettings.FirstStart)) {
            onRequestRoot();
        }
        sendBroadcast(new Intent(NotificationService.ACTION_COMMAND).putExtra(
                NotificationService.EXTRA_COMMAND, NotificationService.COMMAND_IS_CONNECTED
        ));
    }

    @Click(R.id.show_notification_settings)
    protected void onShowNotificationSettings() {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
    }

    @Click(R.id.request_root)
    protected void onRequestRoot() {
        if(RootTools.hasRootAccess()) {
            rootAccessView.setChecked(true);
            continueButton.setEnabled(true);
        } else {
            rootAccessView.setChecked(false);
        }
    }

    @Click(R.id.continue_button)
    protected void onContinue() {
        new Preferences(this).putBoolean(Preferences.BooleanSettings.FirstStart, false);
        QuickcirclemodSettings_.intent(this).start();
        this.finish();
    }

    @Click(R.id.dt2wfix)
    protected void onDT2WFixChanged() {
        new Preferences(this).putBoolean(Preferences.BooleanSettings.DT2WFix, dt2wfix.isChecked());
    }

    @Receiver(actions = NotificationService.ACTION_IS_CONNECTED)
    protected void onNotificationServiceIsConnected(
            @Receiver.Extra(NotificationService.EXTRA_IS_CONNECTED)boolean isConnected) {
        notificationAccessView.setChecked(isConnected);
    }
}
