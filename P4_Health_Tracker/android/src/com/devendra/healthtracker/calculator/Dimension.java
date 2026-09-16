package com.devendra.healthtracker.calculator;

import java.util.Objects;

/**
 * 7-Fundamental SI Base Dimensional Vector Subsystem.
 * Enforces dimensional correctness and automated dimensional reduction.
 * Base Dimensions: Mass (M), Length (L), Time (T), Current (I), Temperature (Θ), Amount (N), Luminous (J), Data (D).
 */
public final class Dimension {
    public final int mass;               // M (kg)
    public final int length;             // L (m)
    public final int time;               // T (s)
    public final int current;            // I (A)
    public final int temperature;       // Θ (K)
    public final int amount;             // N (mol)
    public final int luminousIntensity;  // J (cd)
    public final int data;               // D (bit)

    public static final Dimension DIMENSIONLESS = new Dimension(0, 0, 0, 0, 0, 0, 0, 0);
    public static final Dimension MASS = new Dimension(1, 0, 0, 0, 0, 0, 0, 0);
    public static final Dimension LENGTH = new Dimension(0, 1, 0, 0, 0, 0, 0, 0);
    public static final Dimension TIME = new Dimension(0, 0, 1, 0, 0, 0, 0, 0);
    public static final Dimension CURRENT = new Dimension(0, 0, 0, 1, 0, 0, 0, 0);
    public static final Dimension TEMPERATURE = new Dimension(0, 0, 0, 0, 1, 0, 0, 0);
    public static final Dimension AMOUNT = new Dimension(0, 0, 0, 0, 0, 1, 0, 0);
    public static final Dimension LUMINOUS = new Dimension(0, 0, 0, 0, 0, 0, 1, 0);
    public static final Dimension DATA = new Dimension(0, 0, 0, 0, 0, 0, 0, 1);

    // Derived Dimensions
    public static final Dimension AREA = LENGTH.multiply(LENGTH);                      // L²
    public static final Dimension VOLUME = AREA.multiply(LENGTH);                      // L³
    public static final Dimension VELOCITY = LENGTH.divide(TIME);                      // L / T
    public static final Dimension ACCELERATION = VELOCITY.divide(TIME);                // L / T²
    public static final Dimension FORCE = MASS.multiply(ACCELERATION);                 // M * L / T² (N)
    public static final Dimension PRESSURE = FORCE.divide(AREA);                       // M / (L * T²) (Pa)
    public static final Dimension ENERGY = FORCE.multiply(LENGTH);                     // M * L² / T² (J)
    public static final Dimension POWER = ENERGY.divide(TIME);                         // M * L² / T³ (W)
    public static final Dimension FREQUENCY = DIMENSIONLESS.divide(TIME);              // 1 / T (Hz)

    public Dimension(int mass, int length, int time, int current, int temperature, int amount, int luminousIntensity, int data) {
        this.mass = mass;
        this.length = length;
        this.time = time;
        this.current = current;
        this.temperature = temperature;
        this.amount = amount;
        this.luminousIntensity = luminousIntensity;
        this.data = data;
    }

    public boolean isDimensionless() {
        return mass == 0 && length == 0 && time == 0 && current == 0 &&
                temperature == 0 && amount == 0 && luminousIntensity == 0 && data == 0;
    }

    public Dimension multiply(Dimension other) {
        return new Dimension(
                this.mass + other.mass,
                this.length + other.length,
                this.time + other.time,
                this.current + other.current,
                this.temperature + other.temperature,
                this.amount + other.amount,
                this.luminousIntensity + other.luminousIntensity,
                this.data + other.data
        );
    }

    public Dimension divide(Dimension other) {
        return new Dimension(
                this.mass - other.mass,
                this.length - other.length,
                this.time - other.time,
                this.current - other.current,
                this.temperature - other.temperature,
                this.amount - other.amount,
                this.luminousIntensity - other.luminousIntensity,
                this.data - other.data
        );
    }

    public Dimension pow(int exponent) {
        return new Dimension(
                this.mass * exponent,
                this.length * exponent,
                this.time * exponent,
                this.current * exponent,
                this.temperature * exponent,
                this.amount * exponent,
                this.luminousIntensity * exponent,
                this.data * exponent
        );
    }

    public String getDerivedName() {
        if (isDimensionless()) return "Dimensionless";
        if (this.equals(LENGTH)) return "Length";
        if (this.equals(AREA)) return "Area";
        if (this.equals(VOLUME)) return "Volume";
        if (this.equals(MASS)) return "Mass";
        if (this.equals(TIME)) return "Time";
        if (this.equals(VELOCITY)) return "Velocity";
        if (this.equals(ACCELERATION)) return "Acceleration";
        if (this.equals(FORCE)) return "Force";
        if (this.equals(PRESSURE)) return "Pressure";
        if (this.equals(ENERGY)) return "Energy";
        if (this.equals(POWER)) return "Power";
        if (this.equals(FREQUENCY)) return "Frequency";
        if (this.equals(TEMPERATURE)) return "Temperature";
        if (this.equals(DATA)) return "Data";
        return toSymbolString();
    }

    public String toSymbolString() {
        if (isDimensionless()) return "1";
        StringBuilder sb = new StringBuilder();
        appendDim(sb, "M", mass);
        appendDim(sb, "L", length);
        appendDim(sb, "T", time);
        appendDim(sb, "I", current);
        appendDim(sb, "Θ", temperature);
        appendDim(sb, "N", amount);
        appendDim(sb, "J", luminousIntensity);
        appendDim(sb, "D", data);
        return sb.toString().trim();
    }

    private void appendDim(StringBuilder sb, String symbol, int exp) {
        if (exp == 0) return;
        if (sb.length() > 0) sb.append("·");
        sb.append(symbol);
        if (exp != 1) {
            sb.append("^").append(exp);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dimension)) return false;
        Dimension dimension = (Dimension) o;
        return mass == dimension.mass &&
                length == dimension.length &&
                time == dimension.time &&
                current == dimension.current &&
                temperature == dimension.temperature &&
                amount == dimension.amount &&
                luminousIntensity == dimension.luminousIntensity &&
                data == dimension.data;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mass, length, time, current, temperature, amount, luminousIntensity, data);
    }

    @Override
    public String toString() {
        return getDerivedName();
    }
}