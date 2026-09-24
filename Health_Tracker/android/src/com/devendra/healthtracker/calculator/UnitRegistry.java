package com.devendra.healthtracker.calculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized Dimensional Unit Registry.
 * Holds exact, documented conversion factors referenced to the International System of Units (SI).
 */
public final class UnitRegistry {
    private static final Map<String, UnitDefinition> UNITS_BY_SYMBOL = new LinkedHashMap<>();
    private static final Map<String, List<UnitDefinition>> UNITS_BY_CATEGORY = new LinkedHashMap<>();

    static {
        // --- 1. LENGTH (Base SI: Meter) ---
        reg(new UnitDefinition("nm", "Nanometer", "Length", Dimension.LENGTH, 1e-9));
        reg(new UnitDefinition("μm", "Micrometer", "Length", Dimension.LENGTH, 1e-6));
        reg(new UnitDefinition("mm", "Millimeter", "Length", Dimension.LENGTH, 1e-3));
        reg(new UnitDefinition("cm", "Centimeter", "Length", Dimension.LENGTH, 1e-2));
        reg(new UnitDefinition("m", "Meter", "Length", Dimension.LENGTH, 1.0));
        reg(new UnitDefinition("km", "Kilometer", "Length", Dimension.LENGTH, 1e3));
        reg(new UnitDefinition("in", "Inch", "Length", Dimension.LENGTH, 0.0254));
        reg(new UnitDefinition("ft", "Foot", "Length", Dimension.LENGTH, 0.3048));
        reg(new UnitDefinition("yd", "Yard", "Length", Dimension.LENGTH, 0.9144));
        reg(new UnitDefinition("mi", "Mile", "Length", Dimension.LENGTH, 1609.344));
        reg(new UnitDefinition("nmi", "Nautical Mile", "Length", Dimension.LENGTH, 1852.0));

        // --- 2. AREA (Base SI: Square Meter) ---
        reg(new UnitDefinition("mm²", "Square Millimeter", "Area", Dimension.AREA, 1e-6));
        reg(new UnitDefinition("cm²", "Square Centimeter", "Area", Dimension.AREA, 1e-4));
        reg(new UnitDefinition("m²", "Square Meter", "Area", Dimension.AREA, 1.0));
        reg(new UnitDefinition("km²", "Square Kilometer", "Area", Dimension.AREA, 1e6));
        reg(new UnitDefinition("in²", "Square Inch", "Area", Dimension.AREA, 0.00064516));
        reg(new UnitDefinition("ft²", "Square Foot", "Area", Dimension.AREA, 0.09290304));
        reg(new UnitDefinition("acre", "Acre", "Area", Dimension.AREA, 4046.8564224));
        reg(new UnitDefinition("ha", "Hectare", "Area", Dimension.AREA, 10000.0));

        // --- 3. VOLUME (Base SI: Cubic Meter) ---
        reg(new UnitDefinition("mL", "Milliliter", "Volume", Dimension.VOLUME, 1e-6));
        reg(new UnitDefinition("L", "Liter", "Volume", Dimension.VOLUME, 1e-3));
        reg(new UnitDefinition("m³", "Cubic Meter", "Volume", Dimension.VOLUME, 1.0));
        reg(new UnitDefinition("cm³", "Cubic Centimeter", "Volume", Dimension.VOLUME, 1e-6));
        reg(new UnitDefinition("in³", "Cubic Inch", "Volume", Dimension.VOLUME, 0.000016387064));
        reg(new UnitDefinition("ft³", "Cubic Foot", "Volume", Dimension.VOLUME, 0.028316846592));
        reg(new UnitDefinition("gal", "US Gallon", "Volume", Dimension.VOLUME, 0.003785411784));
        reg(new UnitDefinition("qt", "US Quart", "Volume", Dimension.VOLUME, 0.000946352946));
        reg(new UnitDefinition("pt", "US Pint", "Volume", Dimension.VOLUME, 0.000473176473));

        // --- 4. MASS (Base SI: Kilogram) ---
        reg(new UnitDefinition("μg", "Microgram", "Mass", Dimension.MASS, 1e-9));
        reg(new UnitDefinition("mg", "Milligram", "Mass", Dimension.MASS, 1e-6));
        reg(new UnitDefinition("g", "Gram", "Mass", Dimension.MASS, 1e-3));
        reg(new UnitDefinition("kg", "Kilogram", "Mass", Dimension.MASS, 1.0));
        reg(new UnitDefinition("t", "Metric Ton", "Mass", Dimension.MASS, 1000.0));
        reg(new UnitDefinition("oz", "Ounce", "Mass", Dimension.MASS, 0.028349523125));
        reg(new UnitDefinition("lb", "Pound", "Mass", Dimension.MASS, 0.45359237));
        reg(new UnitDefinition("stone", "Stone", "Mass", Dimension.MASS, 6.35029318));

        // --- 5. TEMPERATURE (Base SI: Kelvin) ---
        // K is base canonical
        reg(new UnitDefinition("K", "Kelvin", "Temperature", Dimension.TEMPERATURE, 1.0, 0.0));
        // °C to K: K = °C * 1.0 + 273.15
        reg(new UnitDefinition("°C", "Celsius", "Temperature", Dimension.TEMPERATURE, 1.0, 273.15));
        // °F to K: K = (°F - 32) * 5/9 + 273.15 = °F * (5/9) + (273.15 - 160/9) = °F * (5/9) + 255.37222222222223
        reg(new UnitDefinition("°F", "Fahrenheit", "Temperature", Dimension.TEMPERATURE, 5.0 / 9.0, 255.37222222222223));

        // --- 6. TIME (Base SI: Second) ---
        reg(new UnitDefinition("ns", "Nanosecond", "Time", Dimension.TIME, 1e-9));
        reg(new UnitDefinition("μs", "Microsecond", "Time", Dimension.TIME, 1e-6));
        reg(new UnitDefinition("ms", "Millisecond", "Time", Dimension.TIME, 1e-3));
        reg(new UnitDefinition("s", "Second", "Time", Dimension.TIME, 1.0));
        reg(new UnitDefinition("min", "Minute", "Time", Dimension.TIME, 60.0));
        reg(new UnitDefinition("h", "Hour", "Time", Dimension.TIME, 3600.0));
        reg(new UnitDefinition("day", "Day", "Time", Dimension.TIME, 86400.0));
        reg(new UnitDefinition("week", "Week", "Time", Dimension.TIME, 604800.0));
        reg(new UnitDefinition("year", "Julian Year (365.25 d)", "Time", Dimension.TIME, 31557600.0));

        // --- 7. SPEED (Base SI: m/s) ---
        reg(new UnitDefinition("m/s", "Meter per Second", "Speed", Dimension.VELOCITY, 1.0));
        reg(new UnitDefinition("km/h", "Kilometer per Hour", "Speed", Dimension.VELOCITY, 1.0 / 3.6));
        reg(new UnitDefinition("mph", "Mile per Hour", "Speed", Dimension.VELOCITY, 0.44704));
        reg(new UnitDefinition("knot", "Knot", "Speed", Dimension.VELOCITY, 1852.0 / 3600.0));
        reg(new UnitDefinition("ft/s", "Foot per Second", "Speed", Dimension.VELOCITY, 0.3048));

        // --- 8. PRESSURE (Base SI: Pascal) ---
        reg(new UnitDefinition("Pa", "Pascal", "Pressure", Dimension.PRESSURE, 1.0));
        reg(new UnitDefinition("kPa", "Kilopascal", "Pressure", Dimension.PRESSURE, 1e3));
        reg(new UnitDefinition("MPa", "Megapascal", "Pressure", Dimension.PRESSURE, 1e6));
        reg(new UnitDefinition("bar", "Bar", "Pressure", Dimension.PRESSURE, 1e5));
        reg(new UnitDefinition("atm", "Standard Atmosphere", "Pressure", Dimension.PRESSURE, 101325.0));
        reg(new UnitDefinition("psi", "Pound per Square Inch", "Pressure", Dimension.PRESSURE, 6894.757293168));
        reg(new UnitDefinition("torr", "Torr", "Pressure", Dimension.PRESSURE, 101325.0 / 760.0));
        reg(new UnitDefinition("mmHg", "Millimeter of Mercury", "Pressure", Dimension.PRESSURE, 133.322387415));

        // --- 9. ENERGY (Base SI: Joule) ---
        reg(new UnitDefinition("J", "Joule", "Energy", Dimension.ENERGY, 1.0));
        reg(new UnitDefinition("kJ", "Kilojoule", "Energy", Dimension.ENERGY, 1e3));
        reg(new UnitDefinition("cal", "Thermochemical Calorie", "Energy", Dimension.ENERGY, 4.184));
        reg(new UnitDefinition("kcal", "Kilocalorie", "Energy", Dimension.ENERGY, 4184.0));
        reg(new UnitDefinition("Wh", "Watt-hour", "Energy", Dimension.ENERGY, 3600.0));
        reg(new UnitDefinition("kWh", "Kilowatt-hour", "Energy", Dimension.ENERGY, 3.6e6));
        reg(new UnitDefinition("eV", "Electronvolt", "Energy", Dimension.ENERGY, 1.602176634e-19));

        // --- 10. POWER (Base SI: Watt) ---
        reg(new UnitDefinition("W", "Watt", "Power", Dimension.POWER, 1.0));
        reg(new UnitDefinition("kW", "Kilowatt", "Power", Dimension.POWER, 1e3));
        reg(new UnitDefinition("MW", "Megawatt", "Power", Dimension.POWER, 1e6));
        reg(new UnitDefinition("hp", "Mechanical Horsepower", "Power", Dimension.POWER, 745.69987158227022));

        // --- 11. FORCE (Base SI: Newton) ---
        reg(new UnitDefinition("N", "Newton", "Force", Dimension.FORCE, 1.0));
        reg(new UnitDefinition("kN", "Kilonewton", "Force", Dimension.FORCE, 1e3));
        reg(new UnitDefinition("dyn", "Dyne", "Force", Dimension.FORCE, 1e-5));
        reg(new UnitDefinition("lbf", "Pound-force", "Force", Dimension.FORCE, 4.4482216152605));

        // --- 12. FREQUENCY (Base SI: Hertz) ---
        reg(new UnitDefinition("Hz", "Hertz", "Frequency", Dimension.FREQUENCY, 1.0));
        reg(new UnitDefinition("kHz", "Kilohertz", "Frequency", Dimension.FREQUENCY, 1e3));
        reg(new UnitDefinition("MHz", "Megahertz", "Frequency", Dimension.FREQUENCY, 1e6));
        reg(new UnitDefinition("GHz", "Gigahertz", "Frequency", Dimension.FREQUENCY, 1e9));

        // --- 13. DATA STORAGE (Base: byte, decimal vs binary strictly distinguished) ---
        reg(new UnitDefinition("bit", "Bit", "Data", Dimension.DATA, 1.0 / 8.0));
        reg(new UnitDefinition("B", "Byte", "Data", Dimension.DATA, 1.0));
        // Decimal prefixes (10^3)
        reg(new UnitDefinition("KB", "Kilobyte (10³ B)", "Data", Dimension.DATA, 1e3));
        reg(new UnitDefinition("MB", "Megabyte (10⁶ B)", "Data", Dimension.DATA, 1e6));
        reg(new UnitDefinition("GB", "Gigabyte (10⁹ B)", "Data", Dimension.DATA, 1e9));
        reg(new UnitDefinition("TB", "Terabyte (10¹² B)", "Data", Dimension.DATA, 1e12));
        // Binary prefixes (2^10)
        reg(new UnitDefinition("KiB", "Kibibyte (1024 B)", "Data", Dimension.DATA, 1024.0));
        reg(new UnitDefinition("MiB", "Mebibyte (1024² B)", "Data", Dimension.DATA, 1048576.0));
        reg(new UnitDefinition("GiB", "Gibibyte (1024³ B)", "Data", Dimension.DATA, 1073741824.0));
        reg(new UnitDefinition("TiB", "Tebibyte (1024⁴ B)", "Data", Dimension.DATA, 1099511627776.0));
    }

    private static final UnitRegistry INSTANCE = new UnitRegistry();

    public static UnitRegistry getInstance() {
        return INSTANCE;
    }

    public UnitDefinition findUnit(String symbol) {
        return getBySymbol(symbol);
    }

    public double convert(double value, UnitDefinition from, UnitDefinition to) {
        if (from == null) throw new IllegalArgumentException("From unit cannot be null");
        if (to == null) throw new IllegalArgumentException("To unit cannot be null");
        return from.convertTo(value, to);
    }

    private static void reg(UnitDefinition unit) {
        UNITS_BY_SYMBOL.put(unit.symbol.toLowerCase(), unit);
        UNITS_BY_CATEGORY.computeIfAbsent(unit.category, k -> new ArrayList<>()).add(unit);
    }

    public static UnitDefinition getBySymbol(String symbol) {
        if (symbol == null) return null;
        return UNITS_BY_SYMBOL.get(symbol.trim().toLowerCase());
    }

    public static List<String> getCategories() {
        return new ArrayList<>(UNITS_BY_CATEGORY.keySet());
    }

    public static List<UnitDefinition> getUnitsForCategory(String category) {
        List<UnitDefinition> list = UNITS_BY_CATEGORY.get(category);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public static double convert(double value, String fromSymbol, String toSymbol) {
        UnitDefinition from = getBySymbol(fromSymbol);
        UnitDefinition to = getBySymbol(toSymbol);
        if (from == null) throw new IllegalArgumentException("Unknown source unit: " + fromSymbol);
        if (to == null) throw new IllegalArgumentException("Unknown target unit: " + toSymbol);
        return from.convertTo(value, to);
    }
}