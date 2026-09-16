package com.devendra.healthtracker.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.devendra.healthtracker.ui.design.DesignTokens;

import java.util.ArrayList;
import java.util.List;

/**
 * Premium iOS-inspired Segmented Control.
 * Houses mutually exclusive options in a cohesive recessed track,
 * with elevated selected pill and tactile haptic response.
 */
public class PremiumSegmentedControl extends LinearLayout {

    public interface OnSegmentSelectedListener {
        void onSegmentSelected(int index, String tag);
    }

    private boolean isDark = false;
    private int selectedIndex = 0;
    private final List<TextView> segmentViews = new ArrayList<>();
    private final List<String> segmentTags = new ArrayList<>();
    private OnSegmentSelectedListener listener;
    private int accentColor = 0;

    public PremiumSegmentedControl(Context context) {
        super(context);
        init();
    }

    public PremiumSegmentedControl(Context context, boolean isDark) {
        super(context);
        this.isDark = isDark;
        init();
    }

    private void init() {
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (3 * density);
        setPadding(pad, pad, pad, pad);

        applyTrackBackground();
    }

    public void setDarkTheme(boolean isDark) {
        this.isDark = isDark;
        applyTrackBackground();
        updateSegmentsVisuals();
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
        updateSegmentsVisuals();
    }

    public void setOnSegmentSelectedListener(OnSegmentSelectedListener listener) {
        this.listener = listener;
    }

    public void addSegment(String label, String tag) {
        final int index = segmentViews.size();
        segmentTags.add(tag);

        TextView segView = new TextView(getContext());
        segView.setText(label);
        segView.setTextSize(11);
        segView.setGravity(Gravity.CENTER);
        segView.setSingleLine(true);
        segView.setClickable(true);
        segView.setFocusable(true);

        float density = getResources().getDisplayMetrics().density;
        int padV = (int) (7 * density);
        int padH = (int) (8 * density);
        segView.setPadding(padH, padV, padH, padV);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins((int) (1 * density), 0, (int) (1 * density), 0);
        segView.setLayoutParams(lp);

        segView.setOnClickListener(v -> selectSegment(index, true));

        segmentViews.add(segView);
        addView(segView);

        updateSegmentsVisuals();
    }

    public void selectSegment(int index, boolean notify) {
        if (index < 0 || index >= segmentViews.size()) return;
        this.selectedIndex = index;
        updateSegmentsVisuals();

        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        if (notify && listener != null) {
            listener.onSegmentSelected(index, segmentTags.get(index));
        }
    }

    public void selectByTag(String tag, boolean notify) {
        int idx = segmentTags.indexOf(tag);
        if (idx >= 0) {
            selectSegment(idx, notify);
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getSelectedTag() {
        if (selectedIndex >= 0 && selectedIndex < segmentTags.size()) {
            return segmentTags.get(selectedIndex);
        }
        return null;
    }

    private void applyTrackBackground() {
        float density = getResources().getDisplayMetrics().density;
        int radius = (int) (DesignTokens.RADIUS_MD * density);

        GradientDrawable track = new GradientDrawable();
        track.setColor(isDark ? 0xFF0E121B : 0xFFF1F5F9);
        track.setCornerRadius(radius);
        track.setStroke((int) (1.0f * density), isDark ? 0xFF232C3F : 0xFFE2E8F0);
        setBackground(track);
    }

    private void updateSegmentsVisuals() {
        float density = getResources().getDisplayMetrics().density;
        int radius = (int) ((DesignTokens.RADIUS_MD - 2) * density);

        for (int i = 0; i < segmentViews.size(); i++) {
            TextView seg = segmentViews.get(i);
            boolean isSelected = (i == selectedIndex);

            if (isSelected) {
                seg.setTypeface(Typeface.DEFAULT_BOLD);
                seg.setTextColor(isDark ? 0xFFFFFFFF : (accentColor != 0 ? accentColor : 0xFF0F172A));

                GradientDrawable pill = new GradientDrawable();
                pill.setColor(isDark ? 0xFF1C2334 : 0xFFFFFFFF);
                pill.setCornerRadius(radius);
                pill.setStroke((int) (0.8f * density), isDark ? 0xFF2F3B54 : 0xFFCBD5E1);
                seg.setBackground(pill);

                if (Build.VERSION.SDK_INT >= 21) {
                    seg.setElevation(1.5f * density);
                }
            } else {
                seg.setTypeface(Typeface.DEFAULT);
                seg.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
                seg.setBackground(null);
                if (Build.VERSION.SDK_INT >= 21) {
                    seg.setElevation(0);
                }
            }
        }
    }
}
