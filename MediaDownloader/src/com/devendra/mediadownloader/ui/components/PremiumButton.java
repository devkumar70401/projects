package com.devendra.mediadownloader.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

import com.devendra.mediadownloader.theme.ThemeManager;
import com.devendra.mediadownloader.ui.design.DesignTokens;

/**
 * Premium Soft-Tactile Minimal Button.
 * Replaces generic flat-color buttons with refined hierarchy, tactile spring physics,
 * semantic theming, and built-in loading states.
 */
public class PremiumButton extends Button {

    public enum Style {
        PRIMARY,        // Main hero action, bold emphasis
        SECONDARY,      // Elevated neutral surface, subtle border
        TERTIARY,       // Low-emphasis / text-focused
        DESTRUCTIVE,    // Dangerous/delete action (subtle rose tint)
        SUCCESS,        // Confirmative / optimal action (emerald tint)
        OUTLINED        // Transparent surface with clean border
    }

    private Style style = Style.PRIMARY;
    private boolean isDark = false;
    private boolean isLoading = false;
    private String originalText = "";
    private int customAccent = 0;

    private float pressScale = DesignTokens.SCALE_PRESSED_BUTTON;
    private ValueAnimator scaleAnimator;
    private boolean isDown = false;
    private final int touchSlop;
    private OnClickListener userClickListener;

    public PremiumButton(Context context) {
        super(context);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    public PremiumButton(Context context, Style style, boolean isDark) {
        super(context);
        this.style = style;
        this.isDark = isDark;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    public PremiumButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    private void init() {
        setAllCaps(false);
        setTypeface(Typeface.DEFAULT_BOLD);
        setGravity(Gravity.CENTER);
        setClickable(true);
        setFocusable(true);

        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (14 * density);
        int padV = (int) (8 * density);
        setPadding(padH, padV, padH, padV);

        // Ensure minimum touch target height
        setMinHeight((int) (40 * density));

        if (Build.VERSION.SDK_INT >= 21) {
            setStateListAnimator(null); // Eliminate rectangular default shadow
        }

        applyStyle();
    }

    public void setStyle(Style style) {
        this.style = style;
        applyStyle();
    }

    public void setDarkTheme(boolean isDark) {
        this.isDark = isDark;
        applyStyle();
    }

    public void setCustomAccent(int accentColor) {
        this.customAccent = accentColor;
        applyStyle();
    }

    public void setLoading(boolean loading, String loadingText) {
        this.isLoading = loading;
        if (loading) {
            this.originalText = getText().toString();
            setText(loadingText != null ? loadingText : "Processing...");
            setEnabled(false);
            setAlpha(0.7f);
        } else {
            if (originalText != null && !originalText.isEmpty()) {
                setText(originalText);
            }
            setEnabled(true);
            setAlpha(1.0f);
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        this.userClickListener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || isLoading) return false;

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

    public void applyStyle() {
        float density = getResources().getDisplayMetrics().density;
        int radius = (int) (DesignTokens.RADIUS_MD * density);

        int bgColor;
        int fgColor;
        int strokeColor = 0;
        int strokeWidth = 0;

        int activeAccent = customAccent != 0 ? customAccent :
            (isDark ? DesignTokens.ACCENT_ROSE_DARK : DesignTokens.ACCENT_ROSE_PRIMARY);

        switch (style) {
            case PRIMARY:
                bgColor = activeAccent;
                fgColor = Color.WHITE;
                if (Build.VERSION.SDK_INT >= 21) {
                    setElevation(2.0f * density);
                }
                break;

            case SECONDARY:
                bgColor = isDark ? 0xFF2D2D2D : 0xFFF1F5F9;
                fgColor = isDark ? 0xFFD4D4D4 : 0xFF334155;
                strokeColor = isDark ? 0xFF454545 : 0xFFE2E8F0;
                strokeWidth = (int) (1.0f * density);
                if (Build.VERSION.SDK_INT >= 21) {
                    setElevation(1.0f * density);
                }
                break;

            case DESTRUCTIVE:
                bgColor = isDark ? 0xFF3B1B1B : 0xFFFEF2F2;
                fgColor = isDark ? 0xFFF87171 : 0xFFDC2626;
                strokeColor = isDark ? 0xFF5C2424 : 0xFFFECACA;
                strokeWidth = (int) (1.0f * density);
                break;

            case SUCCESS:
                bgColor = isDark ? 0xFF132E24 : 0xFFECFDF5;
                fgColor = isDark ? 0xFF34D399 : 0xFF059669;
                strokeColor = isDark ? 0xFF1B4332 : 0xFFA7F3D0;
                strokeWidth = (int) (1.0f * density);
                break;

            case OUTLINED:
                bgColor = isDark ? 0xFF252526 : Color.WHITE;
                fgColor = isDark ? 0xFFD4D4D4 : 0xFF334155;
                strokeColor = isDark ? 0xFF454545 : 0xFFCBD5E1;
                strokeWidth = (int) (1.0f * density);
                break;

            case TERTIARY:
            default:
                bgColor = Color.TRANSPARENT;
                fgColor = isDark ? 0xFF9D9D9D : 0xFF64748B;
                break;
        }

        setTextColor(fgColor);

        GradientDrawable normalBg = new GradientDrawable();
        normalBg.setColor(bgColor);
        normalBg.setCornerRadius(radius);
        if (strokeWidth > 0 && strokeColor != 0) {
            normalBg.setStroke(strokeWidth, strokeColor);
        }

        setBackground(normalBg);
    }
}
