package com.devendra.healthtracker.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import com.devendra.healthtracker.ui.design.DesignTokens;
import com.devendra.healthtracker.ui.elements.ElementalBorderRenderer;
import com.devendra.healthtracker.ui.elements.ElementType;
import com.devendra.healthtracker.ui.haptics.HapticEngine;

/**
 * Premium Soft-Tactile Minimal Card.
 * Serves as the primary content container.
 * When marked interactive, responds with 1.5% scale compression, surface highlight,
 * and tactile haptic response.
 * Supports dynamic procedural elemental border animations (Bubbling, Fire, Thunder, Air, Volcano).
 */
public class PremiumCard extends LinearLayout {

    private boolean isDark = false;
    private boolean isInteractive = false;
    private int cornerRadiusDp = DesignTokens.RADIUS_LG;
    private int customBg = 0;
    private int customStroke = 0;

    private ElementType elementType = null;
    private ElementalBorderRenderer elementalBorderRenderer = null;

    private float pressScale = DesignTokens.SCALE_PRESSED_CARD;
    private ValueAnimator scaleAnimator;
    private boolean isDown = false;
    private final int touchSlop;
    private OnClickListener userClickListener;

    public PremiumCard(Context context) {
        super(context);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    public PremiumCard(Context context, boolean isDark) {
        super(context);
        this.isDark = isDark;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    public PremiumCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        applySurfaceStyle();
    }

    public void setDarkTheme(boolean isDark) {
        this.isDark = isDark;
        applySurfaceStyle();
    }

    public void setCustomColors(int bg, int stroke) {
        this.customBg = bg;
        this.customStroke = stroke;
        applySurfaceStyle();
    }

    public void setCornerRadiusDp(int radiusDp) {
        this.cornerRadiusDp = radiusDp;
        applySurfaceStyle();
    }

    public void setElementType(ElementType type) {
        this.elementType = type;
        if (type != null && elementalBorderRenderer == null) {
            elementalBorderRenderer = new ElementalBorderRenderer(getContext());
        }
        setWillNotDraw(false);
        postInvalidateOnAnimation();
    }

    public ElementType getElementType() {
        return elementType;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (elementType != null && elementalBorderRenderer != null) {
            float density = getResources().getDisplayMetrics().density;
            elementalBorderRenderer.draw(canvas, getWidth(), getHeight(), elementType, cornerRadiusDp * density);
            postInvalidateOnAnimation();
        }
    }

    public void setInteractive(boolean interactive) {
        this.isInteractive = interactive;
        setClickable(interactive);
        setFocusable(interactive);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        this.userClickListener = l;
        setInteractive(l != null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isInteractive || !isEnabled()) return super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDown = true;
                animateScale(pressScale, DesignTokens.DURATION_PRESS_DOWN, new DecelerateInterpolator(1.8f));
                highlightSurface(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDown) {
                    float x = event.getX();
                    float y = event.getY();
                    if (x < -touchSlop || x > getWidth() + touchSlop ||
                        y < -touchSlop || y > getHeight() + touchSlop) {
                        isDown = false;
                        animateScale(1.0f, DesignTokens.DURATION_PRESS_UP, new OvershootInterpolator(1.2f));
                        highlightSurface(false);
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isDown) {
                    isDown = false;
                    animateScale(1.0f, DesignTokens.DURATION_PRESS_UP, new OvershootInterpolator(1.3f));
                    highlightSurface(false);
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
                    highlightSurface(false);
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void highlightSurface(boolean pressed) {
        float density = getResources().getDisplayMetrics().density;
        int radius = (int) (cornerRadiusDp * density);

        int bg = customBg != 0 ? customBg : DesignTokens.getSurfaceCard(isDark);
        int stroke = customStroke != 0 ? customStroke : DesignTokens.getBorder(isDark);

        if (pressed) {
            bg = isDark ? 0xFF222B3E : 0xFFF1F5F9;
        }

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bg);
        gd.setCornerRadius(radius);
        gd.setStroke((int) (1.0f * density), stroke);
        setBackground(gd);
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

    public void applySurfaceStyle() {
        float density = getResources().getDisplayMetrics().density;
        int radius = (int) (cornerRadiusDp * density);

        int bg = customBg != 0 ? customBg : DesignTokens.getSurfaceCard(isDark);
        int stroke = customStroke != 0 ? customStroke : DesignTokens.getBorder(isDark);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bg);
        gd.setCornerRadius(radius);
        gd.setStroke((int) (1.0f * density), stroke);
        setBackground(gd);

        if (Build.VERSION.SDK_INT >= 21) {
            setElevation(DesignTokens.ELEVATION_CARD * density);
        }
    }
}
