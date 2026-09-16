package com.devendra.mediadownloader.ui.design;

import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Universal tactile interaction helper implementing iOS-inspired micro-interactions:
 * - Subtle scale compression on touch down (100% -> 96.5%)
 * - Fluid spring recovery on release
 * - Touch slop boundary detection to cancel if user scrolls
 * - Crisp haptic feedback on successful tap
 */
public class TouchFeedback implements View.OnTouchListener {

    private final View targetView;
    private final View.OnClickListener clickListener;
    private final float pressScale;
    private final Rect viewBounds = new Rect();
    private ValueAnimator scaleAnimator;
    private boolean isDown = false;
    private final int touchSlop;

    public TouchFeedback(View view, View.OnClickListener listener, float pressScale) {
        this.targetView = view;
        this.clickListener = listener;
        this.pressScale = pressScale;
        this.touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
    }

    public static void attach(View view, View.OnClickListener listener) {
        attach(view, listener, DesignTokens.SCALE_PRESSED_BUTTON);
    }

    public static void attach(View view, View.OnClickListener listener, float pressScale) {
        if (view == null) return;
        view.setClickable(true);
        view.setFocusable(true);
        TouchFeedback feedback = new TouchFeedback(view, listener, pressScale);
        view.setOnTouchListener(feedback);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!v.isEnabled()) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDown = true;
                v.getDrawingRect(viewBounds);
                animateScale(v, pressScale, DesignTokens.DURATION_PRESS_DOWN, new DecelerateInterpolator(1.8f));
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDown) {
                    float x = event.getX();
                    float y = event.getY();
                    if (x < -touchSlop || x > viewBounds.width() + touchSlop ||
                        y < -touchSlop || y > viewBounds.height() + touchSlop) {
                        isDown = false;
                        animateScale(v, 1.0f, DesignTokens.DURATION_PRESS_UP, new OvershootInterpolator(1.2f));
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isDown) {
                    isDown = false;
                    animateScale(v, 1.0f, DesignTokens.DURATION_PRESS_UP, new OvershootInterpolator(1.3f));
                    try {
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    } catch (Exception ignored) {}
                    if (clickListener != null) {
                        clickListener.onClick(v);
                    }
                    v.playSoundEffect(android.view.SoundEffectConstants.CLICK);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (isDown) {
                    isDown = false;
                    animateScale(v, 1.0f, DesignTokens.DURATION_PRESS_UP, new OvershootInterpolator(1.2f));
                }
                return true;
        }

        return false;
    }

    private void animateScale(View v, float toScale, int duration, android.animation.TimeInterpolator interpolator) {
        if (scaleAnimator != null && scaleAnimator.isRunning()) {
            scaleAnimator.cancel();
        }
        float fromScale = v.getScaleX();
        scaleAnimator = ValueAnimator.ofFloat(fromScale, toScale);
        scaleAnimator.setDuration(duration);
        scaleAnimator.setInterpolator(interpolator);
        scaleAnimator.addUpdateListener(anim -> {
            float val = (Float) anim.getAnimatedValue();
            v.setScaleX(val);
            v.setScaleY(val);
        });
        scaleAnimator.start();
    }
}
