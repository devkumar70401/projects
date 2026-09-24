package com.devendra.healthtracker.ui.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.devendra.healthtracker.features.FeatureItem;
import com.devendra.healthtracker.features.FeatureOrderManager;
import com.devendra.healthtracker.features.FeatureRegistry;
import com.devendra.healthtracker.ui.design.DesignTokens;
import com.devendra.healthtracker.ui.elements.ElementalBorderAnimationView;
import com.devendra.healthtracker.ui.elements.ElementType;
import com.devendra.healthtracker.ui.haptics.HapticEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * FluidBubbleLauncherOverlay
 * Full-screen fluid organic bubble launcher displaying the 11 system capabilities.
 * Opened by swiping from bottom to top on the Home Screen.
 * Features:
 * - Dynamic elemental animated borders on every bubble (Bubbling, Fire, Thunder, Air, Volcano).
 * - Organic buoyant floating physics with tactile haptic response.
 * - Swipe-down to dismiss, back button dismissal, and instant capability launch.
 */
public class FluidBubbleLauncherOverlay extends FrameLayout {

    public interface OnBubbleSelectedListener {
        void onBubbleSelected(String featureId);
        void onDismissed();
    }

    private OnBubbleSelectedListener listener;
    private boolean isDarkTheme = true;
    private boolean isOpen = false;

    private LinearLayout sheetContent;
    private ScrollView bubbleScrollView;
    private final List<BubbleViewItem> bubbleViews = new ArrayList<>();

    // Buoyant floating animator
    private ValueAnimator floatAnimator;

    // Gesture detection for swiping down to dismiss
    private float startY;
    private float startX;
    private boolean isSwipingDown = false;
    private final int touchSlop;
    private final int minSwipeDist;

    public FluidBubbleLauncherOverlay(Context context, boolean isDarkTheme) {
        super(context);
        this.isDarkTheme = isDarkTheme;
        ViewConfiguration vc = ViewConfiguration.get(context);
        this.touchSlop = vc.getScaledTouchSlop();
        this.minSwipeDist = (int) (context.getResources().getDisplayMetrics().density * 60);

        init();
    }

    private void init() {
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setVisibility(GONE);
        setClickable(true);

        // Deep frosted atmospheric scrim backdrop
        setBackgroundColor(isDarkTheme ? Color.argb(230, 8, 10, 18) : Color.argb(235, 248, 250, 252));

        sheetContent = new LinearLayout(getContext());
        sheetContent.setOrientation(LinearLayout.VERTICAL);
        sheetContent.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sheetContent.setPadding(dp(16), dp(16), dp(16), dp(20));

        // 1. Top Drag Handle Pill
        View dragHandle = new View(getContext());
        LinearLayout.LayoutParams dhLp = new LinearLayout.LayoutParams(dp(44), dp(4));
        dhLp.gravity = Gravity.CENTER_HORIZONTAL;
        dhLp.setMargins(0, dp(6), 0, dp(12));
        dragHandle.setLayoutParams(dhLp);
        GradientDrawable dhBg = new GradientDrawable();
        dhBg.setColor(isDarkTheme ? Color.argb(120, 255, 255, 255) : Color.argb(80, 0, 0, 0));
        dhBg.setCornerRadius(dp(3));
        dragHandle.setBackground(dhBg);
        sheetContent.addView(dragHandle);

        // 2. Header Bar: Title, Subtitle, Close Button
        LinearLayout headerBar = new LinearLayout(getContext());
        headerBar.setOrientation(LinearLayout.HORIZONTAL);
        headerBar.setGravity(Gravity.CENTER_VERTICAL);
        headerBar.setPadding(dp(6), 0, dp(6), dp(14));

        LinearLayout titleCol = new LinearLayout(getContext());
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleCol.setLayoutParams(tcLp);

        TextView titleView = new TextView(getContext());
        titleView.setText("✨ SYSTEM CAPABILITIES");
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(isDarkTheme ? Color.WHITE : Color.parseColor("#0F172A"));
        titleView.setShadowLayer(dp(4), 0, dp(1), isDarkTheme ? Color.BLACK : Color.argb(120, 255, 255, 255));
        titleCol.addView(titleView);

        TextView subView = new TextView(getContext());
        subView.setText("11 Fluid Elemental Orbs • Tap to engage");
        subView.setTextSize(12);
        subView.setTextColor(isDarkTheme ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
        titleCol.addView(subView);

        headerBar.addView(titleCol);

        // Close Button [✕]
        PremiumIconButton closeBtn = new PremiumIconButton(getContext(), "✕", isDarkTheme);
        closeBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        closeBtn.setOnClickListener(v -> {
            HapticEngine.tap(v);
            dismiss();
        });
        headerBar.addView(closeBtn);

        sheetContent.addView(headerBar);

        // 3. Scrollable Fluid Bubbles Grid
        bubbleScrollView = new ScrollView(getContext());
        bubbleScrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        bubbleScrollView.setVerticalScrollBarEnabled(false);
        bubbleScrollView.setFillViewport(true);

        GridLayout bubbleGrid = new GridLayout(getContext());
        bubbleGrid.setColumnCount(2);
        bubbleGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        bubbleGrid.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        List<String> order = FeatureOrderManager.getOrder(getContext());
        List<FeatureItem> features = new java.util.ArrayList<>();
        for (String id : order) {
            FeatureItem fi = FeatureRegistry.getFeature(id);
            if (fi != null) features.add(fi);
        }
        for (int i = 0; i < features.size(); i++) {
            FeatureItem item = features.get(i);
            ElementType element = ElementType.forFeature(item.getId());

            BubbleViewItem bubbleItem = new BubbleViewItem(getContext(), item, element, isDarkTheme, i);
            bubbleViews.add(bubbleItem);

            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            glp.columnSpec = GridLayout.spec(i % 2, 1, 1.0f);
            glp.rowSpec = GridLayout.spec(i / 2, 1);
            glp.setMargins(dp(6), dp(6), dp(6), dp(6));
            bubbleItem.setLayoutParams(glp);

            bubbleGrid.addView(bubbleItem);
        }

        bubbleScrollView.addView(bubbleGrid);
        sheetContent.addView(bubbleScrollView);

        addView(sheetContent);
        setupFloatingAnimation();
    }

    private void setupFloatingAnimation() {
        floatAnimator = ValueAnimator.ofFloat(0f, 1f);
        floatAnimator.setDuration(3500);
        floatAnimator.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimator.addUpdateListener(animation -> {
            if (!isOpen) return;
            float time = (SystemClock.uptimeMillis() % 100000) / 1000f;
            for (BubbleViewItem item : bubbleViews) {
                item.applyFloatingOffset(time);
            }
        });
    }

    public void setOnBubbleSelectedListener(OnBubbleSelectedListener l) {
        this.listener = l;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void show() {
        if (isOpen) return;
        isOpen = true;
        setVisibility(VISIBLE);
        setAlpha(0f);
        setTranslationY(getHeight() > 0 ? getHeight() : dp(600));

        HapticEngine.swipeSuccess(getContext());

        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(new DecelerateInterpolator(1.8f))
            .withEndAction(() -> {
                if (floatAnimator != null && !floatAnimator.isRunning()) {
                    floatAnimator.start();
                }
            })
            .start();

        // Staggered bounce bloom for child bubbles
        for (int i = 0; i < bubbleViews.size(); i++) {
            BubbleViewItem item = bubbleViews.get(i);
            item.setScaleX(0.75f);
            item.setScaleY(0.75f);
            item.setAlpha(0f);
            item.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(320)
                .setStartDelay(i * 30L)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .start();
        }
    }

    public void dismiss() {
        if (!isOpen) return;
        isOpen = false;
        if (floatAnimator != null && floatAnimator.isRunning()) {
            floatAnimator.cancel();
        }

        animate()
            .alpha(0f)
            .translationY(getHeight() > 0 ? getHeight() : dp(600))
            .setDuration(240)
            .setInterpolator(new DecelerateInterpolator())
            .withEndAction(() -> {
                setVisibility(GONE);
                if (listener != null) {
                    listener.onDismissed();
                }
            })
            .start();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                isSwipingDown = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = ev.getX() - startX;
                float dy = ev.getY() - startY;
                // Detect deliberate swipe down from near top or when scroll is at 0
                if (dy > touchSlop && dy > Math.abs(dx) * 1.5f) {
                    if (bubbleScrollView != null && bubbleScrollView.getScrollY() == 0) {
                        isSwipingDown = true;
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isSwipingDown = false;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isSwipingDown) {
            if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
                float dy = ev.getY() - startY;
                if (dy > minSwipeDist) {
                    HapticEngine.swipeSuccess(this);
                    dismiss();
                    return true;
                }
                isSwipingDown = false;
            }
            return true;
        }
        return super.onTouchEvent(ev);
    }

    private int dp(float val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    // =========================================================================
    // INNER CLASS: BubbleViewItem
    // Individual floating fluid bubble with dynamic elemental animated border
    // =========================================================================
    private class BubbleViewItem extends FrameLayout {

        private final FeatureItem feature;
        private final ElementType element;
        private final int itemIndex;
        private final ElementalBorderAnimationView borderAnimView;
        private final LinearLayout innerContent;
        private ValueAnimator pressAnimator;

        public BubbleViewItem(Context context, FeatureItem feature, ElementType element, boolean isDark, int index) {
            super(context);
            this.feature = feature;
            this.element = element;
            this.itemIndex = index;

            setClickable(true);
            setFocusable(true);

            // 1. Frosted Glass Bubble Background
            GradientDrawable bubbleBg = new GradientDrawable();
            bubbleBg.setColor(isDark ? Color.argb(175, 20, 24, 38) : Color.argb(215, 255, 255, 255));
            bubbleBg.setCornerRadius(dp(22));
            bubbleBg.setStroke(dp(1), Color.argb(60, 255, 255, 255));
            setBackground(bubbleBg);

            // 2. Animated Elemental Border View (Fire, Thunder, Air, Bubbling, Volcano)
            borderAnimView = new ElementalBorderAnimationView(context, element, isDark);
            borderAnimView.setCornerRadiusDp(22);
            borderAnimView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            addView(borderAnimView);

            // 3. Foreground Typography & Badge Content
            innerContent = new LinearLayout(context);
            innerContent.setOrientation(LinearLayout.VERTICAL);
            innerContent.setPadding(dp(14), dp(14), dp(14), dp(14));
            innerContent.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Element Badge Pill
            TextView badge = new TextView(context);
            badge.setText(element.getDisplayName().toUpperCase());
            badge.setTextSize(9);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            badge.setTextColor(element.getAccentColor());
            badge.setPadding(dp(8), dp(2), dp(8), dp(2));
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(Color.argb(55, Color.red(element.getPrimaryColor()), Color.green(element.getPrimaryColor()), Color.blue(element.getPrimaryColor())));
            badgeBg.setCornerRadius(dp(10));
            badge.setBackground(badgeBg);
            innerContent.addView(badge);

            // Title
            TextView titleView = new TextView(context);
            titleView.setText(feature.getShortName());
            titleView.setTextSize(14);
            titleView.setTypeface(Typeface.DEFAULT_BOLD);
            titleView.setTextColor(isDark ? Color.WHITE : Color.parseColor("#0F172A"));
            titleView.setPadding(0, dp(6), 0, dp(2));
            innerContent.addView(titleView);

            // Subtitle
            TextView subView = new TextView(context);
            subView.setText(feature.getSubtitle());
            subView.setTextSize(11);
            subView.setTextColor(isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
            innerContent.addView(subView);

            addView(innerContent);

            // Tactile Touch Interactions with custom Bubble Pop & Elemental Haptics
            setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        HapticEngine.bubblePop(v);
                        HapticEngine.elementalPulse(getContext(), element.getId());
                        animateScale(0.92f, 120, new DecelerateInterpolator());
                        return true;

                    case MotionEvent.ACTION_UP:
                        animateScale(1.0f, 200, new OvershootInterpolator(1.4f));
                        HapticEngine.tap(v);
                        if (listener != null) {
                            postDelayed(() -> {
                                dismiss();
                                listener.onBubbleSelected(feature.getId());
                            }, 120);
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        animateScale(1.0f, 180, new OvershootInterpolator());
                        return true;
                }
                return false;
            });
        }

        public void applyFloatingOffset(float time) {
            float phase = itemIndex * 0.55f;
            float offsetY = (float) Math.sin(time * 2.2f + phase) * dp(3.5f);
            float offsetX = (float) Math.cos(time * 1.6f + phase) * dp(1.8f);
            setTranslationY(offsetY);
            setTranslationX(offsetX);
        }

        private void animateScale(float toScale, int duration, android.animation.TimeInterpolator interpolator) {
            if (pressAnimator != null && pressAnimator.isRunning()) {
                pressAnimator.cancel();
            }
            pressAnimator = ValueAnimator.ofFloat(getScaleX(), toScale);
            pressAnimator.setDuration(duration);
            pressAnimator.setInterpolator(interpolator);
            pressAnimator.addUpdateListener(anim -> {
                float val = (Float) anim.getAnimatedValue();
                setScaleX(val);
                setScaleY(val);
            });
            pressAnimator.start();
        }
    }
}
