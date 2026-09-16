package com.devendra.healthtracker.calculator;

import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Numerical Precision and Context Configuration.
 * Governs BigDecimal calculation precision, explicit rounding modes,
 * display representations (Standard, Scientific, Engineering), and IEEE-754 numerical tolerances.
 */
public class MathContextConfig {
    // Standard numerical tolerances
    public static final double DEFAULT_EPSILON = 1e-12;
    public static final double STRICT_EPSILON = 1e-15;

    // High-precision MathContext for intermediate scientific calculations
    public static final MathContext HIGH_PRECISION_CONTEXT = new MathContext(64, RoundingMode.HALF_EVEN);
    // Standard calculation MathContext
    public static final MathContext STANDARD_CALC_CONTEXT = new MathContext(34, RoundingMode.HALF_EVEN); // IEEE 754 decimal128 equivalent
    // Display MathContext
    public static final MathContext DISPLAY_CONTEXT = new MathContext(16, RoundingMode.HALF_EVEN);

    public enum DisplayNotation {
        STANDARD,
        SCIENTIFIC,
        ENGINEERING
    }

    private int displayPrecision;
    private RoundingMode roundingMode;
    private DisplayNotation notation;
    private boolean useSignificantFigures;
    private int significantFigures;

    public MathContextConfig() {
        this.displayPrecision = 10;
        this.roundingMode = RoundingMode.HALF_EVEN; // Banker's rounding
        this.notation = DisplayNotation.STANDARD;
        this.useSignificantFigures = false;
        this.significantFigures = 8;
    }

    public int getDisplayPrecision() {
        return displayPrecision;
    }

    public void setDisplayPrecision(int displayPrecision) {
        if (displayPrecision < 0 || displayPrecision > 30) {
            throw new IllegalArgumentException("Display precision must be between 0 and 30");
        }
        this.displayPrecision = displayPrecision;
    }

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    public void setRoundingMode(RoundingMode roundingMode) {
        if (roundingMode == null) throw new IllegalArgumentException("RoundingMode cannot be null");
        this.roundingMode = roundingMode;
    }

    public DisplayNotation getNotation() {
        return notation;
    }

    public void setNotation(DisplayNotation notation) {
        if (notation == null) throw new IllegalArgumentException("DisplayNotation cannot be null");
        this.notation = notation;
    }

    public boolean isUseSignificantFigures() {
        return useSignificantFigures;
    }

    public void setUseSignificantFigures(boolean useSignificantFigures) {
        this.useSignificantFigures = useSignificantFigures;
    }

    public int getSignificantFigures() {
        return significantFigures;
    }

    public void setSignificantFigures(int significantFigures) {
        if (significantFigures < 1 || significantFigures > 30) {
            throw new IllegalArgumentException("Significant figures must be between 1 and 30");
        }
        this.significantFigures = significantFigures;
    }

    public MathContextConfig(int displayPrecision, RoundingMode roundingMode, DisplayNotation notation, int significantFigures) {
        this.displayPrecision = displayPrecision;
        this.roundingMode = roundingMode;
        this.notation = notation;
        this.useSignificantFigures = false;
        this.significantFigures = significantFigures;
    }

    public MathContext toMathContext() {
        return new MathContext(displayPrecision, roundingMode);
    }

    public MathContext getMathContext() {
        return toMathContext();
    }

    public String format(double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        if (Math.abs(value) < 1e-15) return "0";

        switch (notation) {
            case SCIENTIFIC:
                return String.format(java.util.Locale.US, "%." + Math.min(displayPrecision, 10) + "e", value);
            case ENGINEERING: {
                double absVal = Math.abs(value);
                int exp = (int) Math.floor(Math.log10(absVal) / 3.0) * 3;
                double mantissa = value / Math.pow(10, exp);
                String mantStr = formatDecimal(mantissa);
                return exp == 0 ? mantStr : mantStr + "e" + (exp > 0 ? "+" + exp : exp);
            }
            case STANDARD:
            default: {
                double absVal = Math.abs(value);
                if (absVal >= 1e12 || absVal < 1e-6) {
                    return String.format(java.util.Locale.US, "%." + Math.min(displayPrecision, 8) + "e", value);
                }
                return formatDecimal(value);
            }
        }
    }

    public String format(ComplexNumber z) {
        if (z == null) return "0";
        if (z.isReal()) return format(z.real);
        if (Math.abs(z.real) < 1e-13) {
            return (Math.abs(z.imag - 1.0) < 1e-13 ? "" : (Math.abs(z.imag + 1.0) < 1e-13 ? "-" : format(z.imag))) + "i";
        }
        String sign = z.imag >= 0 ? " + " : " - ";
        double absImag = Math.abs(z.imag);
        String imagStr = (Math.abs(absImag - 1.0) < 1e-13) ? "i" : (format(absImag) + "i");
        return format(z.real) + sign + imagStr;
    }

    public String format(java.math.BigDecimal bd) {
        if (bd == null) return "0";
        return bd.stripTrailingZeros().toPlainString();
    }

    private String formatDecimal(double val) {
        if (Math.floor(val) == val && Math.abs(val) < 1e15) {
            return String.format(java.util.Locale.US, "%.0f", val);
        }
        String s = String.format(java.util.Locale.US, "%." + displayPrecision + "f", val);
        // Strip trailing zeros
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
                break;
            }
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}