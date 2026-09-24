package com.devendra.healthtracker.calculator;

import java.util.Objects;

/**
 * Formal Unit Representation.
 * Implements affine transformation for temperature (value * scale + offset)
 * and linear transformation for all standard dimensional units.
 */
public final class UnitDefinition {
    public final String symbol;
    public final String name;
    public final String category;
    public final Dimension dimension;
    public final double scaleToCanonical; // Multiply by this to get to base SI unit
    public final double offsetToCanonical;// Added after multiplication to get to base SI unit (e.g. Celsius to Kelvin)

    public UnitDefinition(String symbol, String name, String category, Dimension dimension, double scaleToCanonical) {
        this(symbol, name, category, dimension, scaleToCanonical, 0.0);
    }

    public UnitDefinition(String symbol, String name, String category, Dimension dimension, double scaleToCanonical, double offsetToCanonical) {
        this.symbol = symbol;
        this.name = name;
        this.category = category;
        this.dimension = dimension;
        this.scaleToCanonical = scaleToCanonical;
        this.offsetToCanonical = offsetToCanonical;
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }

    /**
     * Converts a value in this unit to the base canonical SI unit.
     */
    public double toCanonical(double value) {
        return (value * scaleToCanonical) + offsetToCanonical;
    }

    /**
     * Converts a canonical base SI value into this unit.
     */
    public double fromCanonical(double canonicalValue) {
        return (canonicalValue - offsetToCanonical) / scaleToCanonical;
    }

    /**
     * Converts a value directly from this unit to a target unit, checking dimensional compatibility.
     */
    public double convertTo(double value, UnitDefinition targetUnit) {
        if (!this.dimension.equals(targetUnit.dimension)) {
            throw new IllegalArgumentException("Dimension mismatch: Cannot convert from " +
                    this.name + " [" + this.dimension.getDerivedName() + "] to " +
                    targetUnit.name + " [" + targetUnit.dimension.getDerivedName() + "]");
        }
        double canonical = toCanonical(value);
        return targetUnit.fromCanonical(canonical);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnitDefinition)) return false;
        UnitDefinition that = (UnitDefinition) o;
        return Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

    @Override
    public String toString() {
        return symbol + " (" + name + ")";
    }
}