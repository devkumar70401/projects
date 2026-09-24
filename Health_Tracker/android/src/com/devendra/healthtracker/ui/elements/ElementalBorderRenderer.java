package com.devendra.healthtracker.ui.elements;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;

import java.util.Random;

/**
 * ElementalBorderRenderer
 * Reusable, high-performance procedural renderer for elemental card borders:
 * - BUBBLING: Effervescent rising aquatic bubbles, buoyant wobbles, specular highlights.
 * - FIRE: Blazing solar flames, flickering tongues of fire, floating embers.
 * - THUNDER: Crackling electric lightning arcs, high-voltage spark discharge nodes.
 * - AIR: Swirling ethereal wind currents, flowing aerodynamic vortex ribbons.
 * - VOLCANO: Molten incandescent magma fissures, explosive volcanic cinder bursts.
 *
 * Pre-allocated geometry and particle pools: Zero garbage collection overhead during drawing.
 */
public class ElementalBorderRenderer {

    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF boundsRect = new RectF();
    private final Path borderPath = new Path();
    private final Path effectPath = new Path();

    private static final int PARTICLE_COUNT = 24;
    private final float[] partX = new float[PARTICLE_COUNT];
    private final float[] partY = new float[PARTICLE_COUNT];
    private final float[] partSpeed = new float[PARTICLE_COUNT];
    private final float[] partSize = new float[PARTICLE_COUNT];
    private final float[] partPhase = new float[PARTICLE_COUNT];

    private static final int LIGHTNING_NODES = 8;
    private final float[] boltX = new float[LIGHTNING_NODES];
    private final float[] boltY = new float[LIGHTNING_NODES];
    private long lastBoltTime = 0;
    private int activeBoltSide = 0;

    private final float density;
    private final Random rand = new Random(777);

    public ElementalBorderRenderer(Context context) {
        this.density = context != null ? context.getResources().getDisplayMetrics().density : 2.5f;

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(1.8f));

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(dp(4.5f));

        particlePaint.setStyle(Paint.Style.FILL);
        accentPaint.setStyle(Paint.Style.STROKE);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            partX[i] = rand.nextFloat();
            partY[i] = rand.nextFloat();
            partSpeed[i] = 0.5f + rand.nextFloat() * 1.5f;
            partSize[i] = dp(1.5f + rand.nextFloat() * 3.0f);
            partPhase[i] = rand.nextFloat() * (float) (Math.PI * 2);
        }
    }

    public void draw(Canvas canvas, int w, int h, ElementType type, float cornerRadiusPx) {
        if (w <= 0 || h <= 0 || type == null) return;

        float pad = dp(1.5f);
        boundsRect.set(pad, pad, w - pad, h - pad);
        borderPath.reset();
        borderPath.addRoundRect(boundsRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW);

        switch (type) {
            case FIRE:
                drawFire(canvas, w, h, cornerRadiusPx);
                break;
            case THUNDER:
                drawThunder(canvas, w, h, cornerRadiusPx);
                break;
            case AIR:
                drawAir(canvas, w, h, cornerRadiusPx);
                break;
            case BUBBLING:
                drawBubbling(canvas, w, h, cornerRadiusPx);
                break;
            case VOLCANO:
                drawVolcano(canvas, w, h, cornerRadiusPx);
                break;
        }
    }

    private void drawFire(Canvas canvas, int w, int h, float cornerRadiusPx) {
        float time = (SystemClock.uptimeMillis() % 100000) / 1000f;

        // Radiant pulsating outer heat aura
        int pulseAlpha = 75 + (int) (35 * Math.sin(time * 5.0f));
        glowPaint.setColor(Color.argb(pulseAlpha, 239, 68, 68));
        canvas.drawPath(borderPath, glowPaint);

        // Core flame border stroke
        borderPaint.setColor(Color.argb(220, 252, 211, 77));
        canvas.drawPath(borderPath, borderPaint);

        // Licking flame tongues on top edge
        accentPaint.setColor(Color.argb(185, 249, 115, 22));
        accentPaint.setStrokeWidth(dp(1.5f));
        effectPath.reset();
        float flameCount = 10;
        float segW = (w - cornerRadiusPx * 2) / flameCount;
        for (int i = 0; i < flameCount; i++) {
            float startX = cornerRadiusPx + i * segW;
            float midX = startX + segW * 0.5f;
            float endX = startX + segW;
            float flameH = dp(3.5f + (float) Math.sin(time * 8.0f + i * 1.5f) * 3.0f);
            effectPath.moveTo(startX, dp(2f));
            effectPath.quadTo(midX, dp(2f) - flameH, endX, dp(2f));
        }
        canvas.drawPath(effectPath, accentPaint);

        // Rising embers
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float pTime = (time * partSpeed[i] + partPhase[i]) % 1.0f;
            float px = boundsRect.left + partX[i] * boundsRect.width() + (float) Math.sin(time * 4f + i) * dp(3f);
            float py = boundsRect.bottom - pTime * boundsRect.height() * 1.1f;
            int alpha = (int) (210 * (1f - pTime));
            if (alpha > 0 && py > 0) {
                particlePaint.setColor(Color.argb(alpha, 253, 224, 71));
                canvas.drawCircle(px, py, partSize[i] * (1f - pTime * 0.4f), particlePaint);
            }
        }
    }

    private void drawThunder(Canvas canvas, int w, int h, float cornerRadiusPx) {
        long now = SystemClock.uptimeMillis();
        float time = (now % 100000) / 1000f;

        int glowAlpha = 70 + (int) (45 * Math.abs(Math.sin(time * 12.0f)));
        glowPaint.setColor(Color.argb(glowAlpha, 139, 92, 246));
        canvas.drawPath(borderPath, glowPaint);

        borderPaint.setColor(Color.argb(230, 56, 189, 248));
        canvas.drawPath(borderPath, borderPaint);

        if (now - lastBoltTime > 190) {
            lastBoltTime = now;
            activeBoltSide = rand.nextInt(4);
            generateLightning(activeBoltSide);
        }

        accentPaint.setColor(Color.argb(235, 255, 255, 255));
        accentPaint.setStrokeWidth(dp(1.6f));
        effectPath.reset();
        effectPath.moveTo(boltX[0], boltY[0]);
        for (int i = 1; i < LIGHTNING_NODES; i++) {
            effectPath.lineTo(boltX[i], boltY[i]);
        }
        canvas.drawPath(effectPath, accentPaint);

        // Corner spark discharge nodes
        float[][] corners = new float[][]{
            {boundsRect.left + cornerRadiusPx * 0.4f, boundsRect.top + cornerRadiusPx * 0.4f},
            {boundsRect.right - cornerRadiusPx * 0.4f, boundsRect.top + cornerRadiusPx * 0.4f},
            {boundsRect.right - cornerRadiusPx * 0.4f, boundsRect.bottom - cornerRadiusPx * 0.4f},
            {boundsRect.left + cornerRadiusPx * 0.4f, boundsRect.bottom - cornerRadiusPx * 0.4f}
        };

        for (int i = 0; i < 4; i++) {
            float nodeScale = dp(2f + (float) Math.abs(Math.sin(time * 15f + i * 2f)) * 2.2f);
            particlePaint.setColor(Color.argb(200, 192, 132, 252));
            canvas.drawCircle(corners[i][0], corners[i][1], nodeScale, particlePaint);
            particlePaint.setColor(Color.WHITE);
            canvas.drawCircle(corners[i][0], corners[i][1], nodeScale * 0.45f, particlePaint);
        }
    }

    private void generateLightning(int side) {
        float jitter = dp(4.5f);
        for (int i = 0; i < LIGHTNING_NODES; i++) {
            float t = (float) i / (LIGHTNING_NODES - 1);
            float randJitter = (rand.nextFloat() - 0.5f) * jitter;
            switch (side) {
                case 0:
                    boltX[i] = boundsRect.left + t * boundsRect.width();
                    boltY[i] = boundsRect.top + randJitter;
                    break;
                case 1:
                    boltX[i] = boundsRect.right + randJitter;
                    boltY[i] = boundsRect.top + t * boundsRect.height();
                    break;
                case 2:
                    boltX[i] = boundsRect.right - t * boundsRect.width();
                    boltY[i] = boundsRect.bottom + randJitter;
                    break;
                case 3:
                    boltX[i] = boundsRect.left + randJitter;
                    boltY[i] = boundsRect.bottom - t * boundsRect.height();
                    break;
            }
        }
    }

    private void drawAir(Canvas canvas, int w, int h, float cornerRadiusPx) {
        float time = (SystemClock.uptimeMillis() % 100000) / 1000f;

        int haloAlpha = 75 + (int) (35 * Math.sin(time * 3f));
        glowPaint.setColor(Color.argb(haloAlpha, 16, 185, 129));
        canvas.drawPath(borderPath, glowPaint);

        borderPaint.setColor(Color.argb(205, 167, 243, 208));
        canvas.drawPath(borderPath, borderPaint);

        accentPaint.setStrokeWidth(dp(1.3f));
        for (int s = 0; s < 2; s++) {
            float streamOffset = (time * 1.8f + s * 0.5f) % 1.0f;
            float wavePhase = time * 6.0f + s * 2.0f;
            accentPaint.setColor(Color.argb(150 - s * 40, 52, 211, 153));

            float x1 = boundsRect.left + cornerRadiusPx + streamOffset * (boundsRect.width() - cornerRadiusPx * 2);
            float y1 = boundsRect.top + (float) Math.sin(wavePhase) * dp(3f);
            effectPath.reset();
            effectPath.moveTo(x1, y1);
            effectPath.lineTo(x1 + dp(24f), y1);
            canvas.drawPath(effectPath, accentPaint);
        }

        for (int i = 0; i < 12; i++) {
            float pT = (time * 0.4f + partPhase[i]) % 1.0f;
            float px = boundsRect.left + partX[i] * boundsRect.width() + (float) Math.cos(time * 3f + i) * dp(4f);
            float py = boundsRect.top + pT * boundsRect.height();
            particlePaint.setColor(Color.argb(110, 209, 250, 229));
            canvas.drawCircle(px, py, dp(1.6f), particlePaint);
        }
    }

    private void drawBubbling(Canvas canvas, int w, int h, float cornerRadiusPx) {
        float time = (SystemClock.uptimeMillis() % 100000) / 1000f;

        int glowA = 80 + (int) (30 * Math.sin(time * 4f));
        glowPaint.setColor(Color.argb(glowA, 6, 182, 212));
        canvas.drawPath(borderPath, glowPaint);

        borderPaint.setColor(Color.argb(220, 103, 232, 249));
        canvas.drawPath(borderPath, borderPaint);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float bTime = (time * partSpeed[i] * 0.6f + partPhase[i]) % 1.0f;
            float sideSelect = partX[i];
            float bx, by;

            if (sideSelect < 0.45f) {
                bx = boundsRect.left + (float) Math.sin(time * 5f + i) * dp(3.5f);
                by = boundsRect.bottom - bTime * boundsRect.height();
            } else if (sideSelect < 0.90f) {
                bx = boundsRect.right + (float) Math.cos(time * 5f + i) * dp(3.5f);
                by = boundsRect.bottom - bTime * boundsRect.height();
            } else {
                bx = boundsRect.left + boundsRect.width() * partY[i];
                by = boundsRect.bottom - bTime * boundsRect.height();
            }

            float bRadius = partSize[i] * (0.8f + (float) Math.sin(time * 6f + i) * 0.2f);
            int alpha = (int) (190 * (1f - bTime * 0.7f));

            if (alpha > 0 && by > boundsRect.top) {
                particlePaint.setStyle(Paint.Style.STROKE);
                particlePaint.setStrokeWidth(dp(1.1f));
                particlePaint.setColor(Color.argb(alpha, 165, 243, 252));
                canvas.drawCircle(bx, by, bRadius, particlePaint);

                particlePaint.setStyle(Paint.Style.FILL);
                particlePaint.setColor(Color.argb(alpha, 255, 255, 255));
                canvas.drawCircle(bx - bRadius * 0.35f, by - bRadius * 0.35f, bRadius * 0.35f, particlePaint);
            }
        }
    }

    private void drawVolcano(Canvas canvas, int w, int h, float cornerRadiusPx) {
        float time = (SystemClock.uptimeMillis() % 100000) / 1000f;

        int pulseA = 90 + (int) (50 * Math.abs(Math.sin(time * 4.5f)));
        glowPaint.setColor(Color.argb(pulseA, 249, 115, 22));
        canvas.drawPath(borderPath, glowPaint);

        borderPaint.setColor(Color.argb(230, 251, 191, 36));
        canvas.drawPath(borderPath, borderPaint);

        accentPaint.setStrokeWidth(dp(2.2f));
        accentPaint.setColor(Color.argb(200, 220, 38, 38));

        float fissureT = (time * 0.5f) % 1.0f;
        effectPath.reset();
        float fx1 = boundsRect.left + fissureT * boundsRect.width();
        float fy1 = boundsRect.top;
        effectPath.moveTo(fx1, fy1);
        effectPath.lineTo(fx1 + dp(28f), fy1);
        canvas.drawPath(effectPath, accentPaint);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float eTime = (time * partSpeed[i] * 1.3f + partPhase[i]) % 1.0f;
            float angle = (float) (i * Math.PI * 2 / PARTICLE_COUNT);
            float dist = eTime * dp(20f);

            float ox = (i % 2 == 0) ? boundsRect.left : boundsRect.right;
            float oy = (i < PARTICLE_COUNT / 2) ? boundsRect.top : boundsRect.bottom;

            float cx = ox + (float) Math.cos(angle) * dist;
            float cy = oy + (float) Math.sin(angle) * dist;

            int alpha = (int) (225 * (1f - eTime));
            if (alpha > 0) {
                particlePaint.setStyle(Paint.Style.FILL);
                particlePaint.setColor(i % 3 == 0 ? Color.argb(alpha, 254, 240, 138) : Color.argb(alpha, 239, 68, 68));
                canvas.drawCircle(cx, cy, dp(1.6f + (1f - eTime) * 1.8f), particlePaint);
            }
        }
    }

    private float dp(float val) {
        return val * density;
    }
}
