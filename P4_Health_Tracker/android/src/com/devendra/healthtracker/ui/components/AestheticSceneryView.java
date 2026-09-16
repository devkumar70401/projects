package com.devendra.healthtracker.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import java.util.Random;

/**
 * AestheticSceneryView
 * Pure Canvas-rendered procedural aesthetic scenery featuring mountain silhouettes,
 * twilight/dawn sky gradients, celestial glow (stars/crescent moon in dark mode, sunrise in light mode),
 * and calm horizon lines.
 */
public class AestheticSceneryView extends View {

    private boolean isDarkTheme = true;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    // Deterministic star field
    private static final int STAR_COUNT = 48;
    private final float[] starX = new float[STAR_COUNT];
    private final float[] starY = new float[STAR_COUNT];
    private final float[] starRadius = new float[STAR_COUNT];
    private final int[] starAlpha = new int[STAR_COUNT];

    public AestheticSceneryView(Context context, boolean isDarkTheme) {
        super(context);
        this.isDarkTheme = isDarkTheme;
        initStars();
    }

    public void setDarkTheme(boolean isDark) {
        if (this.isDarkTheme != isDark) {
            this.isDarkTheme = isDark;
            invalidate();
        }
    }

    private void initStars() {
        Random rand = new Random(421337);
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = rand.nextFloat(); // 0.0 to 1.0 (screen relative)
            starY[i] = rand.nextFloat() * 0.55f; // upper 55% of screen
            starRadius[i] = 1.0f + rand.nextFloat() * 2.2f;
            starAlpha[i] = 120 + rand.nextInt(135);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (isDarkTheme) {
            drawDarkScenery(canvas, w, h);
        } else {
            drawLightScenery(canvas, w, h);
        }
    }

    private void drawDarkScenery(Canvas canvas, int w, int h) {
        // 1. Twilight / Midnight Sky Gradient
        LinearGradient skyGradient = new LinearGradient(
            0, 0, 0, h * 0.75f,
            new int[]{
                Color.parseColor("#090D16"), // Deep void navy
                Color.parseColor("#15182D"), // Indigo
                Color.parseColor("#261E3E"), // Twilight purple
                Color.parseColor("#4A1942"), // Crimson twilight horizon
                Color.parseColor("#E11D48")  // Subtle horizon glow
            },
            new float[]{0.0f, 0.35f, 0.65f, 0.88f, 1.0f},
            Shader.TileMode.CLAMP
        );
        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(skyGradient);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // 2. Stars
        for (int i = 0; i < STAR_COUNT; i++) {
            paint.setColor(Color.WHITE);
            paint.setAlpha(starAlpha[i]);
            canvas.drawCircle(starX[i] * w, starY[i] * h, starRadius[i], paint);
        }

        // 3. Glowing Crescent Moon (Upper Left-Center)
        float moonCx = w * 0.22f;
        float moonCy = h * 0.18f;
        float moonRadius = w * 0.08f;

        // Moon Glow
        RadialGradient moonGlow = new RadialGradient(
            moonCx, moonCy, moonRadius * 2.5f,
            new int[]{Color.argb(80, 244, 63, 94), Color.argb(25, 147, 51, 234), Color.TRANSPARENT},
            null,
            Shader.TileMode.CLAMP
        );
        paint.setShader(moonGlow);
        canvas.drawCircle(moonCx, moonCy, moonRadius * 2.5f, paint);
        paint.setShader(null);

        // Crescent Body
        paint.setColor(Color.parseColor("#FFF1F2"));
        canvas.drawCircle(moonCx, moonCy, moonRadius, paint);
        // Cutout to make crescent
        paint.setColor(Color.parseColor("#15182D"));
        canvas.drawCircle(moonCx + moonRadius * 0.42f, moonCy - moonRadius * 0.22f, moonRadius * 0.88f, paint);

        // 4. Distant Mountain Range (Layer 1)
        path.reset();
        path.moveTo(0, h * 0.62f);
        path.lineTo(w * 0.15f, h * 0.54f);
        path.lineTo(w * 0.32f, h * 0.59f);
        path.lineTo(w * 0.48f, h * 0.51f);
        path.lineTo(w * 0.68f, h * 0.58f);
        path.lineTo(w * 0.82f, h * 0.49f);
        path.lineTo(w, h * 0.56f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();

        LinearGradient m1Grad = new LinearGradient(
            0, h * 0.49f, 0, h,
            Color.parseColor("#3B1736"),
            Color.parseColor("#1A1028"),
            Shader.TileMode.CLAMP
        );
        paint.setShader(m1Grad);
        canvas.drawPath(path, paint);
        paint.setShader(null);

        // 5. Mid-ground Mountain Range (Layer 2)
        path.reset();
        path.moveTo(0, h * 0.68f);
        path.lineTo(w * 0.22f, h * 0.60f);
        path.lineTo(w * 0.40f, h * 0.65f);
        path.lineTo(w * 0.58f, h * 0.58f);
        path.lineTo(w * 0.76f, h * 0.63f);
        path.lineTo(w, h * 0.59f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();

        LinearGradient m2Grad = new LinearGradient(
            0, h * 0.58f, 0, h,
            Color.parseColor("#22152F"),
            Color.parseColor("#0E111C"),
            Shader.TileMode.CLAMP
        );
        paint.setShader(m2Grad);
        canvas.drawPath(path, paint);
        paint.setShader(null);

        // 6. Foreground Foothills & Pine Silhouette Ridge (Layer 3)
        path.reset();
        path.moveTo(0, h * 0.74f);
        path.quadTo(w * 0.25f, h * 0.70f, w * 0.5f, h * 0.73f);
        path.quadTo(w * 0.75f, h * 0.76f, w, h * 0.72f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();

        paint.setColor(Color.parseColor("#070A12"));
        canvas.drawPath(path, paint);

        // Draw rhythmic pine tree silhouettes along the ridge
        paint.setColor(Color.parseColor("#04060B"));
        int treeCount = 28;
        for (int i = 0; i < treeCount; i++) {
            float tx = (w * i) / (float) (treeCount - 1);
            float baseRy = h * (0.71f + (float) Math.sin(i * 0.45f) * 0.025f);
            float treeH = h * (0.035f + (i % 3) * 0.015f);
            float treeW = w * 0.024f;

            Path treePath = new Path();
            treePath.moveTo(tx, baseRy - treeH);
            treePath.lineTo(tx - treeW, baseRy);
            treePath.lineTo(tx + treeW, baseRy);
            treePath.close();
            canvas.drawPath(treePath, paint);
        }

        // 7. Base Vignette Gradient for smooth integration with foreground cards
        LinearGradient baseVignette = new LinearGradient(
            0, h * 0.65f, 0, h,
            Color.TRANSPARENT,
            Color.argb(210, 7, 10, 18),
            Shader.TileMode.CLAMP
        );
        paint.setShader(baseVignette);
        canvas.drawRect(0, h * 0.65f, w, h, paint);
        paint.setShader(null);
    }

    private void drawLightScenery(Canvas canvas, int w, int h) {
        // 1. Dawn / Sunrise Sky Gradient
        LinearGradient skyGradient = new LinearGradient(
            0, 0, 0, h * 0.75f,
            new int[]{
                Color.parseColor("#BAE6FD"), // Soft clear morning sky
                Color.parseColor("#E0F2FE"), // Light blue
                Color.parseColor("#FED7AA"), // Warm morning peach
                Color.parseColor("#FECDD3"), // Dawn rose
                Color.parseColor("#FEF08A")  // Golden sunrise rim
            },
            new float[]{0.0f, 0.30f, 0.60f, 0.85f, 1.0f},
            Shader.TileMode.CLAMP
        );
        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(skyGradient);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // 2. Rising Golden Sun (Upper Left-Center)
        float sunCx = w * 0.25f;
        float sunCy = h * 0.24f;
        float sunRadius = w * 0.09f;

        RadialGradient sunGlow = new RadialGradient(
            sunCx, sunCy, sunRadius * 2.8f,
            new int[]{Color.argb(120, 253, 224, 71), Color.argb(40, 251, 146, 60), Color.TRANSPARENT},
            null,
            Shader.TileMode.CLAMP
        );
        paint.setShader(sunGlow);
        canvas.drawCircle(sunCx, sunCy, sunRadius * 2.8f, paint);
        paint.setShader(null);

        paint.setColor(Color.parseColor("#FEF08A"));
        canvas.drawCircle(sunCx, sunCy, sunRadius, paint);

        // 3. Distant Alpine Mountain Range (Layer 1)
        path.reset();
        path.moveTo(0, h * 0.62f);
        path.lineTo(w * 0.16f, h * 0.53f);
        path.lineTo(w * 0.33f, h * 0.58f);
        path.lineTo(w * 0.50f, h * 0.50f);
        path.lineTo(w * 0.70f, h * 0.57f);
        path.lineTo(w * 0.84f, h * 0.48f);
        path.lineTo(w, h * 0.55f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();

        LinearGradient m1Grad = new LinearGradient(
            0, h * 0.48f, 0, h,
            Color.parseColor("#C7D2FE"), // Soft lavender mountain mist
            Color.parseColor("#A5B4FC"),
            Shader.TileMode.CLAMP
        );
        paint.setShader(m1Grad);
        canvas.drawPath(path, paint);
        paint.setShader(null);

        // 4. Mid-ground Mountain Range (Layer 2)
        path.reset();
        path.moveTo(0, h * 0.67f);
        path.lineTo(w * 0.22f, h * 0.59f);
        path.lineTo(w * 0.42f, h * 0.64f);
        path.lineTo(w * 0.60f, h * 0.57f);
        path.lineTo(w * 0.78f, h * 0.62f);
        path.lineTo(w, h * 0.58f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();

        LinearGradient m2Grad = new LinearGradient(
            0, h * 0.57f, 0, h,
            Color.parseColor("#94A3B8"),
            Color.parseColor("#64748B"),
            Shader.TileMode.CLAMP
        );
        paint.setShader(m2Grad);
        canvas.drawPath(path, paint);
        paint.setShader(null);

        // 5. Foreground Foothills & Pine Ridge (Layer 3)
        path.reset();
        path.moveTo(0, h * 0.73f);
        path.quadTo(w * 0.25f, h * 0.69f, w * 0.5f, h * 0.72f);
        path.quadTo(w * 0.75f, h * 0.75f, w, h * 0.71f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();

        paint.setColor(Color.parseColor("#334155"));
        canvas.drawPath(path, paint);

        // Pine silhouettes
        paint.setColor(Color.parseColor("#1E293B"));
        int treeCount = 28;
        for (int i = 0; i < treeCount; i++) {
            float tx = (w * i) / (float) (treeCount - 1);
            float baseRy = h * (0.70f + (float) Math.sin(i * 0.45f) * 0.025f);
            float treeH = h * (0.035f + (i % 3) * 0.015f);
            float treeW = w * 0.024f;

            Path treePath = new Path();
            treePath.moveTo(tx, baseRy - treeH);
            treePath.lineTo(tx - treeW, baseRy);
            treePath.lineTo(tx + treeW, baseRy);
            treePath.close();
            canvas.drawPath(treePath, paint);
        }

        // 6. Base Vignette Gradient for smooth transition
        LinearGradient baseVignette = new LinearGradient(
            0, h * 0.65f, 0, h,
            Color.TRANSPARENT,
            Color.argb(180, 241, 245, 249),
            Shader.TileMode.CLAMP
        );
        paint.setShader(baseVignette);
        canvas.drawRect(0, h * 0.65f, w, h, paint);
        paint.setShader(null);
    }
}
