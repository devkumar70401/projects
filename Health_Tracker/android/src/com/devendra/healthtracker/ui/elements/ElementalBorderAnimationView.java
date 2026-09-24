package com.devendra.healthtracker.ui.elements;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Random;

/**
 * ElementalBorderAnimationView
 * Renders dynamic, 60-FPS procedural animated card borders representing elemental forces:
 * - BUBBLING: Effervescent rising aquatic bubbles, buoyant wobbles, popping ripples.
 * - FIRE: Blazing solar flames, flickering tongues of fire, ascending ember sparks.
 * - THUNDER: Crackling electric lightning arcs, zig-zag discharges, high-voltage spark nodes.
 * - AIR: Swirling ethereal wind currents, flowing vortex ribbons, misty breeze particles.
 * - VOLCANO: Molten incandescent magma surges, explosive cinder eruptions.
 *
 * Performance-optimized: Zero allocations in onDraw(), automatic lifecycle pause when hidden.
 */
public class ElementalBorderAnimationView extends View {

    private ElementType elementType = ElementType.FIRE;
    private int cornerRadiusPx = 36;
    private boolean isDarkTheme = true;

    // Paints
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Reusable geometry
    private final RectF boundsRect = new RectF();
    private final Path borderPath = new Path();
    private final Path effectPath = new Path();

    // Particle pools (pre-allocated)
    private static final int PARTICLE_COUNT = 24;
    private final float[] partX = new float[PARTICLE_COUNT];
    private final float[] partY = new float[PARTICLE_COUNT];
    private final float[] partSpeed = new float[PARTICLE_COUNT];
    private final float[] partSize = new float[PARTICLE_COUNT];
    private final float[] partPhase = new float[PARTICLE_COUNT];
    private final int[] partColor = new int[PARTICLE_COUNT];

    // Lightning segments for Thunder
    private static final int LIGHTNING_NODES = 8;
    private final float[] boltX = new float[LIGHTNING_NODES];
    private final float[] boltY = new float[LIGHTNING_NODES];
    private long lastBoltTime = 0;
    private int activeBoltSide = 0; // 0=top, 1=right, 2=bottom, 3=left

    // Animation state
    private ValueAnimator animator;
    private float animProgress = 0f;
    private final Random rand = new Random(42);

    public ElementalBorderAnimationView(Context context) {
        super(context);
        init();
    }

    public ElementalBorderAnimationView(Context context, ElementType type, boolean isDarkTheme) {
        super(context);
        this.elementType = type != null ? type : ElementType.FIRE;
        this.isDarkTheme = isDarkTheme;
        init();
    }

    private void init() {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(2f));

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(dp(5f));

        particlePaint.setStyle(Paint.Style.FILL);
        accentPaint.setStyle(Paint.Style.STROKE);

        resetParticles();
        setupAnimator();
    }

    public void setElementType(ElementType type) {
        if (this.elementType != type) {
            this.elementType = type != null ? type : ElementType.FIRE;
            resetParticles();
            invalidate();
        }
    }

    public ElementType getElementType() {
        return elementType;
    }

    public void setCornerRadiusDp(float radiusDp) {
        this.cornerRadiusPx = (int) dp(radiusDp);
        invalidate();
    }

    public void setDarkTheme(boolean isDark) {
        this.isDarkTheme = isDark;
        invalidate();
    }

    private void resetParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            partX[i] = rand.nextFloat();
            partY[i] = rand.nextFloat();
            partSpeed[i] = 0.5f + rand.nextFloat() * 1.5f;
            partSize[i] = dp(1.5f + rand.nextFloat() * 3.5f);
            partPhase[i] = rand.nextFloat() * (float) (Math.PI * 2);
            partColor[i] = (i % 2 == 0) ? elementType.getPrimaryColor() : elementType.getAccentColor();
        }
    }

    private void setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(3000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            animProgress = (Float) animation.getAnimatedValue();
            postInvalidateOnAnimation();
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null && !animator.isRunning()) {
            animator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (animator != null) {
            if (visibility == VISIBLE && isShown()) {
                if (!animator.isRunning()) animator.start();
            } else {
                if (animator.isRunning()) animator.cancel();
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = dp(2f);
        boundsRect.set(pad, pad, w - pad, h - pad);
        borderPath.reset();
        borderPath.addRoundRect(boundsRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        switch (elementType) {
            case FIRE:
                drawFireBorder(canvas, w, h);
                break;
            case THUNDER:
                drawThunderBorder(canvas, w, h);
                break;
            case AIR:
                drawAirBorder(canvas, w, h);
                break;
            case BUBBLING:
                drawBubblingBorder(canvas, w, h);
                break;
            case VOLCANO:
                drawVolcanoBorder(canvas, w, h);
                break;
        }
    }

    // =========================================================================
    // 1. FIRE: Blazing solar flames, flickering tongues, ascending ember sparks
    // =========================================================================
    private void drawFireBorder(Canvas canvas, int w, int h) {
        float time = (SystemClock.uptimeMillis() % 100000) / 1000f;

        // Radiant pulsating outer heat aura
        int pulseAlpha = 80 + (int) (40 * Math.sin(time * 5.0f));
        glowPaint.setColor(Color.argb(pulseAlpha, 239, 68, 68));
        canvas.drawPath(borderPath, glowPaint);

        // Core flame border stroke
        borderPaint.setColor(Color.argb(230, 252, 211, 77));
        borderPaint.setStrokeWidth(dp(1.8f));
        canvas.drawPath(borderPath, borderPaint);

        // Dynamic licking flame tongues on the top edge
        accentPaint.setColor(Color.argb(190, 249, 115, 22));
        accentPaint.setStrokeWidth(dp(1.5f));
        accentPaint.setStyle(Paint.Style.STROKE);

        effectPath.reset();
        float flameCount = 12;
        float segW = (w - cornerRadiusPx * 2) / flameCount;
        for (int i = 0; i < flameCount; i++) {
            float startX = cornerRadiusPx + i * segW;
            float midX = startX + segW * 0.5f;
            float endX = startX + segW;
            float flameH = dp(4f + (float) Math.sin(time * 8.0f + i * 1.5f) * 3.5f);

            effectPath.moveTo(startX, dp(2f));
            effectPath.quadTo(midX, dp(2f) - flameH, endX, dp(2f));
        }
        canvas.drawPath(effectPath, accentPaint);

        // Floating ember particles rising upward
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float pTime = (time * partSpeed[i] + partPhase[i]) % 1.0f;
            float px = boundsRect.left + partX[i] * boundsRect.width() + (float) Math.sin(time * 4f + i) * dp(4f);
            float py = boundsRect.bottom - pTime * boundsRect.height() * 1.15f;

            int alpha = (int) (220 * (1f - pTime));
            if (alpha > 0 && py > 0) {
                particlePaint.setColor(Color.argb(alpha, 253, 224, 71));
                canvas.drawCircle(px, py, partSize[i] * (1f - pTime * 0.4f), particlePaint);
            }
        }
    }

    // =========================================================================
    // 2. THUNDER: Crackling electric arcs, zig-zag discharges, electric sparks
    // =========================================================================
    private void drawThunderBorder(Canvas canvas, int w, int h) {
        long now = SystemClock.uptimeMillis();
        float time = (now % 100000) / 1000f;

        // Violet-cyan electric outer aura
        int glowAlpha = 70 + (int) (50 * Math.abs(Math.sin(time * 12.0f)));
        glowPaint.setColor(Color.argb(glowAlpha, 139, 92, 246));
        canvas.drawPath(borderPath, glowPaint);

        // Crisp neon core stroke
        borderPaint.setColor(Color.argb(240, 56, 189, 248));
        borderPaint.setStrokeWidth(dp(1.8f));
        canvas.drawPath(borderPath, borderPaint);

        // High-voltage zig-zag crackle arcs (randomized burst every 150-300ms)
        if (now - lastBoltTime > 180) {
            lastBoltTime = now;
            activeBoltSide = rand.nextInt(4);
            generateLightningNodes(w, h, activeBoltSide);
        }

        accentPaint.setColor(Color.argb(230, 255, 255, 255));
        accentPaint.setStrokeWidth(dp(1.6f));
        accentPaint.setStyle(Paint.Style.STROKE);

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
            float nodeScale = dp(2f + (float) Math.abs(Math.sin(time * 15f + i * 2f)) * 2.5f);
            particlePaint.setColor(Color.argb(210, 192, 132, 252));
            canvas.drawCircle(corners[i][0], corners[i][1], nodeScale, particlePaint);
            particlePaint.setColor(Color.WHITE);
            canvas.drawCircle(corners[i][0], corners[i][1], nodeScale * 0.45f, particlePaint);
        }
    }

    private void generateLightningNodes(int w, int h, int side) {
        float jitter = dp(5f);
        for (int i = 0; i < LIGHTNING_NODES; i++) {
            float t = (float) i / (LIGHTNING_NODES - 1);
            float randJitter = (rand.nextFloat() - 0.5f) * jitter;
            switch (side) {
                case 0: // Top
                    boltX[i] = boundsRect.left + t * boundsRect.width();
                    boltY[i] = boundsRect.top + randJitter;
                    break;
                case 1: // Right
                    boltX[i] = boundsRect.right + randJitter;
                    boltY[i] = boundsRect.top + t * boundsRect.height();
                    break;
                case 2: // Bottom
                    boltX[i] = boundsRect.right - t * boundsRect.width();
                    boltY[i] = boundsRect.bottom + randJitter;
                    break;
                case 3: // Left
                    boltX[i] = boundsRect.left + randJitter;
                    boltY[i] = boundsRect.bottom - t * boundsRect.height();
                    break;
            }
        }
    }

    // =========================================================================
    // 3. AIR: Swirling ethereal wind currents, flowing vortex ribbons
    // =========================================================================
    private void drawAirBorder(Canvas canvas, int w, int h) {
        float time = (SystemClock.uptimeMillis() % 100000) / 1000f;

        // Ethereal jade/emerald halo
        int haloAlpha = 80 + (int) (40 * Math.sin(time * 3f));
        glowPaint.setColor(Color.argb(haloAlpha, 16, 185, 129));
        canvas.drawPath(borderPath, glowPaint);

        // Core aerodynamic stroke
        borderPaint.setColor(Color.argb(210, 167, 243, 208));
        borderPaint.setStrokeWidth(dp(1.8f));
        canvas.drawPath(borderPath, borderPaint);

        // Flowing wind stream vortex lines orbiting the perimeter
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeWidth(dp(1.4f));

        for (int s = 0; s < 3; s++) {
            float streamOffset = (time * 1.8f + s * 0.33f) % 1.0f;
            float wavePhase = time * 6.0f + s * 2.0f;

            accentPaint.setColor(Color.argb(160 - s * 35, 52, 211, 153));
            effectPath.reset();

            // Sinuous swirl along top border
            float x1 = boundsRect.left + cornerRadiusPx + streamOffset * (boundsRect.width() - cornerRadiusPx * 2);
            float y1 = boundsRect.top + (float) Math.sin(wavePhase) * dp(3.5f);
            float x2 = x1 + dp(28f);
            effectPath.moveTo(x1, y1);
            effectPath.lineTo(x2, y1);
            canvas.drawPath(effectPath, accentPaint);

            // Sinuous swirl along bottom border
            float bx1 = boundsRect.right - cornerRadiusPx - streamOffset * (boundsRect.width() - cornerRadiusPx * 2);
            float by1 = boundsRect.bottom + (float) Math.cos(wavePhase) * dp(3.5f);
            float bx2 = bx1 - dp(28f);
            effectPath.reset();
            effectPath.moveTo(bx1, by1);
            effectPath.lineTo(bx2, by1);
            canvas.drawPath(effectPath, accentPaint);
        }

        // Drifting misty vapor dots
        for (int i = 0; i < 14; i++) {
            float pT = (time * 0.4f + partPhase[i]) % 1.0f;
            float px = boundsRect.left + partX[i] * boundsRect.width() + (float) Math.cos(time * 3f + i) * dp(5f);
            float py = boundsRect.top + pT * boundsRect.height();
            particlePaint.setColor(Color.argb(120, 209, 250, 229));
            canvas.drawCircle(px, py, dp(1.8f), particlePaint);
        }
    }

    // =========================================================================
    // 4. BUBBLING: Effervescent rising aquatic bubbles, buoyant ripples
    // =========================================================================
    private void drawBubblingBorder(Canvas canvas, int w, int h) {
        float time = (SystemClock.uptimeMillis() % 100000) / 1000f;

        // Shimmering oceanic cyan glow
        int glowA = 90 + (int) (35 * Math.sin(time * 4f));
        glowPaint.setColor(Color.argb(glowA, 6, 182, 212));
        canvas.drawPath(borderPath, glowPaint);

        // Crisp aquamarine perimeter
        borderPaint.setColor(Color.argb(225, 103, 232, 249));
        borderPaint.setStrokeWidth(dp(1.8f));
        canvas.drawPath(borderPath, borderPaint);

        // Effervescent bubbles floating up along the borders
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float bTime = (time * partSpeed[i] * 0.6f + partPhase[i]) % 1.0f;
            float sideSelect = partX[i];
            float bx, by;

            if (sideSelect < 0.45f) {
                // Left border stream
                bx = boundsRect.left + (float) Math.sin(time * 5f + i) * dp(4f);
                by = boundsRect.bottom - bTime * boundsRect.height();
            } else if (sideSelect < 0.90f) {
                // Right border stream
                bx = boundsRect.right + (float) Math.cos(time * 5f + i) * dp(4f);
                by = boundsRect.bottom - bTime * boundsRect.height();
            } else {
                // Center buoyant bubble
                bx = boundsRect.left + boundsRect.width() * partY[i];
                by = boundsRect.bottom - bTime * boundsRect.height();
            }

            float bRadius = partSize[i] * (0.8f + (float) Math.sin(time * 6f + i) * 0.2f);
            int alpha = (int) (200 * (1f - bTime * 0.7f));

            if (alpha > 0 && by > boundsRect.top) {
                // Bubble body
                particlePaint.setStyle(Paint.Style.STROKE);
                particlePaint.setStrokeWidth(dp(1.2f));
                particlePaint.setColor(Color.argb(alpha, 165, 243, 252));
                canvas.drawCircle(bx, by, bRadius, particlePaint);

                // Bubble specular reflection dot
                particlePaint.setStyle(Paint.Style.FILL);
                particlePaint.setColor(Color.argb(alpha, 255, 255, 255));
                canvas.drawCircle(bx - bRadius * 0.35f, by - bRadius * 0.35f, bRadius * 0.35f, particlePaint);
            }
        }
    }

    // =========================================================================
    // 5. VOLCANO: Molten incandescent magma surges, explosive cinder bursts
    // =========================================================================
    private void drawVolcanoBorder(Canvas canvas, int w, int h) {
        float time = (SystemClock.uptimeMillis() % 100000) / 1000f;

        // Incandescent molten ruby-orange halo
        int pulseA = 100 + (int) (60 * Math.abs(Math.sin(time * 4.5f)));
        glowPaint.setColor(Color.argb(pulseA, 249, 115, 22));
        canvas.drawPath(borderPath, glowPaint);

        // Core magma vein stroke
        borderPaint.setColor(Color.argb(235, 251, 191, 36));
        borderPaint.setStrokeWidth(dp(2.0f));
        canvas.drawPath(borderPath, borderPaint);

        // Magma pulse fissures traveling along perimeter
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeWidth(dp(2.8f));
        accentPaint.setColor(Color.argb(210, 220, 38, 38));

        float fissureT = (time * 0.5f) % 1.0f;
        effectPath.reset();
        float fx1 = boundsRect.left + fissureT * boundsRect.width();
        float fy1 = boundsRect.top;
        effectPath.moveTo(fx1, fy1);
        effectPath.lineTo(fx1 + dp(32f), fy1);
        canvas.drawPath(effectPath, accentPaint);

        // Explosive volcanic cinder sparks erupting outwards
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float eTime = (time * partSpeed[i] * 1.3f + partPhase[i]) % 1.0f;
            float angle = (float) (i * Math.PI * 2 / PARTICLE_COUNT);
            float dist = eTime * dp(24f);

            float ox = (i % 2 == 0) ? boundsRect.left : boundsRect.right;
            float oy = (i < PARTICLE_COUNT / 2) ? boundsRect.top : boundsRect.bottom;

            float cx = ox + (float) Math.cos(angle) * dist;
            float cy = oy + (float) Math.sin(angle) * dist;

            int alpha = (int) (240 * (1f - eTime));
            if (alpha > 0) {
                particlePaint.setStyle(Paint.Style.FILL);
                particlePaint.setColor(i % 3 == 0 ? Color.argb(alpha, 254, 240, 138) : Color.argb(alpha, 239, 68, 68));
                canvas.drawCircle(cx, cy, dp(1.8f + (1f - eTime) * 2f), particlePaint);
            }
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
