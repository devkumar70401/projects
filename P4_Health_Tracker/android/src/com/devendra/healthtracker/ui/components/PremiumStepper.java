package com.devendra.healthtracker.ui.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.devendra.healthtracker.ui.design.DesignTokens;

/**
 * Premium iOS-inspired Stepper Control.
 * Houses decrement (−) and increment (+) controls inside a unified pill track
 * with generous touch targets and tactile haptic response.
 */
public class PremiumStepper extends LinearLayout {

    public interface OnStepperActionListener {
        void onMinus();
        void onPlus();
    }

    private boolean isDark = false;
    private PremiumIconButton minusBtn;
    private PremiumIconButton plusBtn;
    private TextView labelView;
    private OnStepperActionListener listener;
    private int accentColor = DesignTokens.ACCENT_SKY_PRIMARY;

    public PremiumStepper(Context context) {
        super(context);
        init();
    }

    public PremiumStepper(Context context, boolean isDark) {
        super(context);
        this.isDark = isDark;
        init();
    }

    private void init() {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (4 * density);
        setPadding(pad, pad, pad, pad);

        // 1. Minus button
        minusBtn = new PremiumIconButton(getContext(), "−", isDark);
        minusBtn.setTextSize(16);
        minusBtn.setCircular(false);
        minusBtn.setOnClickListener(v -> {
            if (listener != null) listener.onMinus();
        });
        LinearLayout.LayoutParams mbLp = new LinearLayout.LayoutParams((int) (44 * density), (int) (36 * density));
        minusBtn.setLayoutParams(mbLp);
        addView(minusBtn);

        // 2. Middle Label
        labelView = new TextView(getContext());
        labelView.setGravity(Gravity.CENTER);
        labelView.setTextSize(13);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        labelView.setLayoutParams(lblLp);
        addView(labelView);

        // 3. Plus button
        plusBtn = new PremiumIconButton(getContext(), "+", isDark);
        plusBtn.setTextSize(16);
        plusBtn.setCircular(false);
        plusBtn.setOnClickListener(v -> {
            if (listener != null) listener.onPlus();
        });
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams((int) (44 * density), (int) (36 * density));
        plusBtn.setLayoutParams(pbLp);
        addView(plusBtn);

        applyStyle();
    }

    public void setDarkTheme(boolean isDark) {
        this.isDark = isDark;
        if (minusBtn != null) minusBtn.setDarkTheme(isDark);
        if (plusBtn != null) plusBtn.setDarkTheme(isDark);
        applyStyle();
    }

    public void setAccentColor(int accent) {
        this.accentColor = accent;
        if (plusBtn != null) plusBtn.setCustomAccent(accent);
        applyStyle();
    }

    public void setLabel(String text) {
        if (labelView != null) {
            labelView.setText(text);
        }
    }

    public void setOnStepperActionListener(OnStepperActionListener listener) {
        this.listener = listener;
    }

    private void applyStyle() {
        float density = getResources().getDisplayMetrics().density;
        int radius = (int) (DesignTokens.RADIUS_MD * density);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isDark ? 0xFF141923 : 0xFFF8FAFC);
        bg.setCornerRadius(radius);
        bg.setStroke((int) (1.0f * density), isDark ? 0xFF232C3F : 0xFFCBD5E1);
        setBackground(bg);

        if (labelView != null) {
            labelView.setTextColor(isDark ? 0xFFFFFFFF : 0xFF0F172A);
        }
    }
}
