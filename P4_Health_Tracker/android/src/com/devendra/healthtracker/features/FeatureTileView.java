package com.devendra.healthtracker.features;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.devendra.healthtracker.theme.ThemeManager;

/**
 * Polished rectangular interactive button/tile representing an application capability.
 * Implements iOS-inspired tactile physics, procedural vector icon rendering,
 * jiggle edit state, reorder micro-chevrons, and adaptive dark/light mode surface hierarchy.
 */
public class FeatureTileView extends View {

    public interface OnTileClickListener {
        void onTileClicked(FeatureItem feature);
    }

    public interface OnTileLongClickListener {
        void onTileLongClicked(FeatureItem feature, FeatureTileView view, float rawX, float rawY);
    }

    private final FeatureItem feature;
    private final FeatureIconDrawer iconDrawer = new FeatureIconDrawer();

    private boolean isDark = false;
    private boolean isEditMode = false;
    private boolean isSelectedForSwap = false;
    private boolean isPressedDown = false;
    private boolean isBeingDragged = false;
    private boolean canMoveLeft = false;
    private boolean canMoveRight = false;
    private float pressScale = 1.0f;

    private OnTileClickListener clickListener;
    private OnTileLongClickListener longClickListener;

    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPrimaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textSecondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint editBadgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint editBadgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint navBtnBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint navBtnBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint navBtnTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF boundsRect = new RectF();
    private final RectF iconBgRect = new RectF();
    private final RectF iconGlyphRect = new RectF();
    private final RectF leftButtonRect = new RectF();
    private final RectF rightButtonRect = new RectF();

    private ValueAnimator pressAnimator;
    private ValueAnimator jiggleAnimator;
    private Runnable longPressRunnable;
    private boolean longPressTriggered = false;
    private float downRawX, downRawY;

    public FeatureTileView(Context context, FeatureItem feature, boolean isDark) {
        super(context);
        this.feature = feature;
        this.isDark = isDark;
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);

        float density = getResources().getDisplayMetrics().density;
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f * density);

        textPrimaryPaint.setTextAlign(Paint.Align.CENTER);
        textPrimaryPaint.setTypeface(Typeface.DEFAULT_BOLD);

        textSecondaryPaint.setTextAlign(Paint.Align.CENTER);
        textSecondaryPaint.setTypeface(Typeface.DEFAULT);

        editBadgePaint.setStyle(Paint.Style.FILL);
        editBadgeTextPaint.setTextAlign(Paint.Align.CENTER);
        editBadgeTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        editBadgeTextPaint.setColor(Color.WHITE);

        navBtnBgPaint.setStyle(Paint.Style.FILL);
        navBtnBorderPaint.setStyle(Paint.Style.STROKE);
        navBtnBorderPaint.setStrokeWidth(0.8f * density);

        navBtnTextPaint.setTextAlign(Paint.Align.CENTER);
        navBtnTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

        if (Build.VERSION.SDK_INT >= 21) {
            setElevation(2.0f * density);
        }

        setContentDescription(feature.getShortName() + ", " + feature.getSubtitle());
    }

    public FeatureItem getFeature() {
        return feature;
    }

    public void setTheme(boolean isDark) {
        this.isDark = isDark;
        invalidate();
    }

    public void setEditMode(boolean editMode, boolean canMoveLeft, boolean canMoveRight) {
        this.isEditMode = editMode;
        this.canMoveLeft = canMoveLeft;
        this.canMoveRight = canMoveRight;
        float density = getResources().getDisplayMetrics().density;
        if (Build.VERSION.SDK_INT >= 21) {
            setElevation(editMode ? 4.0f * density : 2.0f * density);
        }
        if (editMode) {
            startJiggle();
        } else {
            stopJiggle();
        }
        invalidate();
    }

    public void setEditMode(boolean editMode) {
        setEditMode(editMode, false, false);
    }

    public boolean isSelectedForSwap() {
        return isSelectedForSwap;
    }

    public void setSelectedForSwap(boolean selected) {
        if (this.isSelectedForSwap == selected) return;
        this.isSelectedForSwap = selected;
        float density = getResources().getDisplayMetrics().density;
        if (selected) {
            stopJiggle();
            animatePress(1.06f);
            if (Build.VERSION.SDK_INT >= 21) {
                setElevation(8.0f * density);
            }
        } else {
            animatePress(1.0f);
            if (Build.VERSION.SDK_INT >= 21) {
                setElevation(isEditMode ? 4.0f * density : 2.0f * density);
            }
            if (isEditMode) {
                startJiggle();
            }
        }
        invalidate();
    }

    public void setBeingDragged(boolean dragged) {
        this.isBeingDragged = dragged;
        float density = getResources().getDisplayMetrics().density;
        if (dragged) {
            stopJiggle();
            setAlpha(0.92f);
            setScaleX(1.08f);
            setScaleY(1.08f);
            if (Build.VERSION.SDK_INT >= 21) {
                setElevation(20.0f * density);
            }
        } else {
            setAlpha(1.0f);
            setScaleX(1.0f);
            setScaleY(1.0f);
            if (Build.VERSION.SDK_INT >= 21) {
                setElevation(isEditMode ? 4.0f * density : 2.0f * density);
            }
            if (isEditMode) {
                startJiggle();
            }
        }
        invalidate();
    }

    public boolean isLeftButtonHit(float localX, float localY) {
        return isEditMode && canMoveLeft && leftButtonRect.contains(localX, localY);
    }

    public boolean isRightButtonHit(float localX, float localY) {
        return isEditMode && canMoveRight && rightButtonRect.contains(localX, localY);
    }

    private void startJiggle() {
        if (jiggleAnimator != null) return;
        int hash = Math.abs(feature.getId().hashCode());
        float amp = 1.1f;
        float startA = (hash % 2 == 0) ? -amp : amp;
        jiggleAnimator = ValueAnimator.ofFloat(startA, -startA);
        jiggleAnimator.setDuration(150 + (hash % 50));
        jiggleAnimator.setRepeatMode(ValueAnimator.REVERSE);
        jiggleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        jiggleAnimator.addUpdateListener(anim -> {
            if (!isBeingDragged) {
                setRotation((Float) anim.getAnimatedValue());
            }
        });
        jiggleAnimator.start();
    }

    private void stopJiggle() {
        if (jiggleAnimator != null) {
            jiggleAnimator.cancel();
            jiggleAnimator = null;
        }
        setRotation(0f);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopJiggle();
        cancelLongPress();
    }

    public void setOnTileClickListener(OnTileClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnTileLongClickListener(OnTileLongClickListener listener) {
        this.longClickListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        int defaultHeight = (int) (100 * density);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

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
        float cornerRadius = 16.0f * density;

        // Apply tactile press scaling centered
        canvas.save();
        canvas.scale(pressScale, pressScale, w / 2.0f, h / 2.0f);

        // 1. Surface Background & Border (Obsidian Midnight Surface)
        int surfaceColor = isDark ? 0xFF141923 : Color.WHITE;
        int borderColor = isDark ? 0xFF232C3F : Color.parseColor("#E2E8F0");
        float strokeW = 1.0f * density;

        if (isSelectedForSwap) {
            surfaceColor = isDark ? 0xFF1E2238 : Color.parseColor("#EEF2FF"); // Soft Indigo tint
            borderColor = isDark ? Color.parseColor("#818CF8") : Color.parseColor("#4F46E5"); // Active Indigo glow ring
            strokeW = 2.0f * density;
        } else if (isEditMode) {
            borderColor = isDark ? Color.parseColor("#FB7185") : Color.parseColor("#E11D48"); // Rose highlight
            strokeW = 1.2f * density;
        } else if (isPressedDown) {
            surfaceColor = isDark ? 0xFF1F2636 : Color.parseColor("#F1F5F9");
        }

        surfacePaint.setColor(surfaceColor);
        surfacePaint.setStyle(Paint.Style.FILL);

        borderPaint.setStrokeWidth(strokeW);
        float halfStroke = strokeW / 2.0f;
        boundsRect.set(halfStroke, halfStroke, w - halfStroke, h - halfStroke);

        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, surfacePaint);

        borderPaint.setColor(borderColor);
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, borderPaint);

        // 2. Icon Container (34dp x 34dp centered horizontally near top)
        float iconBoxSize = 34.0f * density;
        float iconLeft = (w - iconBoxSize) / 2.0f;
        float iconTop = 11.0f * density;
        iconBgRect.set(iconLeft, iconTop, iconLeft + iconBoxSize, iconTop + iconBoxSize);

        int accentColor = Color.parseColor(feature.getAccentColorHex());
        int iconBgColor = Color.argb(isDark ? 45 : 25, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
        iconBgPaint.setColor(iconBgColor);
        iconBgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(iconBgRect, 12.0f * density, 12.0f * density, iconBgPaint);

        if (isDark) {
            // Delicate luminous hairline border around the icon container
            Paint iconBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            iconBorderPaint.setStyle(Paint.Style.STROKE);
            iconBorderPaint.setStrokeWidth(0.8f * density);
            iconBorderPaint.setColor(Color.argb(70, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
            canvas.drawRoundRect(iconBgRect, 12.0f * density, 12.0f * density, iconBorderPaint);
        }

        // 3. Draw Procedural Vector Icon inside container
        float glyphPadding = 7.0f * density;
        iconGlyphRect.set(
            iconBgRect.left + glyphPadding,
            iconBgRect.top + glyphPadding,
            iconBgRect.right - glyphPadding,
            iconBgRect.bottom - glyphPadding
        );

        int strokeColor = accentColor;
        iconDrawer.draw(canvas, feature.getId(), iconGlyphRect, strokeColor, density);

        // 4. Feature Short Label (Crisp Diamond White in Dark Mode)
        float textY = iconBgRect.bottom + 17.0f * density;
        textPrimaryPaint.setTextSize(12.0f * density);
        textPrimaryPaint.setColor(isDark ? 0xFFFFFFFF : Color.parseColor("#0F172A"));
        canvas.drawText(feature.getShortName(), w / 2.0f, textY, textPrimaryPaint);

        // 5. Feature Subtitle (Clear, readable Slate-400)
        float subY = textY + 12.0f * density;
        textSecondaryPaint.setTextSize(9.0f * density);
        textSecondaryPaint.setColor(isDark ? 0xFF94A3B8 : Color.parseColor("#64748B"));
        canvas.drawText(feature.getSubtitle(), w / 2.0f, subY, textSecondaryPaint);

        // 6. Edit Mode Controls (Swap Badges)
        if (isEditMode) {
            if (isSelectedForSwap) {
                // Active Selected Badge: "⇄ 1ST" pill
                float pillW = 46.0f * density;
                float pillH = 18.0f * density;
                float pillR = pillH / 2.0f;
                float pillRight = w - 6.0f * density;
                float pillTop = 6.0f * density;
                RectF pillRect = new RectF(pillRight - pillW, pillTop, pillRight, pillTop + pillH);

                editBadgePaint.setColor(isDark ? Color.parseColor("#6366F1") : Color.parseColor("#4F46E5"));
                canvas.drawRoundRect(pillRect, pillR, pillR, editBadgePaint);

                editBadgeTextPaint.setTextSize(9.0f * density);
                editBadgeTextPaint.setColor(Color.WHITE);
                float textYPos = pillRect.centerY() - ((editBadgeTextPaint.descent() + editBadgeTextPaint.ascent()) / 2.0f);
                canvas.drawText("⇄ 1ST", pillRect.centerX(), textYPos, editBadgeTextPaint);
            } else {
                // Subtle Swap Indicator Circle: "⇄"
                float badgeR = 9.0f * density;
                float bx = w - 12.0f * density;
                float by = 12.0f * density;
                editBadgePaint.setColor(isDark ? Color.parseColor("#F43F5E") : Color.parseColor("#E11D48"));
                canvas.drawCircle(bx, by, badgeR, editBadgePaint);

                editBadgeTextPaint.setTextSize(9.0f * density);
                editBadgeTextPaint.setColor(Color.WHITE);
                float badgeTextY = by - ((editBadgeTextPaint.descent() + editBadgeTextPaint.ascent()) / 2.0f);
                canvas.drawText("⇄", bx, badgeTextY, editBadgeTextPaint);
            }
        }

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressedDown = true;
                longPressTriggered = false;
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                animatePress(isSelectedForSwap ? 1.02f : 0.96f);

                if (longPressRunnable == null) {
                    longPressRunnable = () -> {
                        longPressTriggered = true;
                        animatePress(isSelectedForSwap ? 1.06f : 1.0f);
                        try {
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        } catch (Exception ignored) {}
                        if (longClickListener != null) {
                            longClickListener.onTileLongClicked(feature, FeatureTileView.this, downRawX, downRawY);
                        }
                    };
                }
                postDelayed(longPressRunnable, 380);
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getRawX() - downRawX);
                float dy = Math.abs(event.getRawY() - downRawY);
                float density = getResources().getDisplayMetrics().density;
                if (dx > 10.0f * density || dy > 10.0f * density) {
                    cancelLongPress();
                    if (isPressedDown) {
                        isPressedDown = false;
                        animatePress(isSelectedForSwap ? 1.06f : 1.0f);
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                cancelLongPress();
                if (isPressedDown) {
                    isPressedDown = false;
                    animatePress(isSelectedForSwap ? 1.06f : 1.0f);
                    if (!longPressTriggered) {
                        try {
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        } catch (Exception ignored) {}
                        if (clickListener != null) {
                            clickListener.onTileClicked(feature);
                        }
                        performClick();
                    }
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                cancelLongPress();
                isPressedDown = false;
                animatePress(isSelectedForSwap ? 1.06f : 1.0f);
                return true;
        }

        return super.onTouchEvent(event);
    }

    public void cancelLongPress() {
        if (longPressRunnable != null) {
            removeCallbacks(longPressRunnable);
        }
    }

    private void animatePress(float targetScale) {
        if (pressAnimator != null && pressAnimator.isRunning()) {
            pressAnimator.cancel();
        }
        pressAnimator = ValueAnimator.ofFloat(pressScale, targetScale);
        pressAnimator.setDuration(targetScale < 1.0f ? 100 : 180);
        pressAnimator.setInterpolator(targetScale < 1.0f ? new DecelerateInterpolator(1.8f) : new OvershootInterpolator(1.4f));
        pressAnimator.addUpdateListener(anim -> {
            pressScale = (Float) anim.getAnimatedValue();
            invalidate();
        });
        pressAnimator.start();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
