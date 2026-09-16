package com.devendra.healthtracker.calculator;

import java.util.Objects;

/**
 * Dimension-Aware Quantity Subsystem (Value + Unit + Dimension).
 * Enforces dimensional compatibility on operations and automates derived unit synthesis.
 */
public final class DimensionalQuantity {
    public final double value;
    public final UnitDefinition unit;
    public final Dimension dimension;

    public DimensionalQuantity(double value, UnitDefinition unit) {
        if (unit == null) throw new IllegalArgumentException("Unit cannot be null");
        this.value = value;
        this.unit = unit;
        this.dimension = unit.dimension;
    }

    public DimensionalQuantity(double value, Dimension dimension) {
        if (dimension == null) throw new IllegalArgumentException("Dimension cannot be null");
        this.value = value;
        this.dimension = dimension;
        this.unit = findCanonicalUnitForDimension(dimension);
    }

    public double getValue() { return value; }
    public UnitDefinition getUnit() { return unit; }
    public Dimension getDimension() { return dimension; }

    public static DimensionalQuantity of(double value, String unitSymbol) {
        UnitDefinition u = UnitRegistry.getBySymbol(unitSymbol);
        if (u == null) throw new IllegalArgumentException("Unknown unit symbol: " + unitSymbol);
        return new DimensionalQuantity(value, u);
    }

    public static DimensionalQuantity dimensionless(double value) {
        return new DimensionalQuantity(value, Dimension.DIMENSIONLESS);
    }

    public DimensionalQuantity add(DimensionalQuantity other) {
        if (!this.dimension.equals(other.dimension)) {
            throw new IllegalArgumentException("Dimension Mismatch: Cannot add " +
                    this.dimension.getDerivedName() + " (" + this.unit.symbol + ") and " +
                    other.dimension.getDerivedName() + " (" + other.unit.symbol + ")");
        }
        double otherConverted = other.unit.convertTo(other.value, this.unit);
        return new DimensionalQuantity(this.value + otherConverted, this.unit);
    }

    public DimensionalQuantity subtract(DimensionalQuantity other) {
        if (!this.dimension.equals(other.dimension)) {
            throw new IllegalArgumentException("Dimension Mismatch: Cannot subtract " +
                    other.dimension.getDerivedName() + " (" + other.unit.symbol + ") from " +
                    this.dimension.getDerivedName() + " (" + this.unit.symbol + ")");
        }
        double otherConverted = other.unit.convertTo(other.value, this.unit);
        return new DimensionalQuantity(this.value - otherConverted, this.unit);
    }

    public DimensionalQuantity multiply(DimensionalQuantity other) {
        double canonicalVal = this.unit.toCanonical(this.value) * other.unit.toCanonical(other.value);
        Dimension newDim = this.dimension.multiply(other.dimension);
        UnitDefinition newUnit = findCanonicalUnitForDimension(newDim);
        double finalVal = newUnit != null ? newUnit.fromCanonical(canonicalVal) : canonicalVal;
        return new DimensionalQuantity(finalVal, newDim);
    }

    public DimensionalQuantity multiply(double scalar) {
        return new DimensionalQuantity(this.value * scalar, this.unit);
    }

    public DimensionalQuantity divide(DimensionalQuantity other) {
        if (other.value == 0.0) throw new ArithmeticException("Division by zero in dimensional calculation");
        double canonicalVal = this.unit.toCanonical(this.value) / other.unit.toCanonical(other.value);
        Dimension newDim = this.dimension.divide(other.dimension);
        UnitDefinition newUnit = findCanonicalUnitForDimension(newDim);
        double finalVal = newUnit != null ? newUnit.fromCanonical(canonicalVal) : canonicalVal;
        return new DimensionalQuantity(finalVal, newDim);
    }

    public DimensionalQuantity divide(double scalar) {
        if (scalar == 0.0) throw new ArithmeticException("Division by zero");
        return new DimensionalQuantity(this.value / scalar, this.unit);
    }

    public DimensionalQuantity convertTo(UnitDefinition targetUnit) {
        double converted = this.unit.convertTo(this.value, targetUnit);
        return new DimensionalQuantity(converted, targetUnit);
    }

    public DimensionalQuantity convertTo(String targetSymbol) {
        UnitDefinition target = UnitRegistry.getBySymbol(targetSymbol);
        if (target == null) throw new IllegalArgumentException("Unknown target unit: " + targetSymbol);
        return convertTo(target);
    }

    private static UnitDefinition findCanonicalUnitForDimension(Dimension dim) {
        if (dim.equals(Dimension.LENGTH)) return UnitRegistry.getBySymbol("m");
        if (dim.equals(Dimension.AREA)) return UnitRegistry.getBySymbol("m²");
        if (dim.equals(Dimension.VOLUME)) return UnitRegistry.getBySymbol("m³");
        if (dim.equals(Dimension.MASS)) return UnitRegistry.getBySymbol("kg");
        if (dim.equals(Dimension.TIME)) return UnitRegistry.getBySymbol("s");
        if (dim.equals(Dimension.VELOCITY)) return UnitRegistry.getBySymbol("m/s");
        if (dim.equals(Dimension.FORCE)) return UnitRegistry.getBySymbol("N");
        if (dim.equals(Dimension.PRESSURE)) return UnitRegistry.getBySymbol("Pa");
        if (dim.equals(Dimension.ENERGY)) return UnitRegistry.getBySymbol("J");
        if (dim.equals(Dimension.POWER)) return UnitRegistry.getBySymbol("W");
        if (dim.equals(Dimension.FREQUENCY)) return UnitRegistry.getBySymbol("Hz");
        if (dim.equals(Dimension.TEMPERATURE)) return UnitRegistry.getBySymbol("K");
        if (dim.equals(Dimension.DATA)) return UnitRegistry.getBySymbol("B");
        return new UnitDefinition(dim.toSymbolString(), dim.getDerivedName(), "Derived", dim, 1.0);
    }

    public String format(int decimals) {
        String fmt = "%." + decimals + "f";
        String valStr = String.format(java.util.Locale.US, fmt, value);
        if (valStr.contains(".")) {
            while (valStr.endsWith("0")) valStr = valStr.substring(0, valStr.length() - 1);
            if (valStr.endsWith(".")) valStr = valStr.substring(0, valStr.length() - 1);
        }
        return valStr + " " + unit.symbol;
    }

    @Override
    public String toString() {
        return format(4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DimensionalQuantity)) return false;
        DimensionalQuantity that = (DimensionalQuantity) o;
        if (!dimension.equals(that.dimension)) return false;
        double thisCanonical = unit.toCanonical(value);
        double thatCanonical = that.unit.toCanonical(that.value);
        return Math.abs(thisCanonical - thatCanonical) < 1e-12;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Math.round(unit.toCanonical(value) * 1e8), dimension);
    }
}