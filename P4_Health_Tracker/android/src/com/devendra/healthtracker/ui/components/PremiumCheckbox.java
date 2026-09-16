package com.devendra.healthtracker.ui.components;

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
import android.view.animation.OvershootInterpolator;

import com.devendra.healthtracker.ui.design.DesignTokens;
import com.devendra.healthtracker.ui.haptics.HapticEngine;

/**
 * Premium Soft-Tactile Checkbox.
 * Replaces standard generic Android checkbox with a refined circular checkmark
 * featuring smooth pop animation, tactile spring feedback, and semantic tinting.
 */
public class PremiumCheckbox extends View {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(PremiumCheckbox checkbox, boolean isChecked);
    }

    private boolean isChecked = false;
    private boolean isDark = false;
    private int accentColor = DesignTokens.ACCENT_EMERALD_PRIMARY;

    private float checkScale = 0.0f;
    private ValueAnimator checkAnimator;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();

    private OnCheckedChangeListener listener;

    public PremiumCheckbox(Context context) {
        super(context);
        init();
    }

    public PremiumCheckbox(Context context, boolean isDark) {
        super(context);
        this.isDark = isDark;
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);

        borderPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStyle(Paint.Style.FILL);

        checkPaint.setTextAlign(Paint.Align.CENTER);
        checkPaint.setTypeface(Typeface.DEFAULT_BOLD);
        checkPaint.setColor(Color.WHITE);

        checkScale = isChecked ? 1.0f : 0.0f;
    }

    public void setDarkTheme(boolean isDark) {
        this.isDark = isDark;
        invalidate();
    }

    public void setAccentColor(int accent) {
        this.accentColor = accent;
        invalidate();
    }

    public void setChecked(boolean checked, boolean animate) {
        if (this.isChecked == checked) return;
        this.isChecked = checked;
        if (animate) {
            animateCheck(checked);
        } else {
            checkScale = checked ? 1.0f : 0.0f;
            invalidate();
        }
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    private void animateCheck(boolean checked) {
        if (checkAnimator != null && checkAnimator.isRunning()) {
            checkAnimator.cancel();
        }
        checkAnimator = ValueAnimator.ofFloat(checkScale, checked ? 1.0f : 0.0f);
        checkAnimator.setDuration(180);
        checkAnimator.setInterpolator(new OvershootInterpolator(1.4f));
        checkAnimator.addUpdateListener(anim -> {
            checkScale = (Float) anim.getAnimatedValue();
            invalidate();
        });
        checkAnimator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (26 * density);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float radius = Math.min(w, h) / 2.0f;
        float strokeHalf = 1.0f * density;

        bounds.set(strokeHalf, strokeHalf, w - strokeHalf, h - strokeHalf);

        // 1. Draw Background
        if (isChecked || checkScale > 0.05f) {
            bgPaint.setColor(accentColor);
            bgPaint.setAlpha((int) (checkScale * 255));
            canvas.drawCircle(w / 2.0f, h / 2.0f, radius - strokeHalf, bgPaint);
        } else {
            bgPaint.setColor(isDark ? 0xFF1B2232 : 0xFFFFFFFF);
            canvas.drawCircle(w / 2.0f, h / 2.0f, radius - strokeHalf, bgPaint);
        }

        // 2. Draw Border
        borderPaint.setStrokeWidth(1.2f * density);
        if (isChecked || checkScale > 0.5f) {
            borderPaint.setColor(accentColor);
        } else {
            borderPaint.setColor(isDark ? 0xFF35425D : 0xFFCBD5E1);
        }
        canvas.drawCircle(w / 2.0f, h / 2.0f, radius - strokeHalf, borderPaint);

        // 3. Draw Checkmark Glyph "✓" with pop scaling
        if (checkScale > 0.1f) {
            canvas.save();
            canvas.scale(checkScale, checkScale, w / 2.0f, h / 2.0f);
            checkPaint.setTextSize(13 * density);
            float textY = (h / 2.0f) - ((checkPaint.descent() + checkPaint.ascent()) / 2.0f);
            canvas.drawText("✓", w / 2.0f, textY, checkPaint);
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            setChecked(!isChecked, true);
            HapticEngine.tick(this);
            if (listener != null) {
                listener.onCheckedChanged(this, isChecked);
            }
            playSoundEffect(android.view.SoundEffectConstants.CLICK);
            return true;
        }
        return true;
    }
}
