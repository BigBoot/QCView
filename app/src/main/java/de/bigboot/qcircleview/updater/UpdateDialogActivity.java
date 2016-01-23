package de.bigboot.qcircleview.updater;

import android.app.Activity;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;

import de.bigboot.qcircleview.R;

@EActivity(R.layout.activity_update_dialog)
public class UpdateDialogActivity extends Activity {
    @Extra
    protected Update[] updates;

    @ViewById(R.id.title)
    protected TextView title;
    @ViewById(R.id.content)
    protected TextView content;

    @AfterViews
    protected void init() {
        setTitle(getString(R.string.update_dialog_title));
        StringBuilder contentBuilder = new StringBuilder();

        for (Update update : updates) {
            contentBuilder.append(update.getVersionName());
            contentBuilder.append(":\n");

            for (String change : update.getChanges()) {
                contentBuilder.append(" \u2022 ");
                contentBuilder.append(change);
                contentBuilder.append("\n");
            }
            contentBuilder.append("\n");
        }

        content.setText(contentBuilder.toString());
    }

    @Click(R.id.btn_update)
    protected void onUpdateClicked() {
        new UpdateManager(getApplicationContext()).downloadUpdate(updates[0]);
        finish();
    }

    @Click(R.id.btn_cancel)
    protected void onCancelClicked() {
        finish();
    }
}
