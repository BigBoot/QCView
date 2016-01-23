package de.bigboot.qcircleview.config;

import android.app.Activity;
import android.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;

import de.bigboot.qcircleview.Preferences;
import de.bigboot.qcircleview.R;
import de.bigboot.qcircleview.cover.KnockView;

@EActivity(R.layout.activity_knock_settings)
public class KnockSettingsActivity extends Activity {
    @ViewById(R.id.toolbar)
    protected Toolbar toolbar;

    @FragmentById(R.id.content)
    protected KnockSettingsFragment content;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_knock_settings, menu);
        MenuItem item = menu.findItem(R.id.enabled);
        Switch enabledSwitch = (Switch) item.getActionView().findViewById(R.id.switchForActionBar);
        enabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                onEnabledChanged(b);
            }
        });
        Preferences preferences = new Preferences(this);
        enabledSwitch.setChecked(preferences.getBoolean(Preferences.BooleanSettings.KnockCodeEnabled));
        return true;
    }

    @AfterViews
    protected void init() {
        setActionBar(toolbar);
        Preferences preferences = new Preferences(this);
        content.setEnabled(preferences.getBoolean(Preferences.BooleanSettings.KnockCodeEnabled));
    }

    protected void onEnabledChanged(boolean b) {
        content.setEnabled(b);
    }

    @EFragment(R.layout.fragment_knock_settings)
    public static class KnockSettingsFragment extends Fragment {
        private static final int MINIMUM_CODE_LENGTH = 3;
        @ViewById(R.id.knock_code)
        protected KnockView knockView;

        @ViewById(R.id.cancel_button)
        protected Button cancelButton;

        @ViewById(R.id.continue_button)
        protected Button continueButton;

        @ViewById(R.id.description)
        protected TextView descriptionView;

        private int[] confirmKnockCode = null;
        private ArrayList<Integer> currentKnockCode = new ArrayList<>();

        @AfterViews
        protected void init() {
            knockView.setClickListener(new KnockView.ClickListener() {
                @Override
                public void onClick(int sector) {
                    onSectorClick(sector);
                }
            });
        }

        private void onSectorClick(int sector) {
            currentKnockCode.add(sector);
            cancelButton.setText(R.string.pref_knock_code_retry);
            updateButtons();
        }

        private void updateButtons() {
            if (currentKnockCode.size() >= MINIMUM_CODE_LENGTH) {
                continueButton.setEnabled(true);
            } else {
                continueButton.setEnabled(false);
            }
            if (currentKnockCode.size() == 0 && confirmKnockCode == null) {
                cancelButton.setEnabled(false);
            } else {
                cancelButton.setEnabled(true);
            }
        }

        @Click(R.id.continue_button)
        protected void onContinue() {
            if (confirmKnockCode == null) {
                confirmKnockCode = new int[currentKnockCode.size()];
                for (int i = 0; i < confirmKnockCode.length; i++) {
                    confirmKnockCode[i] = currentKnockCode.get(i);
                }
                currentKnockCode.clear();
                updateButtons();
                cancelButton.setText(R.string.pref_knock_code_cancel);
                continueButton.setText(R.string.pref_knock_code_finish);
                Toast.makeText(getActivity(), "Please repeat to confirm the code",
                        Toast.LENGTH_SHORT).show();
            } else {
                if(currentKnockCode.size() == confirmKnockCode.length) {
                    for (int i = 0; i < confirmKnockCode.length; i++) {
                        if (confirmKnockCode[i] != currentKnockCode.get(i)) {
                            Toast.makeText(getActivity(), "The codes don't match, please try again.",
                                    Toast.LENGTH_SHORT).show();
                            onCancel();
                            return;
                        }
                    }
                    Preferences prefs = new Preferences(getActivity());
                    prefs.setKnockCode(confirmKnockCode);
                    prefs.putBoolean(Preferences.BooleanSettings.KnockCodeEnabled, true);
                    Toast.makeText(getActivity(), "The knock code has been set successfully",
                            Toast.LENGTH_SHORT).show();
                    currentKnockCode.clear();
                    onCancel();
                    return;
                }
                Toast.makeText(getActivity(), "The codes don't match, please try again.",
                        Toast.LENGTH_SHORT).show();
                currentKnockCode.clear();
                onCancel();
            }
        }

        @Click(R.id.cancel_button)
        protected void onCancel() {
            if (currentKnockCode.size() > 0) {
                currentKnockCode.clear();
                continueButton.setText(R.string.pref_knock_code_continue);
                cancelButton.setText(R.string.pref_knock_code_cancel);
                continueButton.setEnabled(false);
            } else if (confirmKnockCode != null) {
                currentKnockCode.clear();
                continueButton.setText(R.string.pref_knock_code_continue);
                cancelButton.setText(R.string.pref_knock_code_cancel);
                continueButton.setEnabled(false);
                confirmKnockCode = null;
            } else {
                getActivity().finish();
            }
        }

        public void setEnabled(boolean enabled) {
            Preferences prefs = new Preferences(getActivity());
            prefs.putBoolean(Preferences.BooleanSettings.KnockCodeEnabled, enabled);

            cancelButton.setEnabled(enabled);
            continueButton.setEnabled(enabled);
            knockView.setEnabled(enabled);
            descriptionView.setEnabled(enabled);

            if (enabled)
                updateButtons();
        }
    }
}
