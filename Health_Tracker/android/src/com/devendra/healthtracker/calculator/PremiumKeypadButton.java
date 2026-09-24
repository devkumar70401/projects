package com.devendra.healthtracker.calculator;

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
import android.widget.Button;

import com.devendra.healthtracker.ui.design.DesignTokens;

/**
 * Premium Soft-Tactile Keypad Button for Calculator & Scientific Engine.
 * Features specialized semantic hierarchy, tactile 0.95 scale compression,
 * crisp haptic feedback, and anti-glare Dark Mode surfaces.
 */
public class PremiumKeypadButton extends Button {

    public enum KeyType {
        NUMBER,         // 0-9, decimal point
        OPERATOR,       // +, −, ×, ÷, %, ±
        FUNCTION,       // sin, cos, tan, log, ln, sqrt, etc.
        ACTION_CLEAR,   // AC, C, DEL
        ACTION_EQUALS,  // =
        MEMORY,         // MC, MR, M+, M-, MS
        RADIX           // HEX, DEC, OCT, BIN bitwise keys
    }

    private KeyType keyType = KeyType.NUMBER;
    private boolean isDark = false;
    private float pressScale = DesignTokens.SCALE_PRESSED_KEY;
    private ValueAnimator scaleAnimator;
    private boolean isDown = false;
    private final int touchSlop;
    private OnClickListener userClickListener;

    public PremiumKeypadButton(Context context) {
        super(context);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    public PremiumKeypadButton(Context context, String label, KeyType type, boolean isDark) {
        super(context);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.keyType = type;
        this.isDark = isDark;
        setText(label);
        init();
    }

    private void init() {
        setAllCaps(false);
        setGravity(Gravity.CENTER);
        setClickable(true);
        setFocusable(true);
        setTypeface(Typeface.DEFAULT_BOLD);

        float density = getResources().getDisplayMetrics().density;
        setPadding((int) (2 * density), (int) (2 * density), (int) (2 * density), (int) (2 * density));

        if (Build.VERSION.SDK_INT >= 21) {
            setStateListAnimator(null);
        }

        applyKeyStyle();
    }

    public void setKeyType(KeyType type) {
        this.keyType = type;
        applyKeyStyle();
    }

    public void setDarkTheme(boolean isDark) {
        this.isDark = isDark;
        applyKeyStyle();
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
                    try {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    } catch (Exception ignored) {}
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

    public void applyKeyStyle() {
        float density = getResources().getDisplayMetrics().density;
        int radius = (int) (DesignTokens.RADIUS_MD * density);

        int bgColor;
        int fgColor;
        int strokeColor = 0;
        int strokeWidth = 0;

        switch (keyType) {
            case ACTION_EQUALS:
                bgColor = isDark ? DesignTokens.ACCENT_AMBER_DARK : DesignTokens.ACCENT_AMBER_PRIMARY;
                fgColor = isDark ? 0xFF0B0E14 : Color.WHITE;
                if (Build.VERSION.SDK_INT >= 21) {
                    setElevation(2.0f * density);
                }
                break;

            case OPERATOR:
                bgColor = isDark ? 0xFF2B2112 : 0xFFFFFBEB;
                fgColor = isDark ? 0xFFFBBF24 : 0xFFD97706;
                strokeColor = isDark ? 0xFF423318 : 0xFFFDE68A;
                strokeWidth = (int) (1.0f * density);
                break;

            case ACTION_CLEAR:
                bgColor = isDark ? 0xFF36141A : 0xFFFEF2F2;
                fgColor = isDark ? 0xFFFB7185 : 0xFFDC2626;
                strokeColor = isDark ? 0xFF541C24 : 0xFFFECACA;
                strokeWidth = (int) (1.0f * density);
                break;

            case FUNCTION:
            case RADIX:
                bgColor = isDark ? 0xFF1B2232 : 0xFFF1F5F9;
                fgColor = isDark ? 0xFFF8FAFC : 0xFF334155;
                strokeColor = isDark ? 0xFF2C384E : 0xFFE2E8F0;
                strokeWidth = (int) (0.8f * density);
                break;

            case MEMORY:
                bgColor = isDark ? 0xFF161B28 : 0xFFF8FAFC;
                fgColor = isDark ? 0xFF94A3B8 : 0xFF64748B;
                strokeColor = isDark ? 0xFF253046 : 0xFFE2E8F0;
                strokeWidth = (int) (0.8f * density);
                radius = (int) (DesignTokens.RADIUS_SM * density);
                break;

            case NUMBER:
            default:
                bgColor = isDark ? 0xFF181E2C : Color.WHITE;
                fgColor = isDark ? 0xFFFFFFFF : 0xFF0F172A;
                strokeColor = isDark ? 0xFF263044 : 0xFFE2E8F0;
                strokeWidth = (int) (1.0f * density);
                if (Build.VERSION.SDK_INT >= 21) {
                    setElevation(1.0f * density);
                }
                break;
        }

        setTextColor(fgColor);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(radius);
        if (strokeWidth > 0 && strokeColor != 0) {
            bg.setStroke(strokeWidth, strokeColor);
        }
        setBackground(bg);
    }
}
