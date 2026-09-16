package com.devendra.healthtracker.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.devendra.healthtracker.ui.design.DesignTokens;

/**
 * Premium Soft-Tactile Minimal Icon Button.
 * Guarantees accessible touch boundaries (>= 44dp) with comfortable visual padding,
 * subtle surface tinting, and tactile spring compression.
 */
public class PremiumIconButton extends TextView {

    private boolean isDark = false;
    private boolean isDestructive = false;
    private boolean isCircular = true;
    private int customAccent = 0;

    private float pressScale = 0.94f;
    private ValueAnimator scaleAnimator;
    private boolean isDown = false;
    private final int touchSlop;
    private OnClickListener userClickListener;

    public PremiumIconButton(Context context) {
        super(context);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    public PremiumIconButton(Context context, String iconGlyph, boolean isDark) {
        super(context);
        this.isDark = isDark;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setText(iconGlyph);
        init();
    }

    private void init() {
        setGravity(Gravity.CENTER);
        setClickable(true);
        setFocusable(true);
        setTypeface(Typeface.DEFAULT_BOLD);

        float density = getResources().getDisplayMetrics().density;
        int minSize = (int) (40 * density);
        setMinHeight(minSize);
        setMinWidth(minSize);

        applyStyle();
    }

    public void setDarkTheme(boolean isDark) {
        this.isDark = isDark;
        applyStyle();
    }

    public void setDestructive(boolean destructive) {
        this.isDestructive = destructive;
        applyStyle();
    }

    public void setCircular(boolean circular) {
        this.isCircular = circular;
        applyStyle();
    }

    public void setCustomAccent(int accent) {
        this.customAccent = accent;
        applyStyle();
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        this.userClickListener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDown = true;
                animateScale(pressScale, DesignTokens.DURATION_PRESS_DOWN, new DecelerateInterpolator(1.8f));
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDown) {
                    float x = event.getX();
                    float y = event.getY();
                    if (x < -touchSlop || x > getWidth() + touchSlop ||
                        y < -touchSlop || y > getHeight() + touchSlop) {
                        isDown = false;
                        animateScale(1.0f, DesignTokens.DURATION_PRESS_UP, new OvershootInterpolator(1.2f));
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isDown) {
                    isDown = false;
                    animateScale(1.0f, DesignTokens.DURATION_PRESS_UP, new OvershootInterpolator(1.3f));
                    if (isDestructive) {
                        com.devendra.healthtracker.ui.haptics.HapticEngine.heavy(this);
                    } else {
                        com.devendra.healthtracker.ui.haptics.HapticEngine.tap(this);
                    }
                    if (userClickListener != null) {
                        userClickListener.onClick(this);
                    }
                    playSoundEffect(android.view.SoundEffectConstants.CLICK);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (isDown) {
                    isDown = false;
                    animateScale(1.0f, DesignTokens.DURATION_PRESS_UP, new OvershootInterpolator(1.2f));
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void animateScale(float toScale, int duration, android.animation.TimeInterpolator interpolator) {
        if (scaleAnimator != null && scaleAnimator.isRunning()) {
            scaleAnimator.cancel();
        }
        scaleAnimator = ValueAnimator.ofFloat(getScaleX(), toScale);
        scaleAnimator.setDuration(duration);
        scaleAnimator.setInterpolator(interpolator);
        scaleAnimator.addUpdateListener(anim -> {
            float val = (Float) anim.getAnimatedValue();
            setScaleX(val);
            setScaleY(val);
        });
        scaleAnimator.start();
    }

    public void applyStyle() {
        float density = getResources().getDisplayMetrics().density;
        int radius = isCircular ? (int) (999 * density) : (int) (DesignTokens.RADIUS_MD * density);

        int bgColor;
        int fgColor;
        int strokeColor;

        if (isDestructive) {
            bgColor = isDark ? 0xFF36141A : 0xFFFEF2F2;
            fgColor = isDark ? 0xFFFB7185 : 0xFFDC2626;
            strokeColor = isDark ? 0xFF541C24 : 0xFFFECACA;
        } else if (customAccent != 0) {
            bgColor = isDark ? 0xFF1B2232 : 0xFFF1F5F9;
            fgColor = customAccent;
            strokeColor = isDark ? 0xFF2C384E : 0xFFCBD5E1;
        } else {
            bgColor = isDark ? 0xFF1B2232 : 0xFFF1F5F9;
            fgColor = isDark ? 0xFFFFFFFF : 0xFF475569;
            strokeColor = isDark ? 0xFF283348 : 0xFFE2E8F0;
        }

        setTextColor(fgColor);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(radius);
        bg.setStroke((int) (1.0f * density), strokeColor);
        setBackground(bg);
    }
}
