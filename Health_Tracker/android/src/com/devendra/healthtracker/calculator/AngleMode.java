package com.devendra.healthtracker.calculator;

/**
 * Explicit Angle Mode Subsystem (DEG, RAD, GRAD).
 * Guarantees mathematical correctness for all trigonometric evaluations.
 * Special handling for exact quadrant boundaries (e.g. sin(90°) = 1.0, cos(90°) = 0.0, tan(90°) = undefined).
 */
public enum AngleMode {
    DEG("DEG", "Degrees"),
    RAD("RAD", "Radians"),
    GRAD("GRAD", "Gradians");

    public final String code;
    public final String displayName;

    AngleMode(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Converts an angle in this mode to radians.
     */
    public double toRadians(double angle) {
        switch (this) {
            case DEG:
                return Math.toRadians(angle);
            case GRAD:
                return angle * (Math.PI / 200.0);
            case RAD:
            default:
                return angle;
        }
    }

    /**
     * Converts an angle in radians to this mode.
     */
    public double fromRadians(double rad) {
        switch (this) {
            case DEG:
                return Math.toDegrees(rad);
            case GRAD:
                return rad * (200.0 / Math.PI);
            case RAD:
            default:
                return rad;
        }
    }

    /**
     * Numerically robust sin calculation with exact integer quadrant snaps.
     */
    public double sin(double angle) {
        if (this == DEG) {
            double normalized = angle % 360.0;
            if (normalized < 0) normalized += 360.0;
            if (Math.abs(normalized) < 1e-13 || Math.abs(normalized - 180.0) < 1e-13 || Math.abs(normalized - 360.0) < 1e-13) {
                return 0.0;
            }
            if (Math.abs(normalized - 90.0) < 1e-13) return 1.0;
            if (Math.abs(normalized - 270.0) < 1e-13) return -1.0;
        } else if (this == GRAD) {
            double normalized = angle % 400.0;
            if (normalized < 0) normalized += 400.0;
            if (Math.abs(normalized) < 1e-13 || Math.abs(normalized - 200.0) < 1e-13 || Math.abs(normalized - 400.0) < 1e-13) {
                return 0.0;
            }
            if (Math.abs(normalized - 100.0) < 1e-13) return 1.0;
            if (Math.abs(normalized - 300.0) < 1e-13) return -1.0;
        }
        return Math.sin(toRadians(angle));
    }

    /**
     * Numerically robust cos calculation with exact integer quadrant snaps.
     */
    public double cos(double angle) {
        if (this == DEG) {
            double normalized = angle % 360.0;
            if (normalized < 0) normalized += 360.0;
            if (Math.abs(normalized - 90.0) < 1e-13 || Math.abs(normalized - 270.0) < 1e-13) {
                return 0.0;
            }
            if (Math.abs(normalized) < 1e-13 || Math.abs(normalized - 360.0) < 1e-13) return 1.0;
            if (Math.abs(normalized - 180.0) < 1e-13) return -1.0;
        } else if (this == GRAD) {
            double normalized = angle % 400.0;
            if (normalized < 0) normalized += 400.0;
            if (Math.abs(normalized - 100.0) < 1e-13 || Math.abs(normalized - 300.0) < 1e-13) {
                return 0.0;
            }
            if (Math.abs(normalized) < 1e-13 || Math.abs(normalized - 400.0) < 1e-13) return 1.0;
            if (Math.abs(normalized - 200.0) < 1e-13) return -1.0;
        }
        return Math.cos(toRadians(angle));
    }

    /**
     * Numerically robust tan calculation with asymptote detection.
     */
    public double tan(double angle) {
        double c = cos(angle);
        if (Math.abs(c) < 1e-15) {
            throw new ArithmeticException("Domain error: tan is undefined (asymptote at angle " + angle + " " + code + ")");
        }
        double s = sin(angle);
        if (Math.abs(s) < 1e-15) return 0.0;
        return s / c;
    }
}