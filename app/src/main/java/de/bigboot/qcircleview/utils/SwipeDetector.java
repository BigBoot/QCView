package de.bigboot.qcircleview.utils;

import android.view.MotionEvent;
import android.view.View;

public class SwipeDetector implements View.OnTouchListener {

    private int min_distance = 100;
    private float downX;
    private float downY;
    private View v;
    private boolean passEvents = false;

    private onSwipeEvent swipeEventListener = null;


    public SwipeDetector(View v) {
        this(v, false);
    }


    public SwipeDetector(View v, boolean passEvents) {
        this.v = v;
        this.passEvents = passEvents;
        v.setOnTouchListener(this);
    }

    public void setOnSwipeListener(onSwipeEvent listener) {
            swipeEventListener = listener;
    }

    public boolean isPassEvents() {
        return passEvents;
    }

    public void setPassEvents(boolean passEvents) {
        this.passEvents = passEvents;
    }


    public void onRightToLeftSwipe() {
        if (swipeEventListener != null)
            swipeEventListener.SwipeEventDetected(v, SwipeTypeEnum.RIGHT_TO_LEFT);
    }

    public void onLeftToRightSwipe() {
        if (swipeEventListener != null)
            swipeEventListener.SwipeEventDetected(v, SwipeTypeEnum.LEFT_TO_RIGHT);
    }

    public void onTopToBottomSwipe() {
        if (swipeEventListener != null)
            swipeEventListener.SwipeEventDetected(v, SwipeTypeEnum.TOP_TO_BOTTOM);
    }

    public void onBottomToTopSwipe() {
        if (swipeEventListener != null)
            swipeEventListener.SwipeEventDetected(v, SwipeTypeEnum.BOTTOM_TO_TOP);
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = event.getX();
                downY = event.getY();
                return !passEvents;
            }
            case MotionEvent.ACTION_UP: {
                float upX = event.getX();
                float upY = event.getY();

                float deltaX = downX - upX;
                float deltaY = downY - upY;

                //HORIZONTAL SCROLL
                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    if (Math.abs(deltaX) > min_distance) {
                        // left or right
                        if (deltaX < 0) {
                            this.onLeftToRightSwipe();
                            return !passEvents;
                        }
                        if (deltaX > 0) {
                            this.onRightToLeftSwipe();
                            return !passEvents;
                        }
                    } else {
                        //not long enough swipe...
                        return false;
                    }
                }
                //VERTICAL SCROLL
                else {
                    if (Math.abs(deltaY) > min_distance) {
                        // top or down
                        if (deltaY < 0) {
                            this.onTopToBottomSwipe();
                            return !passEvents;
                        }
                        if (deltaY > 0) {
                            this.onBottomToTopSwipe();
                            return !passEvents;
                        }
                    } else {
                        //not long enough swipe...
                        return false;
                    }
                }

                return true;
            }
        }
        return false;
    }

    public interface onSwipeEvent {
        void SwipeEventDetected(View v, SwipeTypeEnum SwipeType);
    }

    public SwipeDetector setMinDistanceInPixels(int min_distance) {
        this.min_distance = min_distance;
        return this;
    }

    public enum SwipeTypeEnum {
        RIGHT_TO_LEFT, LEFT_TO_RIGHT, TOP_TO_BOTTOM, BOTTOM_TO_TOP
    }

}
