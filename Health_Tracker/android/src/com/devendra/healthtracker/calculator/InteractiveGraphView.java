package com.devendra.healthtracker.calculator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.Locale;

/**
 * Interactive 2D Function Plotting Canvas:
 * - Plots y = f(x) over [xMin, xMax]
 * - Renders dynamic grid lines, numeric tick labels, and axes
 * - Handles asymptotes and discontinuities gracefully (no vertical artifacts)
 * - Multi-touch pinch-to-zoom and drag-to-pan
 */
public class InteractiveGraphView extends View {

    private double xMin = -10.0;
    private double xMax = 10.0;
    private double yMin = -10.0;
    private double yMax = 10.0;

    private String functionExpression = "sin(x)";
    private ASTNodes.ExpressionNode parsedAst = null;
    private final EvaluationContext evalContext = new EvaluationContext();
    private String parseError = null;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint errorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path curvePath = new Path();
    private ScaleGestureDetector scaleGestureDetector;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isPanning = false;

    public InteractiveGraphView(Context context) {
        super(context);
        init(context);
    }

    public InteractiveGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public InteractiveGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;

        bgPaint.setColor(0xFF0B0E14); // Obsidian Midnight
        bgPaint.setStyle(Paint.Style.FILL);

        axisPaint.setColor(0xFF64748B); // Slate 500
        axisPaint.setStrokeWidth(2f * density);
        axisPaint.setStyle(Paint.Style.STROKE);

        gridPaint.setColor(0xFF1E293B); // Slate 800
        gridPaint.setStrokeWidth(1f * density);
        gridPaint.setStyle(Paint.Style.STROKE);

        textPaint.setColor(0xFF94A3B8); // Slate 400
        textPaint.setTextSize(10f * density);

        curvePaint.setColor(0xFFF59E0B); // Amber 500
        curvePaint.setStrokeWidth(2.5f * density);
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeCap(Paint.Cap.ROUND);
        curvePaint.setStrokeJoin(Paint.Join.ROUND);

        errorPaint.setColor(0xFFEF4444); // Red 500
        errorPaint.setTextSize(12f * density);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                if (scaleFactor > 0.1f && scaleFactor < 10f) {
                    zoom(1.0 / scaleFactor);
                }
                return true;
            }
        });

        setFunction(functionExpression);
    }

    public void setFunction(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            this.functionExpression = "";
            this.parsedAst = null;
            this.parseError = "No expression";
            invalidate();
            return;
        }

        this.functionExpression = expr.trim();
        try {
            this.parsedAst = ExpressionParser.parse(this.functionExpression);
            this.parseError = null;
        } catch (Exception e) {
            this.parsedAst = null;
            this.parseError = e.getMessage() != null ? e.getMessage() : "Syntax error";
        }
        invalidate();
    }

    public String getFunction() {
        return functionExpression;
    }

    public void zoom(double factor) {
        double xCenter = (xMin + xMax) / 2.0;
        double yCenter = (yMin + yMax) / 2.0;
        double xSpan = (xMax - xMin) * factor / 2.0;
        double ySpan = (yMax - yMin) * factor / 2.0;

        if (xSpan > 1e-6 && xSpan < 1e7 && ySpan > 1e-6 && ySpan < 1e7) {
            xMin = xCenter - xSpan;
            xMax = xCenter + xSpan;
            yMin = yCenter - ySpan;
            yMax = yCenter + ySpan;
            invalidate();
        }
    }

    public void resetView() {
        xMin = -10.0;
        xMax = 10.0;
        yMin = -10.0;
        yMax = 10.0;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scaleGestureDetector != null) {
            scaleGestureDetector.onTouchEvent(event);
        }

        if (event.getPointerCount() > 1) {
            isPanning = false;
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isPanning = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isPanning) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;

                    int width = getWidth();
                    int height = getHeight();
                    if (width > 0 && height > 0) {
                        double deltaMathX = -(dx / (double) width) * (xMax - xMin);
                        double deltaMathY = (dy / (double) height) * (yMax - yMin);

                        xMin += deltaMathX;
                        xMax += deltaMathX;
                        yMin += deltaMathY;
                        yMax += deltaMathY;
                        invalidate();
                    }

                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isPanning = false;
                return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        // 1. Background
        canvas.drawRect(0, 0, width, height, bgPaint);

        // 2. Compute dynamic grid step
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;
        if (xRange <= 0 || yRange <= 0) return;

        double xStep = computeGridStep(xRange);
        double yStep = computeGridStep(yRange);

        // 3. Draw Grid lines & labels
        double firstX = Math.floor(xMin / xStep) * xStep;
        for (double x = firstX; x <= xMax; x += xStep) {
            float sx = toScreenX(x, width);
            canvas.drawLine(sx, 0, sx, height, gridPaint);
            if (Math.abs(x) > 1e-9) {
                String label = formatTickLabel(x);
                canvas.drawText(label, sx + 4, height - 6, textPaint);
            }
        }

        double firstY = Math.floor(yMin / yStep) * yStep;
        for (double y = firstY; y <= yMax; y += yStep) {
            float sy = toScreenY(y, height);
            canvas.drawLine(0, sy, width, sy, gridPaint);
            if (Math.abs(y) > 1e-9) {
                String label = formatTickLabel(y);
                canvas.drawText(label, 6, sy - 4, textPaint);
            }
        }

        // 4. Draw X and Y Axes
        if (yMin <= 0 && yMax >= 0) {
            float sy0 = toScreenY(0, height);
            canvas.drawLine(0, sy0, width, sy0, axisPaint);
        }
        if (xMin <= 0 && xMax >= 0) {
            float sx0 = toScreenX(0, width);
            canvas.drawLine(sx0, 0, sx0, height, axisPaint);
        }

        // 5. Draw Function Curve or Error
        if (parseError != null) {
            canvas.drawText("Error: " + parseError, 20, height / 2f, errorPaint);
            return;
        }

        if (parsedAst == null) return;

        curvePath.reset();
        boolean hasStarted = false;
        double prevY = Double.NaN;
        float prevSy = 0f;

        // Sample across pixels
        int steps = Math.min(width, 600);
        float stepPx = (float) width / (float) steps;

        for (int i = 0; i <= steps; i++) {
            float sx = i * stepPx;
            double x = toMathX(sx, width);

            double y = Double.NaN;
            try {
                evalContext.setVariable("x", x);
                ComplexNumber z = parsedAst.evaluate(evalContext);
                y = z != null ? z.real : Double.NaN;
            } catch (Exception e) {
                y = Double.NaN;
            }

            if (Double.isNaN(y) || Double.isInfinite(y) || Math.abs(y) > 1e8) {
                hasStarted = false;
                prevY = Double.NaN;
                continue;
            }

            float sy = toScreenY(y, height);

            // Asymptote detection: large jump with sign flip (e.g. tan(x), 1/x)
            if (!Double.isNaN(prevY)) {
                if ((prevY * y < 0) && Math.abs(prevSy - sy) > (height * 0.7f)) {
                    hasStarted = false; // Discontinuity gap
                }
            }

            if (!hasStarted) {
                curvePath.moveTo(sx, sy);
                hasStarted = true;
            } else {
                curvePath.lineTo(sx, sy);
            }

            prevY = y;
            prevSy = sy;
        }

        canvas.drawPath(curvePath, curvePaint);
    }

    private float toScreenX(double x, int width) {
        return (float) ((x - xMin) / (xMax - xMin) * width);
    }

    private float toScreenY(double y, int height) {
        return (float) (height - (y - yMin) / (yMax - yMin) * height);
    }

    private double toMathX(float sx, int width) {
        return xMin + (sx / (double) width) * (xMax - xMin);
    }

    private double computeGridStep(double range) {
        double rawStep = range / 8.0;
        double power = Math.pow(10, Math.floor(Math.log10(rawStep)));
        double norm = rawStep / power;

        if (norm < 1.5) return power;
        if (norm < 3.5) return 2 * power;
        if (norm < 7.5) return 5 * power;
        return 10 * power;
    }

    private String formatTickLabel(double val) {
        if (Math.abs(val) >= 10000 || (Math.abs(val) > 0 && Math.abs(val) < 0.01)) {
            return String.format(Locale.US, "%.1e", val);
        }
        if (Math.floor(val) == val) {
            return String.format(Locale.US, "%.0f", val);
        }
        return String.format(Locale.US, "%.2f", val);
    }

    public void setDarkTheme(boolean isDark) {
        if (isDark) {
            bgPaint.setColor(0xFF181818);
            gridPaint.setColor(0xFF282828);
            axisPaint.setColor(0xFF454545);
            textPaint.setColor(0xFF858585);
            curvePaint.setColor(0xFFFBBF24);
        } else {
            bgPaint.setColor(0xFF0F172A);
            gridPaint.setColor(0xFF1E293B);
            axisPaint.setColor(0xFF64748B);
            textPaint.setColor(0xFF94A3B8);
            curvePaint.setColor(0xFFF59E0B);
        }
        invalidate();
    }
}
