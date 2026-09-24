package com.devendra.healthtracker.theme;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Premium iOS-inspired segmented theme toggle control:
 * [ ☀ Light | 🌙 Dark ]
 * Features smooth animated thumb sliding, tactile haptic feedback,
 * and high-contrast semantic indicators for both modes.
 */
public class ThemeToggleView extends View {

    public interface OnThemeChangeListener {
        void onThemeChanged(boolean isDark);
    }

    private boolean isDark = false;
    private float slideProgress = 0.0f; // 0.0f = Light, 1.0f = Dark
    private ValueAnimator animator;
    private OnThemeChangeListener listener;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF trackRect = new RectF();
    private final RectF thumbRect = new RectF();

    public ThemeToggleView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        float density = getResources().getDisplayMetrics().density;
        trackBorderPaint.setStyle(Paint.Style.STROKE);
        trackBorderPaint.setStrokeWidth(1.0f * density);

        thumbBorderPaint.setStyle(Paint.Style.STROKE);
        thumbBorderPaint.setStrokeWidth(1.0f * density);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public void setDarkMode(boolean isDark, boolean animate) {
        this.isDark = isDark;
        float target = isDark ? 1.0f : 0.0f;
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        if (animate && isAttachedToWindow()) {
            animator = ValueAnimator.ofFloat(slideProgress, target);
            animator.setDuration(220);
            animator.setInterpolator(new DecelerateInterpolator(1.6f));
            animator.addUpdateListener(anim -> {
                slideProgress = (Float) anim.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            slideProgress = target;
            invalidate();
        }
    }

    public void setOnThemeChangeListener(OnThemeChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        int defaultWidth = (int) (130 * density);
        int defaultHeight = (int) (32 * density);

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
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float cornerRadius = h / 2.0f;

        // 1. Draw Track
        int trackColor = isDark ? Color.parseColor("#0E121B") : Color.parseColor("#F1F5F9");
        int trackBorderColor = isDark ? Color.parseColor("#232C3F") : Color.parseColor("#E2E8F0");

        trackPaint.setColor(trackColor);
        trackPaint.setStyle(Paint.Style.FILL);
        trackRect.set(0, 0, w, h);
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint);

        trackBorderPaint.setColor(trackBorderColor);
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackBorderPaint);

        // 2. Draw Sliding Thumb
        float padding = 2.0f * density;
        float segmentWidth = (w - (padding * 2.0f)) / 2.0f;
        float thumbLeft = padding + (slideProgress * segmentWidth);
        float thumbRight = thumbLeft + segmentWidth;
        float thumbTop = padding;
        float thumbBottom = h - padding;
        float thumbRadius = (h - (padding * 2.0f)) / 2.0f;

        thumbRect.set(thumbLeft, thumbTop, thumbRight, thumbBottom);

        int thumbColor = isDark ? Color.parseColor("#1C2334") : Color.WHITE;
        int thumbBorder = isDark ? Color.parseColor("#2F3B54") : Color.parseColor("#CBD5E1");

        thumbPaint.setColor(thumbColor);
        thumbPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(thumbRect, thumbRadius, thumbRadius, thumbPaint);

        thumbBorderPaint.setColor(thumbBorder);
        canvas.drawRoundRect(thumbRect, thumbRadius, thumbRadius, thumbBorderPaint);

        // 3. Draw Segment Labels: "☀ Light" & "🌙 Dark"
        float textY = (h / 2.0f) - ((textPaint.descent() + textPaint.ascent()) / 2.0f);
        textPaint.setTextSize(11.0f * density);

        // Left Label: Light
        float leftCenterX = padding + (segmentWidth / 2.0f);
        if (slideProgress < 0.5f) {
            textPaint.setColor(Color.parseColor("#0F172A")); // Active
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            textPaint.setColor(isDark ? Color.parseColor("#64748B") : Color.parseColor("#94A3B8")); // Inactive
            textPaint.setTypeface(Typeface.DEFAULT);
        }
        canvas.drawText("☀ Light", leftCenterX, textY, textPaint);

        // Right Label: Dark
        float rightCenterX = padding + segmentWidth + (segmentWidth / 2.0f);
        if (slideProgress >= 0.5f) {
            textPaint.setColor(Color.parseColor("#FB7185")); // Active Rose accent on Obsidian dark
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            textPaint.setColor(Color.parseColor("#64748B")); // Inactive
            textPaint.setTypeface(Typeface.DEFAULT);
        }
        canvas.drawText("🌙 Dark", rightCenterX, textY, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            boolean newDark = x > (getWidth() / 2.0f);
            if (newDark != isDark) {
                try {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                } catch (Exception ignored) {}
                setDarkMode(newDark, true);
                if (listener != null) {
                    listener.onThemeChanged(newDark);
                }
            }
            performClick();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
