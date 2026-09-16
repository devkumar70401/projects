package com.devendra.healthtracker.calculator;

import java.util.Objects;

/**
 * Immutable High-Precision Complex Number Model (z = a + bi).
 * Implements Smith's method for numerically stable division.
 */
public final class ComplexNumber {
    public static final ComplexNumber ZERO = new ComplexNumber(0.0, 0.0);
    public static final ComplexNumber ONE = new ComplexNumber(1.0, 0.0);
    public static final ComplexNumber I = new ComplexNumber(0.0, 1.0);

    public final double real;
    public final double imag;

    public ComplexNumber(double real, double imag) {
        this.real = (Math.abs(real) < 1e-15) ? 0.0 : real;
        this.imag = (Math.abs(imag) < 1e-15) ? 0.0 : imag;
    }

    public double getReal() {
        return real;
    }

    public double getImaginary() {
        return imag;
    }

    public static ComplexNumber ofReal(double real) {
        return new ComplexNumber(real, 0.0);
    }

    public static ComplexNumber ofPolar(double r, double thetaRad) {
        if (r < 0) throw new IllegalArgumentException("Polar radius r cannot be negative");
        return new ComplexNumber(r * Math.cos(thetaRad), r * Math.sin(thetaRad));
    }

    public boolean isReal() {
        return Math.abs(imag) < 1e-13;
    }

    public boolean isPurelyImaginary() {
        return Math.abs(real) < 1e-13 && Math.abs(imag) >= 1e-13;
    }

    public ComplexNumber add(ComplexNumber other) {
        return new ComplexNumber(this.real + other.real, this.imag + other.imag);
    }

    public ComplexNumber subtract(ComplexNumber other) {
        return new ComplexNumber(this.real - other.real, this.imag - other.imag);
    }

    public ComplexNumber multiply(ComplexNumber other) {
        return new ComplexNumber(
                this.real * other.real - this.imag * other.imag,
                this.real * other.imag + this.imag * other.real
        );
    }

    public ComplexNumber multiply(double scalar) {
        return new ComplexNumber(this.real * scalar, this.imag * scalar);
    }

    /**
     * Numerically stable division using Smith's / Press algorithm to prevent intermediate overflow/underflow.
     */
    public ComplexNumber divide(ComplexNumber other) {
        double c = other.real;
        double d = other.imag;

        if (c == 0.0 && d == 0.0) {
            throw new ArithmeticException("Division by zero in complex arithmetic");
        }

        if (Math.abs(d) <= Math.abs(c)) {
            double ratio = d / c;
            double denom = c + d * ratio;
            return new ComplexNumber(
                    (this.real + this.imag * ratio) / denom,
                    (this.imag - this.real * ratio) / denom
            );
        } else {
            double ratio = c / d;
            double denom = c * ratio + d;
            return new ComplexNumber(
                    (this.real * ratio + this.imag) / denom,
                    (this.imag * ratio - this.real) / denom
            );
        }
    }

    public ComplexNumber divide(double scalar) {
        if (scalar == 0.0) throw new ArithmeticException("Division by zero");
        return new ComplexNumber(this.real / scalar, this.imag / scalar);
    }

    public double abs() {
        return Math.hypot(real, imag);
    }

    public double arg() {
        return Math.atan2(imag, real);
    }

    public ComplexNumber conjugate() {
        return new ComplexNumber(real, -imag);
    }

    public ComplexNumber negate() {
        return new ComplexNumber(-real, -imag);
    }

    public ComplexNumber reciprocal() {
        return ONE.divide(this);
    }

    public ComplexNumber sqrt() {
        double r = abs();
        double u = Math.sqrt((r + Math.abs(real)) / 2.0);
        double v = Math.abs(imag) / (2.0 * u);

        if (real >= 0) {
            return new ComplexNumber(u, Math.copySign(v, imag));
        } else {
            return new ComplexNumber(v, Math.copySign(u, imag));
        }
    }

    public ComplexNumber exp() {
        double expReal = Math.exp(real);
        return new ComplexNumber(expReal * Math.cos(imag), expReal * Math.sin(imag));
    }

    public ComplexNumber ln() {
        if (real == 0.0 && imag == 0.0) {
            throw new ArithmeticException("Domain error: ln(0) is undefined");
        }
        return new ComplexNumber(Math.log(abs()), arg());
    }

    public ComplexNumber pow(ComplexNumber exponent) {
        if (this.real == 0.0 && this.imag == 0.0) {
            if (exponent.real > 0) return ZERO;
            throw new ArithmeticException("Domain error: 0 raised to non-positive complex power");
        }
        return this.ln().multiply(exponent).exp();
    }

    public ComplexNumber pow(double exponent) {
        return pow(ComplexNumber.ofReal(exponent));
    }

    public ComplexNumber sin() {
        return new ComplexNumber(Math.sin(real) * Math.cosh(imag), Math.cos(real) * Math.sinh(imag));
    }

    public ComplexNumber cos() {
        return new ComplexNumber(Math.cos(real) * Math.cosh(imag), -Math.sin(real) * Math.sinh(imag));
    }

    public ComplexNumber tan() {
        return sin().divide(cos());
    }

    public ComplexNumber sinh() {
        return new ComplexNumber(Math.sinh(real) * Math.cos(imag), Math.cosh(real) * Math.sin(imag));
    }

    public ComplexNumber cosh() {
        return new ComplexNumber(Math.cosh(real) * Math.cos(imag), Math.sinh(real) * Math.sin(imag));
    }

    public ComplexNumber tanh() {
        return sinh().divide(cosh());
    }

    public String format(int precision) {
        String fmt = "%." + precision + "f";
        String rStr = String.format(java.util.Locale.US, fmt, real);
        String iStr = String.format(java.util.Locale.US, fmt, Math.abs(imag));

        // Trim trailing zeros
        rStr = trimZeros(rStr);
        iStr = trimZeros(iStr);

        if (isReal()) {
            return rStr;
        }
        if (Math.abs(real) < 1e-12) {
            return (imag < 0 ? "-" : "") + (iStr.equals("1") ? "" : iStr) + "i";
        }
        return rStr + (imag < 0 ? " - " : " + ") + (iStr.equals("1") ? "" : iStr) + "i";
    }

    private static String trimZeros(String s) {
        if (s.contains(".")) {
            while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    @Override
    public String toString() {
        return format(8);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComplexNumber)) return false;
        ComplexNumber that = (ComplexNumber) o;
        return Math.abs(that.real - real) < 1e-12 && Math.abs(that.imag - imag) < 1e-12;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Math.round(real * 1e8), Math.round(imag * 1e8));
    }
}