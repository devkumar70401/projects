package com.devendra.healthtracker.calculator;

import java.util.Objects;

/**
 * 2D and 3D Geometric and Physical Vector Subsystem.
 */
public final class Vector3D {
    public final double x;
    public final double y;
    public final double z;
    public final boolean is2D;

    public Vector3D(double x, double y) {
        this.x = x;
        this.y = y;
        this.z = 0.0;
        this.is2D = true;
    }

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.is2D = false;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public Vector3D add(Vector3D other) {
        return new Vector3D(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public Vector3D subtract(Vector3D other) {
        return new Vector3D(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vector3D multiply(double scalar) {
        return new Vector3D(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public double dot(Vector3D other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public Vector3D cross(Vector3D other) {
        return new Vector3D(
                this.y * other.z - this.z * other.y,
                this.z * other.x - this.x * other.z,
                this.x * other.y - this.y * other.x
        );
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3D normalize() {
        double mag = magnitude();
        if (mag < 1e-15) {
            throw new ArithmeticException("Cannot normalize zero-length vector");
        }
        return new Vector3D(x / mag, y / mag, z / mag);
    }

    /**
     * Calculates angle between vectors in radians.
     */
    public double angleBetween(Vector3D other) {
        double magProd = this.magnitude() * other.magnitude();
        if (magProd < 1e-15) {
            throw new ArithmeticException("Cannot compute angle with zero-length vector");
        }
        double cosTheta = this.dot(other) / magProd;
        // Clamp numerical precision to [-1.0, 1.0] for acos
        cosTheta = Math.max(-1.0, Math.min(1.0, cosTheta));
        return Math.acos(cosTheta);
    }

    /**
     * Projects this vector onto another vector: proj_v(u) = ((u · v) / |v|²) * v.
     */
    public Vector3D projectOnto(Vector3D target) {
        double targetMagSq = target.x * target.x + target.y * target.y + target.z * target.z;
        if (targetMagSq < 1e-15) {
            throw new ArithmeticException("Cannot project onto zero-length vector");
        }
        double factor = this.dot(target) / targetMagSq;
        return target.multiply(factor);
    }

    public String format(int decimals) {
        String fmt = "%." + decimals + "f";
        String xs = cleanZero(String.format(java.util.Locale.US, fmt, x));
        String ys = cleanZero(String.format(java.util.Locale.US, fmt, y));
        if (is2D && Math.abs(z) < 1e-15) {
            return "[" + xs + ", " + ys + "]";
        }
        String zs = cleanZero(String.format(java.util.Locale.US, fmt, z));
        return "[" + xs + ", " + ys + ", " + zs + "]";
    }

    private static String cleanZero(String s) {
        if (s.contains(".")) {
            while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    @Override
    public String toString() {
        return format(4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vector3D)) return false;
        Vector3D vector3D = (Vector3D) o;
        return Math.abs(vector3D.x - x) < 1e-12 &&
                Math.abs(vector3D.y - y) < 1e-12 &&
                Math.abs(vector3D.z - z) < 1e-12;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Math.round(x * 1e8), Math.round(y * 1e8), Math.round(z * 1e8));
    }
}