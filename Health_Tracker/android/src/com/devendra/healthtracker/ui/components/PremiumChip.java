package com.devendra.healthtracker.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.devendra.healthtracker.ui.design.DesignTokens;
import com.devendra.healthtracker.ui.haptics.HapticEngine;

/**
 * Premium Soft-Tactile Chip.
 * Used for category tags, format filters, quality selectors, and presets.
 */
public class PremiumChip extends TextView {

    private boolean isSelected = false;
    private boolean isDark = false;
    private int accentColor = DesignTokens.ACCENT_ROSE_PRIMARY;
    private String chipTag = "";

    private float pressScale = 0.95f;
    private ValueAnimator scaleAnimator;
    private boolean isDown = false;
    private final int touchSlop;
    private OnClickListener userClickListener;

    public PremiumChip(Context context) {
        super(context);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    public PremiumChip(Context context, String text, boolean isDark) {
        super(context);
        this.isDark = isDark;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setText(text);
        init();
    }

    private void init() {
        setGravity(Gravity.CENTER);
        setClickable(true);
        setFocusable(true);
        setSingleLine(true);

        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (12 * density);
        int padV = (int) (6 * density);
        setPadding(padH, padV, padH, padV);
        setTextSize(11);

        applyStyle();
    }

    public void setDarkTheme(boolean isDark) {
        this.isDark = isDark;
        applyStyle();
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
        applyStyle();
    }

    public void setChipSelected(boolean selected) {
        this.isSelected = selected;
        applyStyle();
    }

    public boolean isChipSelected() {
        return isSelected;
    }

    public void setChipTag(String tag) {
        this.chipTag = tag;
    }

    public String getChipTag() {
        return chipTag;
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
                    HapticEngine.tap(this);
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
        int radius = (int) (DesignTokens.RADIUS_MD * density);

        int bgColor;
        int fgColor;
        int strokeColor;

        if (isSelected) {
            // Selected: Soft accent tint background with accent border
            int r = Color.red(accentColor);
            int g = Color.green(accentColor);
            int b = Color.blue(accentColor);
            bgColor = isDark ? Color.argb(45, r, g, b) : Color.argb(25, r, g, b);
            fgColor = isDark ? (accentColor == DesignTokens.ACCENT_ROSE_PRIMARY ? DesignTokens.ACCENT_ROSE_DARK : accentColor) : accentColor;
            strokeColor = accentColor;
            setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            // Unselected: Neutral elevated surface with subtle border (Obsidian Midnight)
            bgColor = isDark ? 0xFF181E2C : 0xFFFFFFFF;
            fgColor = isDark ? 0xFF94A3B8 : 0xFF64748B;
            strokeColor = isDark ? 0xFF253046 : 0xFFE2E8F0;
            setTypeface(Typeface.DEFAULT);
        }

        setTextColor(fgColor);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(radius);
        bg.setStroke((int) (1.0f * density), strokeColor);
        setBackground(bg);
    }
}
