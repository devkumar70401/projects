package com.devendra.healthtracker.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;

public class SnapHorizontalScrollView extends HorizontalScrollView {

    public interface OnPageChangeListener {
        void onPageSelected(int page);
    }

    private int currentPage = 0; // Start on First Card (Diet / Routine)
    private int totalPages = 6;
    private OnPageChangeListener pageListener;

    public void setTotalPages(int pages) {
        this.totalPages = pages;
    }
    private VelocityTracker velocityTracker;
    private float startX = 0;
    private float startY = 0;
    private boolean isHorizontalScroll = false;
    private ValueAnimator animator;

    public SnapHorizontalScrollView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setFillViewport(false);
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.pageListener = listener;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void scrollToPage(int page, boolean animate) {
        currentPage = Math.max(0, Math.min(totalPages - 1, page));
        int targetX = currentPage * getWidth();

        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }

        if (animate && getWidth() > 0) {
            animator = ValueAnimator.ofInt(getScrollX(), targetX);
            animator.setDuration(280);
            animator.setInterpolator(new DecelerateInterpolator(1.8f));
            animator.addUpdateListener(animation -> scrollTo((int) animation.getAnimatedValue(), 0));
            animator.start();
        } else {
            scrollTo(targetX, 0);
        }

        if (pageListener != null) {
            pageListener.onPageSelected(currentPage);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                isHorizontalScroll = false;
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - startX);
                float dy = Math.abs(ev.getY() - startY);
                if (dx > dy && dx > 15) {
                    isHorizontalScroll = true;
                    if (velocityTracker != null) velocityTracker.addMovement(ev);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isHorizontalScroll = false;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                velocityTracker.computeCurrentVelocity(1000);
                float xVelocity = velocityTracker.getXVelocity();
                int pageWidth = getWidth();
                int scrollX = getScrollX();

                int targetPage = currentPage;
                // Strong fling check
                if (xVelocity > 800 && currentPage > 0) {
                    targetPage = currentPage - 1;
                } else if (xVelocity < -800 && currentPage < totalPages - 1) {
                    targetPage = currentPage + 1;
                } else if (pageWidth > 0) {
                    // Position threshold check (30% displacement)
                    float deltaX = scrollX - (currentPage * pageWidth);
                    if (deltaX > pageWidth * 0.25f && currentPage < totalPages - 1) {
                        targetPage = currentPage + 1;
                    } else if (deltaX < -pageWidth * 0.25f && currentPage > 0) {
                        targetPage = currentPage - 1;
                    }
                }

                scrollToPage(targetPage, true);

                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                return true;
        }

        return super.onTouchEvent(ev);
    }
}
