package com.devendra.healthtracker.features;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * High-precision procedural vector icon engine.
 * Renders consistent, optically balanced line-art glyphs across all display densities
 * with zero bitmap memory overhead and 100% vector fidelity.
 */
public class FeatureIconDrawer {

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public FeatureIconDrawer() {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    public void draw(Canvas canvas, String featureId, RectF bounds, int strokeColor, float density) {
        strokePaint.setColor(strokeColor);
        strokePaint.setStrokeWidth(2.0f * density);
        fillPaint.setColor(strokeColor);

        float cx = bounds.centerX();
        float cy = bounds.centerY();
        float s = Math.min(bounds.width(), bounds.height());
        float r = s / 2.0f;

        switch (featureId) {
            case FeatureRegistry.ID_CHECKLIST:
                drawChecklist(canvas, bounds, density);
                break;
            case FeatureRegistry.ID_ROUTINE:
                drawRoutine(canvas, bounds, density);
                break;
            case FeatureRegistry.ID_POMODORO:
                drawPomodoro(canvas, bounds, density);
                break;
            case FeatureRegistry.ID_WELLBEING:
                drawWellbeing(canvas, bounds, density);
                break;
            case FeatureRegistry.ID_SYSMON:
                drawSysmon(canvas, bounds, density);
                break;
            case FeatureRegistry.ID_DEEPCLEAN:
                drawDeepClean(canvas, bounds, density);
                break;
            case FeatureRegistry.ID_HYDRATION:
                drawHydration(canvas, bounds, density);
                break;
            case FeatureRegistry.ID_BLUEPRINT:
                drawBlueprint(canvas, bounds, density);
                break;
            case FeatureRegistry.ID_MEDIA:
                drawDownloader(canvas, bounds, density);
                break;
            case FeatureRegistry.ID_CALCULATOR:
                drawCalculator(canvas, bounds, density);
                break;
            default:
                drawGeneric(canvas, bounds, density);
                break;
        }
    }

    // 1. Checklist: Checkmark + 3 horizontal task bars
    private void drawChecklist(Canvas canvas, RectF b, float density) {
        path.reset();
        // Checkmark
        path.moveTo(b.left + b.width() * 0.16f, b.top + b.height() * 0.35f);
        path.lineTo(b.left + b.width() * 0.30f, b.top + b.height() * 0.50f);
        path.lineTo(b.left + b.width() * 0.46f, b.top + b.height() * 0.22f);
        canvas.drawPath(path, strokePaint);

        // Lines
        canvas.drawLine(b.left + b.width() * 0.56f, b.top + b.height() * 0.32f, b.right - b.width() * 0.12f, b.top + b.height() * 0.32f, strokePaint);
        canvas.drawLine(b.left + b.width() * 0.20f, b.top + b.height() * 0.58f, b.right - b.width() * 0.12f, b.top + b.height() * 0.58f, strokePaint);
        canvas.drawLine(b.left + b.width() * 0.20f, b.top + b.height() * 0.82f, b.right - b.width() * 0.28f, b.top + b.height() * 0.82f, strokePaint);
    }

    // 2. Routine: Circadian Sun/Clock Cadence
    private void drawRoutine(Canvas canvas, RectF b, float density) {
        float cx = b.centerX();
        float cy = b.centerY();
        float r = b.width() * 0.38f;
        canvas.drawCircle(cx, cy, r, strokePaint);

        // Hands pointing to 6:00 / 7:00
        canvas.drawLine(cx, cy, cx, cy - r * 0.55f, strokePaint); // Hour hand up
        canvas.drawLine(cx, cy, cx + r * 0.45f, cy + r * 0.25f, strokePaint); // Minute hand angled

        // Small 4 compass ticks
        canvas.drawLine(cx, b.top + b.height() * 0.05f, cx, b.top + b.height() * 0.14f, strokePaint);
        canvas.drawLine(cx, b.bottom - b.height() * 0.14f, cx, b.bottom - b.height() * 0.05f, strokePaint);
        canvas.drawLine(b.left + b.width() * 0.05f, cy, b.left + b.width() * 0.14f, cy, strokePaint);
        canvas.drawLine(b.right - b.width() * 0.14f, cy, b.right - b.width() * 0.05f, cy, strokePaint);
    }

    // 3. Pomodoro: Stopwatch with top pushers
    private void drawPomodoro(Canvas canvas, RectF b, float density) {
        float cx = b.centerX();
        float cy = b.top + b.height() * 0.56f;
        float r = b.width() * 0.38f;
        canvas.drawCircle(cx, cy, r, strokePaint);

        // Top Crown stem & bar
        canvas.drawLine(cx, cy - r, cx, b.top + b.height() * 0.08f, strokePaint);
        canvas.drawLine(cx - 3.5f * density, b.top + b.height() * 0.08f, cx + 3.5f * density, b.top + b.height() * 0.08f, strokePaint);

        // Angled side pusher
        canvas.drawLine(cx + r * 0.65f, cy - r * 0.75f, cx + r * 0.85f, cy - r * 0.95f, strokePaint);

        // Timer Hand
        canvas.drawLine(cx, cy, cx + r * 0.5f, cy - r * 0.4f, strokePaint);
    }

    // 4. Wellbeing: Analytics Bar Chart
    private void drawWellbeing(Canvas canvas, RectF b, float density) {
        float base = b.bottom - b.height() * 0.16f;
        float barW = b.width() * 0.14f;

        // 4 Vertical Bars
        float[] heights = {0.35f, 0.70f, 0.50f, 0.90f};
        for (int i = 0; i < 4; i++) {
            float x = b.left + b.width() * 0.14f + i * (barW + b.width() * 0.06f);
            float h = b.height() * 0.68f * heights[i];
            RectF barRect = new RectF(x, base - h, x + barW, base);
            canvas.drawRoundRect(barRect, 2.5f * density, 2.5f * density, strokePaint);
        }
    }

    // 5. System Monitor: Microchip CPU with circuit pins
    private void drawSysmon(Canvas canvas, RectF b, float density) {
        float cx = b.centerX();
        float cy = b.centerY();
        float boxHalf = b.width() * 0.28f;
        RectF chip = new RectF(cx - boxHalf, cy - boxHalf, cx + boxHalf, cy + boxHalf);
        canvas.drawRoundRect(chip, 3.5f * density, 3.5f * density, strokePaint);

        // Pins Top & Bottom
        canvas.drawLine(cx - boxHalf * 0.5f, b.top + b.height() * 0.08f, cx - boxHalf * 0.5f, chip.top, strokePaint);
        canvas.drawLine(cx + boxHalf * 0.5f, b.top + b.height() * 0.08f, cx + boxHalf * 0.5f, chip.top, strokePaint);
        canvas.drawLine(cx - boxHalf * 0.5f, chip.bottom, cx - boxHalf * 0.5f, b.bottom - b.height() * 0.08f, strokePaint);
        canvas.drawLine(cx + boxHalf * 0.5f, chip.bottom, cx + boxHalf * 0.5f, b.bottom - b.height() * 0.08f, strokePaint);

        // Pins Left & Right
        canvas.drawLine(b.left + b.width() * 0.08f, cy - boxHalf * 0.5f, chip.left, cy - boxHalf * 0.5f, strokePaint);
        canvas.drawLine(b.left + b.width() * 0.08f, cy + boxHalf * 0.5f, chip.left, cy + boxHalf * 0.5f, strokePaint);
        canvas.drawLine(chip.right, cy - boxHalf * 0.5f, b.right - b.width() * 0.08f, cy - boxHalf * 0.5f, strokePaint);
        canvas.drawLine(chip.right, cy + boxHalf * 0.5f, b.right - b.width() * 0.08f, cy + boxHalf * 0.5f, strokePaint);

        // Center Activity Dot
        canvas.drawCircle(cx, cy, 2.5f * density, fillPaint);
    }

    // 6. Deep Cleaner: Sparkle Star + Sweeping Broom
    private void drawDeepClean(Canvas canvas, RectF b, float density) {
        // Sparkle Star (top right)
        float sx = b.right - b.width() * 0.28f;
        float sy = b.top + b.height() * 0.25f;
        float sr = b.width() * 0.16f;
        canvas.drawLine(sx, sy - sr, sx, sy + sr, strokePaint);
        canvas.drawLine(sx - sr, sy, sx + sr, sy, strokePaint);

        // Broom Handle & Brush
        path.reset();
        path.moveTo(b.left + b.width() * 0.20f, b.top + b.height() * 0.20f);
        path.lineTo(b.left + b.width() * 0.52f, b.top + b.height() * 0.55f);
        canvas.drawPath(path, strokePaint);

        // Broom Head
        path.reset();
        path.moveTo(b.left + b.width() * 0.42f, b.top + b.height() * 0.64f);
        path.lineTo(b.left + b.width() * 0.60f, b.top + b.height() * 0.48f);
        path.lineTo(b.left + b.width() * 0.78f, b.top + b.height() * 0.78f);
        path.lineTo(b.left + b.width() * 0.50f, b.bottom - b.height() * 0.12f);
        path.close();
        canvas.drawPath(path, strokePaint);
    }

    // 7. Hydration: Water Droplet
    private void drawHydration(Canvas canvas, RectF b, float density) {
        float cx = b.centerX();
        path.reset();
        path.moveTo(cx, b.top + b.height() * 0.14f);
        // Left cubic curve down to bottom lobe
        path.cubicTo(
            b.left + b.width() * 0.18f, b.top + b.height() * 0.50f,
            b.left + b.width() * 0.20f, b.bottom - b.height() * 0.14f,
            cx, b.bottom - b.height() * 0.14f
        );
        // Right cubic curve back up to tip
        path.cubicTo(
            b.right - b.width() * 0.20f, b.bottom - b.height() * 0.14f,
            b.right - b.width() * 0.18f, b.top + b.height() * 0.50f,
            cx, b.top + b.height() * 0.14f
        );
        canvas.drawPath(path, strokePaint);

        // Inner Specular Arc
        path.reset();
        path.moveTo(cx - b.width() * 0.12f, b.top + b.height() * 0.58f);
        path.quadTo(cx - b.width() * 0.16f, b.bottom - b.height() * 0.26f, cx - b.width() * 0.04f, b.bottom - b.height() * 0.20f);
        canvas.drawPath(path, strokePaint);
    }

    // 8. Blueprint: Open Book Folio
    private void drawBlueprint(Canvas canvas, RectF b, float density) {
        float cx = b.centerX();
        float cy = b.centerY();
        path.reset();
        // Left Page
        path.moveTo(cx, b.top + b.height() * 0.25f);
        path.quadTo(b.left + b.width() * 0.24f, b.top + b.height() * 0.18f, b.left + b.width() * 0.14f, b.top + b.height() * 0.24f);
        path.lineTo(b.left + b.width() * 0.14f, b.bottom - b.height() * 0.22f);
        path.quadTo(b.left + b.width() * 0.24f, b.bottom - b.height() * 0.28f, cx, b.bottom - b.height() * 0.22f);
        // Right Page
        path.quadTo(b.right - b.width() * 0.24f, b.bottom - b.height() * 0.28f, b.right - b.width() * 0.14f, b.bottom - b.height() * 0.22f);
        path.lineTo(b.right - b.width() * 0.14f, b.top + b.height() * 0.24f);
        path.quadTo(b.right - b.width() * 0.24f, b.top + b.height() * 0.18f, cx, b.top + b.height() * 0.25f);
        canvas.drawPath(path, strokePaint);

        // Center spine line
        canvas.drawLine(cx, b.top + b.height() * 0.25f, cx, b.bottom - b.height() * 0.22f, strokePaint);
    }

    // 9. Downloader: Plunging Arrow into Tray
    private void drawDownloader(Canvas canvas, RectF b, float density) {
        float cx = b.centerX();
        // Vertical arrow stem
        canvas.drawLine(cx, b.top + b.height() * 0.14f, cx, b.bottom - b.height() * 0.38f, strokePaint);
        // Arrowhead
        path.reset();
        path.moveTo(cx - b.width() * 0.22f, b.bottom - b.height() * 0.54f);
        path.lineTo(cx, b.bottom - b.height() * 0.36f);
        path.lineTo(cx + b.width() * 0.22f, b.bottom - b.height() * 0.54f);
        canvas.drawPath(path, strokePaint);

        // Bottom Tray
        path.reset();
        path.moveTo(b.left + b.width() * 0.16f, b.bottom - b.height() * 0.32f);
        path.lineTo(b.left + b.width() * 0.16f, b.bottom - b.height() * 0.16f);
        path.lineTo(b.right - b.width() * 0.16f, b.bottom - b.height() * 0.16f);
        path.lineTo(b.right - b.width() * 0.16f, b.bottom - b.height() * 0.32f);
        canvas.drawPath(path, strokePaint);
    }

    // 10. Calculator: 2x2 Operator Matrix (+, −, ×, =)
    private void drawCalculator(Canvas canvas, RectF b, float density) {
        float cx = b.centerX();
        float cy = b.centerY();
        float d = b.width() * 0.22f;

        // Top Left: + (Plus)
        canvas.drawLine(cx - d, cy - d - 3.5f * density, cx - d, cy - d + 3.5f * density, strokePaint);
        canvas.drawLine(cx - d - 3.5f * density, cy - d, cx - d + 3.5f * density, cy - d, strokePaint);

        // Top Right: − (Minus)
        canvas.drawLine(cx + d - 3.5f * density, cy - d, cx + d + 3.5f * density, cy - d, strokePaint);

        // Bottom Left: × (Multiply)
        float md = 2.8f * density;
        canvas.drawLine(cx - d - md, cy + d - md, cx - d + md, cy + d + md, strokePaint);
        canvas.drawLine(cx - d - md, cy + d + md, cx - d + md, cy + d - md, strokePaint);

        // Bottom Right: = (Equals)
        canvas.drawLine(cx + d - 3.5f * density, cy + d - 2.0f * density, cx + d + 3.5f * density, cy + d - 2.0f * density, strokePaint);
        canvas.drawLine(cx + d - 3.5f * density, cy + d + 2.0f * density, cx + d + 3.5f * density, cy + d + 2.0f * density, strokePaint);
    }

    private void drawGeneric(Canvas canvas, RectF b, float density) {
        canvas.drawCircle(b.centerX(), b.centerY(), b.width() * 0.35f, strokePaint);
    }
}
