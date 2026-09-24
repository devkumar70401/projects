package com.devendra.healthtracker.ui.components;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;

import com.devendra.healthtracker.ui.haptics.HapticEngine;

/**
 * Modern iOS / Cupertino-style animated toggle switch.
 * Features:
 * - Pill-shaped background track with smooth color morphing
 * - Elevated white thumb knob with realistic drop-shadow
 * - Fluid ValueAnimator slide physics (220ms)
 * - Both tap-to-toggle and horizontal drag gestures
 * - Parent scroll interception lock for nested horizontal pagers
 * - Subtle haptic feedback on state change
 */
public class ModernToggleSwitch extends View {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(ModernToggleSwitch toggleSwitch, boolean isChecked);
    }

    private boolean isChecked = true;
    private float animProgress = 1.0f; // 0.0f = OFF, 1.0f = ON
    private ValueAnimator animator;
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    // Visual theme colors (Modern Emerald & Slate)
    private int onColor = Color.parseColor("#10B981");       // Modern Emerald Green
    private int offColor = Color.parseColor("#E2E8F0");      // Slate-200
    private int offBorderColor = Color.parseColor("#CBD5E1");// Slate-300
    private int thumbColor = Color.parseColor("#FFFFFF");    // Pure Porcelain White
    private int thumbBorderColor = Color.parseColor("#E2E8F0");

    // Paint & Geometry objects
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF trackRect = new RectF();
    private final RectF borderRect = new RectF();

    private OnCheckedChangeListener listener;

    // Gesture tracking
    private float touchStartX;
    private float touchStartY;
    private boolean isDragging = false;
    private int touchSlop;

    public ModernToggleSwitch(Context context) {
        super(context);
        init();
    }

    public ModernToggleSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ModernToggleSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        // Software layer ensures thumb drop-shadow renders smoothly across all OEM GPU pipelines
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        float density = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Math.max(1f, 1f * density));

        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(thumbColor);
        // Realistic iOS drop shadow
        thumbPaint.setShadowLayer(2.5f * density, 0, 1.5f * density, Color.parseColor("#33000000"));

        thumbBorderPaint.setStyle(Paint.Style.STROKE);
        thumbBorderPaint.setColor(thumbBorderColor);
        thumbBorderPaint.setStrokeWidth(0.5f * density);

        animProgress = isChecked ? 1.0f : 0.0f;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        int defaultWidth = (int) (50 * density);
        int defaultHeight = (int) (30 * density);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = (widthMode == MeasureSpec.EXACTLY) ? widthSize : defaultWidth;
        int height = (heightMode == MeasureSpec.EXACTLY) ? heightSize : defaultHeight;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        float cornerRadius = height / 2.0f;
        float padding = Math.max(2f, height * 0.07f); // ~2dp padding around thumb
        float thumbDiameter = height - (padding * 2.0f);
        float thumbRadius = thumbDiameter / 2.0f;

        // 1. Draw Track (Pill)
        int currentTrackColor = (Integer) argbEvaluator.evaluate(animProgress, offColor, onColor);
        trackPaint.setColor(currentTrackColor);
        trackPaint.setStyle(Paint.Style.FILL);
        trackRect.set(0, 0, width, height);
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint);

        // 2. Draw Track Border when in OFF or transitioning
        if (animProgress < 0.99f) {
            int alpha = (int) ((1.0f - animProgress) * 255);
            borderPaint.setColor(offBorderColor);
            borderPaint.setAlpha(alpha);
            float strokeHalf = borderPaint.getStrokeWidth() / 2.0f;
            borderRect.set(strokeHalf, strokeHalf, width - strokeHalf, height - strokeHalf);
            canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, borderPaint);
        }

        // 3. Draw Thumb (White elevated circle)
        float maxTravel = width - (padding * 2.0f) - thumbDiameter;
        float thumbCenterX = padding + thumbRadius + (animProgress * maxTravel);
        float thumbCenterY = height / 2.0f;

        canvas.drawCircle(thumbCenterX, thumbCenterY, thumbRadius, thumbPaint);
        canvas.drawCircle(thumbCenterX, thumbCenterY, thumbRadius, thumbBorderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                isDragging = false;
                // Lock parent scroll so user can freely interact with toggle
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - touchStartX;
                float dy = event.getY() - touchStartY;
                if (!isDragging && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    isDragging = true;
                }

                if (isDragging) {
                    float width = getWidth();
                    float height = getHeight();
                    float padding = Math.max(2f, height * 0.07f);
                    float thumbDiameter = height - (padding * 2.0f);
                    float maxTravel = width - (padding * 2.0f) - thumbDiameter;
                    if (maxTravel > 0) {
                        float newProgress = (event.getX() - padding - (thumbDiameter / 2.0f)) / maxTravel;
                        animProgress = Math.max(0.0f, Math.min(1.0f, newProgress));
                        invalidate();
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }

                boolean targetState;
                if (isDragging) {
                    targetState = animProgress >= 0.5f;
                } else {
                    targetState = !isChecked;
                }
                setChecked(targetState, true, true);
                isDragging = false;
                performClick();
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                setChecked(isChecked, true, false);
                isDragging = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        setChecked(checked, true, false);
    }

    public void setChecked(boolean checked, boolean animate) {
        setChecked(checked, animate, false);
    }

    public void setChecked(boolean checked, boolean animate, boolean notifyListener) {
        boolean stateChanged = (this.isChecked != checked);
        this.isChecked = checked;

        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }

        if (animate && isAttachedToWindow()) {
            float start = animProgress;
            float end = checked ? 1.0f : 0.0f;
            animator = ValueAnimator.ofFloat(start, end);
            animator.setDuration(220);
            animator.setInterpolator(new DecelerateInterpolator(1.5f));
            animator.addUpdateListener(animation -> {
                animProgress = (Float) animation.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            animProgress = checked ? 1.0f : 0.0f;
            invalidate();
        }

        if (stateChanged) {
            HapticEngine.tick(this);
            if (notifyListener && listener != null) {
                listener.onCheckedChanged(this, checked);
            }
        }
    }

    public void toggle() {
        setChecked(!isChecked, true, true);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    public void setOnColor(int color) {
        this.onColor = color;
        invalidate();
    }

    public void setOffColor(int color) {
        this.offColor = color;
        invalidate();
    }

    public void setDarkTheme(boolean isDark) {
        if (isDark) {
            this.offColor = Color.parseColor("#161B28");
            this.offBorderColor = Color.parseColor("#2A344C");
            this.thumbColor = Color.parseColor("#FFFFFF");
            this.thumbBorderColor = Color.parseColor("#2F3B54");
        } else {
            this.offColor = Color.parseColor("#E2E8F0");
            this.offBorderColor = Color.parseColor("#CBD5E1");
            this.thumbColor = Color.parseColor("#FFFFFF");
            this.thumbBorderColor = Color.parseColor("#E2E8F0");
        }
        thumbPaint.setColor(thumbColor);
        thumbBorderPaint.setColor(thumbBorderColor);
        invalidate();
    }
}
