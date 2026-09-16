package com.devendra.healthtracker.features;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsive 3-column launcher grid with direct 2-card swapping mechanism.
 * Tapping any two cards seamlessly swaps their positions with fluid iOS-inspired motion.
 * Eliminates dragging gesture interference with parent scrolling while providing 100% reliable reordering.
 */
public class ReorderableFeatureGrid extends ViewGroup {

    public interface OnFeatureSelectedListener {
        void onFeatureSelected(FeatureItem feature);
    }

    public interface OnOrderChangeListener {
        void onOrderChanged(List<String> newOrder);
        void onEditModeChanged(boolean isEditing);
        default void onSwapSelectionChanged(FeatureItem selectedItem) {}
    }

    private static final int COLUMNS = 3;
    private final List<FeatureItem> items = new ArrayList<>();
    private final Map<String, FeatureTileView> tileViews = new HashMap<>();

    private boolean isDark = false;
    private boolean isEditMode = false;

    // Direct 2-card swapping state
    private FeatureTileView selectedTileForSwap = null;
    private boolean isAnimatingSwap = false;

    private OnFeatureSelectedListener featureListener;
    private OnOrderChangeListener orderListener;

    public ReorderableFeatureGrid(Context context) {
        super(context);
        init();
    }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setFeatureListener(OnFeatureSelectedListener listener) {
        this.featureListener = listener;
    }

    public void setOrderListener(OnOrderChangeListener listener) {
        this.orderListener = listener;
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public void setEditMode(boolean editMode) {
        if (this.isEditMode == editMode) return;
        this.isEditMode = editMode;

        if (!editMode && selectedTileForSwap != null) {
            selectedTileForSwap.setSelectedForSwap(false);
            selectedTileForSwap = null;
        }

        updateAllTileEditState();

        if (orderListener != null) {
            orderListener.onEditModeChanged(editMode);
            orderListener.onSwapSelectionChanged(null);
        }
    }

    public void updateAllTileEditState() {
        int count = items.size();
        for (int i = 0; i < count; i++) {
            FeatureItem item = items.get(i);
            FeatureTileView tv = tileViews.get(item.getId());
            if (tv != null) {
                tv.setEditMode(isEditMode, i > 0, i < count - 1);
            }
        }
    }

    public void setFeatures(List<FeatureItem> newItems, boolean isDark) {
        this.isDark = isDark;
        this.items.clear();
        this.items.addAll(newItems);
        this.tileViews.clear();
        this.selectedTileForSwap = null;
        removeAllViews();

        for (FeatureItem item : items) {
            FeatureTileView tile = new FeatureTileView(getContext(), item, isDark);
            tile.setOnTileClickListener(feature -> {
                if (isEditMode) {
                    handleTileSwapTap(tile);
                } else if (featureListener != null) {
                    featureListener.onFeatureSelected(feature);
                }
            });
            tile.setOnTileLongClickListener((feature, view, rawX, rawY) -> {
                handleTileLongPress(tile);
            });
            tileViews.put(item.getId(), tile);
            addView(tile);
        }

        updateAllTileEditState();
        requestLayout();
        invalidate();
    }

    public void handleTileSwapTap(FeatureTileView tile) {
        if (!isEditMode) return;
        if (isAnimatingSwap) return;

        if (selectedTileForSwap == null) {
            // First card selected!
            selectedTileForSwap = tile;
            tile.setSelectedForSwap(true);
            try {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            } catch (Exception ignored) {}
            if (orderListener != null) {
                orderListener.onSwapSelectionChanged(tile.getFeature());
            }
        } else if (selectedTileForSwap == tile) {
            // Tapped same card again -> deselect
            tile.setSelectedForSwap(false);
            selectedTileForSwap = null;
            try {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            } catch (Exception ignored) {}
            if (orderListener != null) {
                orderListener.onSwapSelectionChanged(null);
            }
        } else {
            // Tapped second card -> SWAP!
            FeatureTileView card1 = selectedTileForSwap;
            card1.setSelectedForSwap(false);
            selectedTileForSwap = null;

            int indexA = getFeatureIndexForView(card1);
            int indexB = getFeatureIndexForView(tile);

            if (indexA >= 0 && indexB >= 0 && indexA != indexB) {
                swapItems(indexA, indexB);
            }
            if (orderListener != null) {
                orderListener.onSwapSelectionChanged(null);
            }
        }
    }

    public void handleTileLongPress(FeatureTileView tile) {
        if (!isEditMode) {
            setEditMode(true);
        }
        handleTileSwapTap(tile);
    }

    public void setTheme(boolean isDark) {
        this.isDark = isDark;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof FeatureTileView) {
                ((FeatureTileView) child).setTheme(isDark);
            }
        }
    }

    public List<String> getCurrentOrder() {
        List<String> order = new ArrayList<>();
        for (FeatureItem item : items) {
            order.add(item.getId());
        }
        return order;
    }

    private int getSlotWidth() {
        float density = getResources().getDisplayMetrics().density;
        int totalWidth = getWidth();
        if (totalWidth <= 0) totalWidth = getResources().getDisplayMetrics().widthPixels;

        int maxGridWidth = (int) (520 * density);
        int contentWidth = Math.min(totalWidth, maxGridWidth);
        int hPadding = (int) (14 * density);
        int hGap = (int) (10 * density);
        int totalContentWidth = contentWidth - (hPadding * 2);
        return Math.max(1, (totalContentWidth - ((COLUMNS - 1) * hGap)) / COLUMNS);
    }

    private Point calculatePositionForSlot(int slotIndex) {
        float density = getResources().getDisplayMetrics().density;
        int totalWidth = getWidth();
        if (totalWidth <= 0) totalWidth = getResources().getDisplayMetrics().widthPixels;

        int maxGridWidth = (int) (520 * density);
        int contentWidth = Math.min(totalWidth, maxGridWidth);
        int startOffset = (totalWidth - contentWidth) / 2;

        int hPadding = (int) (14 * density) + startOffset;
        int vPadding = (int) (10 * density);
        int hGap = (int) (10 * density);
        int vGap = (int) (10 * density);

        int childW = getSlotWidth();
        int childH = (int) (100 * density);

        int col = slotIndex % COLUMNS;
        int row = slotIndex / COLUMNS;

        int left = hPadding + col * (childW + hGap);
        int top = vPadding + row * (childH + vGap);

        return new Point(left, top);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int tileWidth = getSlotWidth();
        int tileHeight = (int) (100 * density);

        int count = items.size();
        int rows = (count + COLUMNS - 1) / COLUMNS;
        if (rows <= 0) rows = 1;

        int vPadding = (int) (10 * density);
        int vGap = (int) (10 * density);
        int totalHeight = (vPadding * 2) + (rows * tileHeight) + (Math.max(0, rows - 1) * vGap);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(
                MeasureSpec.makeMeasureSpec(tileWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(tileHeight, MeasureSpec.EXACTLY)
            );
        }

        setMeasuredDimension(widthSize, totalHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < items.size(); i++) {
            FeatureItem item = items.get(i);
            View child = tileViews.get(item.getId());
            if (child == null) continue;

            Point pos = calculatePositionForSlot(i);
            int childW = child.getMeasuredWidth();
            int childH = child.getMeasuredHeight();

            child.layout(pos.x, pos.y, pos.x + childW, pos.y + childH);
        }
    }

    public void swapItems(int fromIndex, int toIndex) {
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0 ||
            fromIndex >= items.size() || toIndex >= items.size()) return;
        if (isAnimatingSwap) return;

        isAnimatingSwap = true;
        FeatureItem itemA = items.get(fromIndex);
        FeatureItem itemB = items.get(toIndex);
        FeatureTileView viewA = tileViews.get(itemA.getId());
        FeatureTileView viewB = tileViews.get(itemB.getId());

        if (viewA == null || viewB == null) {
            Collections.swap(items, fromIndex, toIndex);
            requestLayout();
            updateAllTileEditState();
            saveCurrentOrder();
            isAnimatingSwap = false;
            return;
        }

        Point posA = calculatePositionForSlot(fromIndex);
        Point posB = calculatePositionForSlot(toIndex);

        float transAX = posB.x - viewA.getLeft();
        float transAY = posB.y - viewA.getTop();
        float transBX = posA.x - viewB.getLeft();
        float transBY = posA.y - viewB.getTop();

        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        viewA.bringToFront();
        viewB.bringToFront();

        viewA.animate()
            .translationX(transAX)
            .translationY(transAY)
            .scaleX(1.05f).scaleY(1.05f)
            .setDuration(240)
            .setInterpolator(new DecelerateInterpolator(1.8f))
            .start();

        viewB.animate()
            .translationX(transBX)
            .translationY(transBY)
            .scaleX(1.05f).scaleY(1.05f)
            .setDuration(240)
            .setInterpolator(new DecelerateInterpolator(1.8f))
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Collections.swap(items, fromIndex, toIndex);

                    viewA.setScaleX(1.0f);
                    viewA.setScaleY(1.0f);
                    viewB.setScaleX(1.0f);
                    viewB.setScaleY(1.0f);

                    viewA.setTranslationX(0);
                    viewA.setTranslationY(0);
                    viewB.setTranslationX(0);
                    viewB.setTranslationY(0);

                    requestLayout();
                    updateAllTileEditState();
                    saveCurrentOrder();
                    isAnimatingSwap = false;
                }
            })
            .start();
    }

    private int getFeatureIndexForView(View v) {
        for (int i = 0; i < items.size(); i++) {
            if (tileViews.get(items.get(i).getId()) == v) {
                return i;
            }
        }
        return -1;
    }

    private void saveCurrentOrder() {
        List<String> order = getCurrentOrder();
        FeatureOrderManager.saveOrder(getContext(), order);
        if (orderListener != null) {
            orderListener.onOrderChanged(order);
        }
    }
}
