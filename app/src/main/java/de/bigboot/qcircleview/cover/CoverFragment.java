/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bigboot.qcircleview.cover;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.ViewById;

import java.util.List;

import de.bigboot.qcircleview.NotificationService;
import de.bigboot.qcircleview.NotificationService_;
import de.bigboot.qcircleview.Preferences;
import de.bigboot.qcircleview.R;
import de.bigboot.qcircleview.config.Clock;
import de.bigboot.qcircleview.utils.SwipeDetector;

import static de.bigboot.qcircleview.NotificationService.Notification;

@EFragment(R.layout.fragment_cover)
public class CoverFragment extends Fragment {

    @ViewById(R.id.viewpager)
    protected ViewPager mViewPager;
    @ViewById(R.id.menu_slide)
    protected SlidingUpPanelLayout menuSlide;
    @ViewById(R.id.menu)
    protected View menu;
    private CoverAdapter adapter;
    private Notification currentNotification = null;
    private MediaSessionManager mediaSessionManager;
    private MediaSessionListener mediaSessionListener;

    @AfterViews
    protected void init() {
        Preferences preferences = new Preferences(getActivity());

        FragmentManager fragMan = getChildFragmentManager();
        FragmentTransaction fragTransaction = fragMan.beginTransaction();

        Clock clock = preferences.getActiveClock();
        if(clock == null) {
            clock = Clock.STATIC_CLOCKS[0];
        }
        Fragment fragment;
        if(clock instanceof Clock.StaticClock) {
            Clock.StaticClock staticClock = (Clock.StaticClock) clock;
            fragment = staticClock.getFragment();
        } else {
            fragment = CoverAnalogClockFragment_.builder().clock(clock).build();
        }
        fragTransaction.add(R.id.clock_layout, fragment);
        fragTransaction.commit();

        adapter = new CoverAdapter(getActivity());
        mediaSessionListener = new MediaSessionListener();
        mediaSessionManager = (MediaSessionManager) getActivity().getSystemService(Context.MEDIA_SESSION_SERVICE);
        try {
            List<MediaController> mediaControllers = mediaSessionManager.getActiveSessions(new ComponentName(getActivity(), NotificationService_.class));
            adapter.onActiveSessionsChanged(mediaControllers);
            mediaSessionManager.addOnActiveSessionsChangedListener(mediaSessionListener, new ComponentName(getActivity(), NotificationService_.class));
        } catch (SecurityException ex) {
            // No notification access
        }

        mViewPager.setAdapter(adapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == adapter.getDefaultFragmentPosition()) {
                    currentNotification = null;
                    menuSlide.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.menu_panel_height));
                } else {
                    currentNotification = adapter.getNotification(position);
                    menuSlide.setPanelHeight(0);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        new SwipeDetector(mViewPager, true).setOnSwipeListener(new SwipeDetector.onSwipeEvent() {
            @Override
            public void SwipeEventDetected(View v, SwipeDetector.SwipeTypeEnum SwipeType) {
                if (SwipeType == SwipeDetector.SwipeTypeEnum.TOP_TO_BOTTOM && menuSlide.getPanelHeight() > 0) {
                    menuSlide.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // We should call
        // mediaSessionManager.addOnActiveSessionsChangedListener(mediaSessionListener, new ComponentName(getActivity(), NotificationService_.class));
        // but that doesn't work: https://code.google.com/p/android/issues/detail?id=161398
        // So we use a workaround
        mediaSessionListener.active = true;
        mViewPager.setCurrentItem(adapter.getDefaultFragmentPosition(), false);
        getActivity().sendBroadcast(new Intent(NotificationService.ACTION_COMMAND).
                putExtra(NotificationService.EXTRA_COMMAND, NotificationService.COMMAND_LIST));
        Preferences prefs = new Preferences(getActivity());
        menuSlide.setTouchEnabled(prefs.getBoolean(Preferences.BooleanSettings.EnableLauncher));
    }

    @Override
    public void onPause() {
        if(menuSlide.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            menuSlide.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
        // We should call
        // mediaSessionManager.removeOnActiveSessionsChangedListener(mediaSessionListener);
        // but that doesn't work: https://code.google.com/p/android/issues/detail?id=161398
        // So we use a workaround
        mediaSessionListener.active = false;
        super.onPause();
    }

    @Receiver(actions = NotificationService.ACTION_NOTIFICATION_ADDED)
    protected void onNotificationAdded(@Receiver.Extra(NotificationService.EXTRA_NOTIFCATION)Notification notification) {
        boolean onEmptyPage = mViewPager.getCurrentItem() == adapter.getDefaultFragmentPosition();
        adapter.onNotificationAdded(notification);
        if(onEmptyPage) {
            mViewPager.setCurrentItem(adapter.getDefaultFragmentPosition());
        }
    }

    @Receiver(actions = NotificationService.ACTION_NOTIFICATION_REMOVED)
    protected void onNotificationRemoved(@Receiver.Extra(NotificationService.EXTRA_NOTIFCATION) Notification notification,
                                         @Receiver.Extra(NotificationService.EXTRA_INDEX) int index) {
        adapter.onNotificationRemoved(notification, index);
    }

    public void onCoverOpened() {
        if(currentNotification != null && menuSlide.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
            try {
                currentNotification.getContentIntent().send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    private class MediaSessionListener implements MediaSessionManager.OnActiveSessionsChangedListener {
        public boolean active = true;
        @Override
        public void onActiveSessionsChanged(List<MediaController> mediaControllers) {
            if(active)
                adapter.onActiveSessionsChanged(mediaControllers);
        }
    }
}
