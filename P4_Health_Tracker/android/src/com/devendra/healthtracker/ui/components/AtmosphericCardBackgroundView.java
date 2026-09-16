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
 * AtmosphericCardBackgroundView
 * Renders custom, evocative procedural canvas art unique to each capability card.
 * Concept: Deep pain transformed into quiet, formidable darkness—silent lonely nights,
 * cold obsidian horizons, dead moons, solitary ruins, and ethereal abyssal depths.
 * Designed with artistic subtlety and non-literal, atmospheric symbolism.
 */
public class AtmosphericCardBackgroundView extends View {

    private final String featureId;
    private boolean isDarkTheme = true;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    // Deterministic particles for background atmosphere
    private static final int PARTICLE_COUNT = 36;
    private final float[] pX = new float[PARTICLE_COUNT];
    private final float[] pY = new float[PARTICLE_COUNT];
    private final float[] pR = new float[PARTICLE_COUNT];
    private final int[] pAlpha = new int[PARTICLE_COUNT];

    public AtmosphericCardBackgroundView(Context context, String featureId, boolean isDarkTheme) {
        super(context);
        this.featureId = featureId != null ? featureId : "";
        this.isDarkTheme = isDarkTheme;
        initParticles();
    }

    private void initParticles() {
        int seed = featureId.hashCode();
        Random rand = new Random(seed);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            pX[i] = rand.nextFloat();
            pY[i] = rand.nextFloat();
            pR[i] = 1.0f + rand.nextFloat() * 2.5f;
            pAlpha[i] = 40 + rand.nextInt(140);
        }
    }

    public void setDarkTheme(boolean isDark) {
        if (this.isDarkTheme != isDark) {
            this.isDarkTheme = isDark;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        switch (featureId) {
            case "routine":
                drawRoutineAtmosphere(canvas, w, h);
                break;
            case "pomodoro":
                drawPomodoroAtmosphere(canvas, w, h);
                break;
            case "checklist":
                drawChecklistAtmosphere(canvas, w, h);
                break;
            case "wellbeing":
                drawWellbeingAtmosphere(canvas, w, h);
                break;
            case "sysmon":
                drawMonitorAtmosphere(canvas, w, h);
                break;
            case "deepclean":
                drawCleanerAtmosphere(canvas, w, h);
                break;
            case "hydration":
                drawHydrationAtmosphere(canvas, w, h);
                break;
            case "blueprint":
                drawBlueprintAtmosphere(canvas, w, h);
                break;
            case "media":
                drawMediaAtmosphere(canvas, w, h);
                break;
            case "calculator":
                drawCalculatorAtmosphere(canvas, w, h);
                break;
            case "calendar":
                drawCalendarAtmosphere(canvas, w, h);
                break;
            default:
                drawDefaultAtmosphere(canvas, w, h);
                break;
        }

        // Global base vignette gradient to ensure foreground text and translucent cards pop with crisp readability
        drawGlobalVignette(canvas, w, h);
    }

    private void drawGlobalVignette(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        int topScrim = isDarkTheme ? Color.argb(120, 8, 10, 18) : Color.argb(80, 245, 247, 250);
        int midScrim = isDarkTheme ? Color.argb(60, 6, 8, 14) : Color.argb(40, 240, 243, 246);
        int bottomScrim = isDarkTheme ? Color.argb(190, 4, 6, 10) : Color.argb(140, 226, 232, 240);

        LinearGradient vg = new LinearGradient(
            0, 0, 0, h,
            new int[]{topScrim, midScrim, bottomScrim},
            new float[]{0.0f, 0.45f, 1.0f},
            Shader.TileMode.CLAMP
        );
        paint.setShader(vg);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);
    }

    // 1. ROUTINE: The Lonely Endless Road through Dead Forest
    private void drawRoutineAtmosphere(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        LinearGradient sky = new LinearGradient(
            0, 0, 0, h * 0.75f,
            isDarkTheme
                ? new int[]{Color.parseColor("#09060E"), Color.parseColor("#1B1024"), Color.parseColor("#3B1D38"), Color.parseColor("#0D0B12")}
                : new int[]{Color.parseColor("#E0E7FF"), Color.parseColor("#DDD6FE"), Color.parseColor("#C4B5FD"), Color.parseColor("#EDE9FE")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(sky);
        canvas.drawRect(0, 0, w, h, paint);

        // Distant fractured ring moon
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(isDarkTheme ? Color.argb(90, 244, 63, 94) : Color.argb(120, 168, 85, 247));
        canvas.drawCircle(w * 0.78f, h * 0.22f, dp(38), paint);
        paint.setStyle(Paint.Style.FILL);

        // Mountain ridge
        path.reset();
        path.moveTo(0, h * 0.58f);
        path.lineTo(w * 0.25f, h * 0.50f);
        path.lineTo(w * 0.52f, h * 0.56f);
        path.lineTo(w * 0.78f, h * 0.48f);
        path.lineTo(w, h * 0.54f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        paint.setColor(isDarkTheme ? Color.parseColor("#120A1A") : Color.parseColor("#818CF8"));
        canvas.drawPath(path, paint);

        // Winding desolate road
        path.reset();
        path.moveTo(w * 0.48f, h * 0.56f);
        path.quadTo(w * 0.40f, h * 0.72f, w * 0.15f, h);
        path.lineTo(w * 0.85f, h);
        path.quadTo(w * 0.60f, h * 0.72f, w * 0.52f, h * 0.56f);
        path.close();
        paint.setColor(isDarkTheme ? Color.parseColor("#08050B") : Color.parseColor("#475569"));
        canvas.drawPath(path, paint);

        // Twisted leafless silhouette trees along the road
        paint.setColor(isDarkTheme ? Color.parseColor("#040306") : Color.parseColor("#1E1B4B"));
        paint.setStrokeWidth(dp(2));
        drawTwistedTree(canvas, w * 0.18f, h * 0.82f, dp(55));
        drawTwistedTree(canvas, w * 0.82f, h * 0.78f, dp(65));
    }

    // 2. POMODORO: The Smoldering Eclipse & Crowning of Darkness
    private void drawPomodoroAtmosphere(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        LinearGradient sky = new LinearGradient(
            0, 0, 0, h * 0.75f,
            isDarkTheme
                ? new int[]{Color.parseColor("#0A0304"), Color.parseColor("#20080C"), Color.parseColor("#4A0E17"), Color.parseColor("#140407")}
                : new int[]{Color.parseColor("#FFE4E6"), Color.parseColor("#FECDD3"), Color.parseColor("#FDA4AF"), Color.parseColor("#FFF1F2")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(sky);
        canvas.drawRect(0, 0, w, h, paint);

        // Black Sun / Solar Eclipse
        float ex = w * 0.35f;
        float ey = h * 0.26f;
        float er = dp(42);

        RadialGradient corona = new RadialGradient(
            ex, ey, er * 2.8f,
            new int[]{Color.argb(190, 225, 29, 72), Color.argb(80, 190, 18, 60), Color.TRANSPARENT},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(corona);
        canvas.drawCircle(ex, ey, er * 2.8f, paint);

        paint.setShader(null);
        paint.setColor(isDarkTheme ? Color.parseColor("#070203") : Color.parseColor("#1F070A"));
        canvas.drawCircle(ex, ey, er, paint);

        // Jagged towering cliff silhouette
        path.reset();
        path.moveTo(0, h * 0.52f);
        path.lineTo(w * 0.38f, h * 0.58f);
        path.lineTo(w * 0.50f, h * 0.50f);
        path.lineTo(w * 0.62f, h * 0.66f);
        path.lineTo(w, h * 0.60f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        paint.setColor(isDarkTheme ? Color.parseColor("#0D0305") : Color.parseColor("#9F1239"));
        canvas.drawPath(path, paint);

        // Floating ember particles rising
        paint.setColor(Color.parseColor("#FB7185"));
        for (int i = 0; i < 18; i++) {
            paint.setAlpha(60 + (i * 10));
            canvas.drawCircle(pX[i] * w, (pY[i] * 0.6f + 0.4f) * h, pR[i] * 1.2f, paint);
        }
    }

    // 3. CHECKLIST: Solitary Broken Pillars in Ash
    private void drawChecklistAtmosphere(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        LinearGradient sky = new LinearGradient(
            0, 0, 0, h * 0.75f,
            isDarkTheme
                ? new int[]{Color.parseColor("#040C08"), Color.parseColor("#0B1F17"), Color.parseColor("#17362B"), Color.parseColor("#06120D")}
                : new int[]{Color.parseColor("#ECFDF5"), Color.parseColor("#D1FAE5"), Color.parseColor("#A7F3D0"), Color.parseColor("#F0FDF4")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(sky);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Broken classical pillars standing in the mist
        paint.setColor(isDarkTheme ? Color.parseColor("#091812") : Color.parseColor("#34D399"));
        // Pillar 1 (Tall, fractured top)
        canvas.drawRect(w * 0.16f, h * 0.38f, w * 0.22f, h * 0.75f, paint);
        // Pillar 2 (Shattered half)
        canvas.drawRect(w * 0.74f, h * 0.48f, w * 0.80f, h * 0.75f, paint);
        // Arch remnant
        path.reset();
        path.moveTo(w * 0.14f, h * 0.38f);
        path.lineTo(w * 0.24f, h * 0.38f);
        path.lineTo(w * 0.28f, h * 0.44f);
        path.close();
        canvas.drawPath(path, paint);

        // Ground plateau
        path.reset();
        path.moveTo(0, h * 0.72f);
        path.quadTo(w * 0.4f, h * 0.68f, w, h * 0.74f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        paint.setColor(isDarkTheme ? Color.parseColor("#050E0A") : Color.parseColor("#065F46"));
        canvas.drawPath(path, paint);
    }

    // 4. WELLBEING: The Crying Moon & Infinite Frozen Desert
    private void drawWellbeingAtmosphere(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        LinearGradient sky = new LinearGradient(
            0, 0, 0, h * 0.75f,
            isDarkTheme
                ? new int[]{Color.parseColor("#040813"), Color.parseColor("#0C172E"), Color.parseColor("#182B52"), Color.parseColor("#080F1E")}
                : new int[]{Color.parseColor("#F0F9FF"), Color.parseColor("#E0F2FE"), Color.parseColor("#BAE6FD"), Color.parseColor("#F8FAFC")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(sky);
        canvas.drawRect(0, 0, w, h, paint);

        // Pale weeping moon
        float mx = w * 0.72f;
        float my = h * 0.20f;
        float mr = dp(32);
        RadialGradient mg = new RadialGradient(
            mx, my, mr * 2.5f,
            new int[]{Color.argb(140, 56, 189, 248), Color.argb(40, 14, 165, 233), Color.TRANSPARENT},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(mg);
        canvas.drawCircle(mx, my, mr * 2.5f, paint);

        paint.setShader(null);
        paint.setColor(isDarkTheme ? Color.parseColor("#E0F2FE") : Color.WHITE);
        canvas.drawCircle(mx, my, mr, paint);

        // Cry light beam downward
        paint.setColor(isDarkTheme ? Color.argb(35, 56, 189, 248) : Color.argb(45, 14, 165, 233));
        path.reset();
        path.moveTo(mx - dp(12), my + mr);
        path.lineTo(mx + dp(12), my + mr);
        path.lineTo(w * 0.65f, h);
        path.lineTo(w * 0.45f, h);
        path.close();
        canvas.drawPath(path, paint);

        // Cracked permafrost horizon
        path.reset();
        path.moveTo(0, h * 0.64f);
        path.lineTo(w * 0.35f, h * 0.68f);
        path.lineTo(w * 0.65f, h * 0.62f);
        path.lineTo(w, h * 0.66f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        paint.setColor(isDarkTheme ? Color.parseColor("#060C18") : Color.parseColor("#0284C7"));
        canvas.drawPath(path, paint);
    }

    // 5. MONITOR: The Electric Wasteland & Silent Needle
    private void drawMonitorAtmosphere(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        LinearGradient sky = new LinearGradient(
            0, 0, 0, h * 0.75f,
            isDarkTheme
                ? new int[]{Color.parseColor("#070512"), Color.parseColor("#150F30"), Color.parseColor("#271C56"), Color.parseColor("#0A071A")}
                : new int[]{Color.parseColor("#EEF2FF"), Color.parseColor("#E0E7FF"), Color.parseColor("#C7D2FE"), Color.parseColor("#F5F3FF")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(sky);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Transmission Needle / Spire silhouette
        float tx = w * 0.32f;
        float ty = h * 0.28f;
        paint.setColor(isDarkTheme ? Color.parseColor("#110D27") : Color.parseColor("#6366F1"));
        canvas.drawRect(tx - dp(2), ty, tx + dp(2), h * 0.65f, paint);

        // Concentric pulse rings from needle tip
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(isDarkTheme ? Color.argb(120, 129, 140, 248) : Color.argb(160, 99, 102, 241));
        canvas.drawCircle(tx, ty, dp(18), paint);
        canvas.drawCircle(tx, ty, dp(38), paint);
        canvas.drawCircle(tx, ty, dp(64), paint);
        paint.setStyle(Paint.Style.FILL);

        // Jagged jagged tectonic mountain base
        path.reset();
        path.moveTo(0, h * 0.60f);
        path.lineTo(w * 0.32f, h * 0.52f);
        path.lineTo(w * 0.64f, h * 0.62f);
        path.lineTo(w, h * 0.55f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        paint.setColor(isDarkTheme ? Color.parseColor("#0A0718") : Color.parseColor("#4338CA"));
        canvas.drawPath(path, paint);
    }

    // 6. CLEANER: The Gravitational Void & Incineration
    private void drawCleanerAtmosphere(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        LinearGradient sky = new LinearGradient(
            0, 0, 0, h * 0.75f,
            isDarkTheme
                ? new int[]{Color.parseColor("#030D0C"), Color.parseColor("#082220"), Color.parseColor("#103D39"), Color.parseColor("#041211")}
                : new int[]{Color.parseColor("#F0FDFA"), Color.parseColor("#CCFBF1"), Color.parseColor("#99F6E4"), Color.parseColor("#F5F5F5")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(sky);
        canvas.drawRect(0, 0, w, h, paint);

        // Singularity Vortex in center-right
        float vx = w * 0.68f;
        float vy = h * 0.30f;
        float vr = dp(46);

        RadialGradient vg = new RadialGradient(
            vx, vy, vr * 2.6f,
            new int[]{Color.argb(160, 20, 184, 166), Color.argb(60, 13, 148, 136), Color.TRANSPARENT},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(vg);
        canvas.drawCircle(vx, vy, vr * 2.6f, paint);

        paint.setShader(null);
        paint.setColor(isDarkTheme ? Color.parseColor("#020807") : Color.parseColor("#115E59"));
        canvas.drawCircle(vx, vy, vr, paint);

        // Dark tectonic plates collapsing
        path.reset();
        path.moveTo(0, h * 0.66f);
        path.lineTo(w * 0.42f, h * 0.58f);
        path.lineTo(w * 0.82f, h * 0.68f);
        path.lineTo(w, h * 0.62f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        paint.setColor(isDarkTheme ? Color.parseColor("#030B0A") : Color.parseColor("#0F766E"));
        canvas.drawPath(path, paint);
    }

    // 7. HYDRATION: The Abyssal Deep Trench
    private void drawHydrationAtmosphere(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        LinearGradient deepSea = new LinearGradient(
            0, 0, 0, h,
            isDarkTheme
                ? new int[]{Color.parseColor("#020710"), Color.parseColor("#06172E"), Color.parseColor("#0B294F"), Color.parseColor("#030A14")}
                : new int[]{Color.parseColor("#F0F9FF"), Color.parseColor("#E0F2FE"), Color.parseColor("#7DD3FC"), Color.parseColor("#0284C7")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(deepSea);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Underwater monolithic canyon walls
        path.reset();
        path.moveTo(0, h * 0.35f);
        path.lineTo(w * 0.28f, h * 0.55f);
        path.lineTo(w * 0.18f, h);
        path.lineTo(0, h);
        path.close();
        paint.setColor(isDarkTheme ? Color.parseColor("#030B17") : Color.parseColor("#0369A1"));
        canvas.drawPath(path, paint);

        path.reset();
        path.moveTo(w, h * 0.40f);
        path.lineTo(w * 0.72f, h * 0.62f);
        path.lineTo(w * 0.82f, h);
        path.lineTo(w, h);
        path.close();
        canvas.drawPath(path, paint);

        // Bioluminescent rising bubbles
        paint.setColor(isDarkTheme ? Color.argb(120, 56, 189, 248) : Color.argb(160, 2, 132, 199));
        for (int i = 0; i < 24; i++) {
            canvas.drawCircle(pX[i] * w, pY[i] * h, pR[i] * 1.5f, paint);
        }
    }

    // 8. BLUEPRINT: The Tower of Solitude & Mind Fortress
    private void drawBlueprintAtmosphere(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        LinearGradient sky = new LinearGradient(
            0, 0, 0, h * 0.75f,
            isDarkTheme
                ? new int[]{Color.parseColor("#0A040E"), Color.parseColor("#200D2B"), Color.parseColor("#441A5C"), Color.parseColor("#110617")}
                : new int[]{Color.parseColor("#FAF5FF"), Color.parseColor("#F3E8FF"), Color.parseColor("#D8B4FE"), Color.parseColor("#F5F3FF")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(sky);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Monolith / Citadel Tower rising into the dark
        float cx = w * 0.5f;
        float cy = h * 0.22f;
        path.reset();
        path.moveTo(cx, cy); // Needle tip
        path.lineTo(cx + dp(28), h * 0.75f);
        path.lineTo(cx - dp(28), h * 0.75f);
        path.close();
        paint.setColor(isDarkTheme ? Color.parseColor("#12071C") : Color.parseColor("#7E22CE"));
        canvas.drawPath(path, paint);

        // Crimson beacon at the top of the tower
        paint.setColor(Color.parseColor("#E11D48"));
        canvas.drawCircle(cx, cy, dp(4), paint);
        paint.setColor(Color.argb(90, 225, 29, 72));
        canvas.drawCircle(cx, cy, dp(14), paint);

        // Rolling fog bed covering the ground
        LinearGradient fog = new LinearGradient(
            0, h * 0.65f, 0, h,
            Color.TRANSPARENT,
            isDarkTheme ? Color.argb(180, 10, 4, 14) : Color.argb(160, 243, 232, 255),
            Shader.TileMode.CLAMP
        );
        paint.setShader(fog);
        canvas.drawRect(0, h * 0.65f, w, h, paint);
        paint.setShader(null);
    }

    // 9. MEDIA: The Phantom Web in the Amethyst Void
    private void drawMediaAtmosphere(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        LinearGradient sky = new LinearGradient(
            0, 0, 0, h * 0.75f,
            isDarkTheme
                ? new int[]{Color.parseColor("#09040D"), Color.parseColor("#1B0B27"), Color.parseColor("#37144F"), Color.parseColor("#0C0512")}
                : new int[]{Color.parseColor("#FDF4FF"), Color.parseColor("#FAE8FF"), Color.parseColor("#E879F9"), Color.parseColor("#F3E8FF")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(sky);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Web filaments connecting across the abyss
        paint.setColor(isDarkTheme ? Color.argb(70, 192, 132, 252) : Color.argb(90, 147, 51, 234));
        paint.setStrokeWidth(dp(1));
        float ox = w * 0.5f;
        float oy = h * 0.32f;
        for (int i = 0; i < 12; i++) {
            float angle = (float) (i * Math.PI / 6.0);
            float ex = ox + (float) Math.cos(angle) * w * 0.5f;
            float ey = oy + (float) Math.sin(angle) * h * 0.35f;
            canvas.drawLine(ox, oy, ex, ey, paint);
        }

        // Floating monolith shards
        paint.setColor(isDarkTheme ? Color.parseColor("#100618") : Color.parseColor("#9333EA"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(w * 0.12f, h * 0.58f, w * 0.28f, h * 0.72f, paint);
        canvas.drawRect(w * 0.70f, h * 0.48f, w * 0.88f, h * 0.64f, paint);
    }

    // 10. CALCULATOR: The Cold Geometric Abyss
    private void drawCalculatorAtmosphere(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        LinearGradient sky = new LinearGradient(
            0, 0, 0, h * 0.75f,
            isDarkTheme
                ? new int[]{Color.parseColor("#0C0802"), Color.parseColor("#211604"), Color.parseColor("#422B06"), Color.parseColor("#120B02")}
                : new int[]{Color.parseColor("#FFFBEB"), Color.parseColor("#FEF3C7"), Color.parseColor("#FDE68A"), Color.parseColor("#FFFDF5")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(sky);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Geometric fractured angular planes
        path.reset();
        path.moveTo(0, h * 0.48f);
        path.lineTo(w * 0.45f, h * 0.58f);
        path.lineTo(w * 0.30f, h * 0.75f);
        path.lineTo(0, h * 0.80f);
        path.close();
        paint.setColor(isDarkTheme ? Color.parseColor("#191003") : Color.parseColor("#D97706"));
        canvas.drawPath(path, paint);

        path.reset();
        path.moveTo(w, h * 0.42f);
        path.lineTo(w * 0.55f, h * 0.56f);
        path.lineTo(w * 0.68f, h * 0.78f);
        path.lineTo(w, h * 0.72f);
        path.close();
        paint.setColor(isDarkTheme ? Color.parseColor("#130C02") : Color.parseColor("#B45309"));
        canvas.drawPath(path, paint);

        // Golden rift in the center
        paint.setColor(isDarkTheme ? Color.argb(90, 245, 158, 11) : Color.argb(120, 217, 119, 6));
        paint.setStrokeWidth(dp(2));
        canvas.drawLine(w * 0.45f, h * 0.58f, w * 0.55f, h * 0.56f, paint);
        canvas.drawLine(w * 0.50f, h * 0.57f, w * 0.50f, h, paint);
    }

    // 11. CALENDAR: The Silent Rain & Withered Vigil
    private void drawCalendarAtmosphere(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        LinearGradient sky = new LinearGradient(
            0, 0, 0, h * 0.75f,
            isDarkTheme
                ? new int[]{Color.parseColor("#050811"), Color.parseColor("#0F1728"), Color.parseColor("#1E293B"), Color.parseColor("#090D17")}
                : new int[]{Color.parseColor("#F1F5F9"), Color.parseColor("#E2E8F0"), Color.parseColor("#CBD5E1"), Color.parseColor("#F8FAFC")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(sky);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Solitary hill with lone gnarled tree
        path.reset();
        path.moveTo(0, h * 0.72f);
        path.quadTo(w * 0.35f, h * 0.62f, w, h * 0.76f);
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        paint.setColor(isDarkTheme ? Color.parseColor("#070B14") : Color.parseColor("#334155"));
        canvas.drawPath(path, paint);

        // The lone gnarled tree standing vigil
        paint.setColor(isDarkTheme ? Color.parseColor("#03060C") : Color.parseColor("#0F172A"));
        drawTwistedTree(canvas, w * 0.35f, h * 0.62f, dp(75));

        // Diagonal silent rain streaks
        paint.setColor(isDarkTheme ? Color.argb(70, 148, 163, 184) : Color.argb(90, 71, 85, 105));
        paint.setStrokeWidth(dp(1));
        for (int i = 0; i < 35; i++) {
            float rx = pX[i] * w;
            float ry = pY[i] * h;
            canvas.drawLine(rx, ry, rx - dp(10), ry + dp(24), paint);
        }
    }

    private void drawDefaultAtmosphere(Canvas canvas, int w, int h) {
        paint.reset();
        paint.setAntiAlias(true);
        LinearGradient g = new LinearGradient(
            0, 0, 0, h,
            isDarkTheme
                ? new int[]{Color.parseColor("#0A0C14"), Color.parseColor("#151824"), Color.parseColor("#08090F")}
                : new int[]{Color.parseColor("#F8FAFC"), Color.parseColor("#E2E8F0"), Color.parseColor("#CBD5E1")},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(g);
        canvas.drawRect(0, 0, w, h, paint);
    }

    private void drawTwistedTree(Canvas canvas, float x, float y, float height) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        // Trunk
        path.reset();
        path.moveTo(x, y);
        path.cubicTo(x - dp(4), y - height * 0.4f, x + dp(6), y - height * 0.7f, x - dp(2), y - height);
        canvas.drawPath(path, paint);

        // Branches
        paint.setStrokeWidth(dp(2));
        canvas.drawLine(x - dp(2), y - height * 0.6f, x - dp(24), y - height * 0.85f, paint);
        canvas.drawLine(x - dp(24), y - height * 0.85f, x - dp(32), y - height * 0.75f, paint);

        canvas.drawLine(x + dp(3), y - height * 0.7f, x + dp(28), y - height * 0.92f, paint);
        canvas.drawLine(x + dp(28), y - height * 0.92f, x + dp(38), y - height * 0.82f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
