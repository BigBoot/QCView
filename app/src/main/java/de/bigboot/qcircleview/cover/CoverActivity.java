/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package de.bigboot.qcircleview.cover;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.ViewById;

import de.bigboot.qcirclelib.QCircleActivity;
import de.bigboot.qcircleview.Preferences;
import de.bigboot.qcircleview.R;


@EActivity
public class CoverActivity extends QCircleActivity {

    @ViewById(R.id.circle_animation)
    protected CircleAnimationView circleAnimationView;

    @ViewById(R.id.knock_code)
    protected KnockView knockCode;

    @FragmentById(R.id.content)
    protected CoverFragment coverFragment;

    @Override
    protected CircleView onCreateView(CircleViewBuilder builder) {
        builder.setUseMask(false);
        return builder.build(R.layout.activity_main);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onCoverOpened() {
        super.onCoverOpened();
        coverFragment.onCoverOpened();
    }

    @AfterViews
    protected void init() {
        updateKnockView();
    }

    @Receiver(actions = Intent.ACTION_SCREEN_ON)
    protected void onScreenOn() {
        if(circleAnimationView != null) {
            circleAnimationView.startAnimation();
            updateKnockView();
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    onScreenOn();
                }
            }, 100L);
        }
    }

    private void updateKnockView() {
        Preferences prefs = new Preferences(this);
        if (prefs.getBoolean(Preferences.BooleanSettings.KnockCodeEnabled) && prefs.getKnockCode().length > 0) {
            setDoubleTap2SleepEnabled(false);
            knockCode.setKnockCode(prefs.getKnockCode());
            knockCode.setVisibility(View.VISIBLE);
            knockCode.setUnlockListener(new KnockView.UnlockListener() {
                @Override
                public void onUnlock() {
                    setDoubleTap2SleepEnabled(true);
                    knockCode.setVisibility(View.GONE);
                }
            });
        } else {
            knockCode.setVisibility(View.GONE);
        }
    }
}
