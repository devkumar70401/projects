package com.devendra.healthtracker.ui.components;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

/**
 * SwipeGestureLayout
 * Smoothly captures horizontal swipe gestures (left-to-right and right-to-left)
 * while allowing vertical scrolling inside child views to operate without interference.
 */
public class SwipeGestureLayout extends FrameLayout {

    public interface OnSwipeListener {
        void onSwipeRight();
        void onSwipeLeft();
        default void onSwipeUp() {}
        default void onSwipeDown() {}
    }

    private OnSwipeListener swipeListener;
    private float startX;
    private float startY;
    private boolean isHorizontalSwipe = false;
    private int touchSlop;
    private int minSwipeDistance;

    public SwipeGestureLayout(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        minSwipeDistance = (int) (context.getResources().getDisplayMetrics().density * 55);
    }

    public void setOnSwipeListener(OnSwipeListener listener) {
        this.swipeListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (swipeListener == null) {
            return super.onInterceptTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                isHorizontalSwipe = false;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = ev.getX() - startX;
                float dy = ev.getY() - startY;
                float absDx = Math.abs(dx);
                float absDy = Math.abs(dy);

                // Only intercept decisively horizontal swipes (e.g. swipe-back or opening drawer)
                // Never intercept vertical gestures, allowing child views like ScrollView to operate freely
                if (absDx > touchSlop && absDx > absDy * 1.4f) {
                    isHorizontalSwipe = true;
                    return true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isHorizontalSwipe = false;
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (swipeListener == null) {
            return super.onTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = ev.getX() - startX;
                float dy = ev.getY() - startY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 1.4f) {
                    isHorizontalSwipe = true;
                }
                return true;

            case MotionEvent.ACTION_UP:
                float finalDx = ev.getX() - startX;
                float finalDy = ev.getY() - startY;
                if (isHorizontalSwipe && Math.abs(finalDx) > minSwipeDistance && Math.abs(finalDx) > Math.abs(finalDy)) {
                    if (finalDx > 0) {
                        swipeListener.onSwipeRight();
                    } else {
                        swipeListener.onSwipeLeft();
                    }
                    isHorizontalSwipe = false;
                    return true;
                }
                isHorizontalSwipe = false;
                break;

            case MotionEvent.ACTION_CANCEL:
                isHorizontalSwipe = false;
                break;
        }

        return super.onTouchEvent(ev);
    }
}
