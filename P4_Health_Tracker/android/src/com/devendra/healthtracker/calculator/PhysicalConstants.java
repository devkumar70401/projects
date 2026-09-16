package com.devendra.healthtracker.calculator;

import com.devendra.healthtracker.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized Scientific and Physical Constants Registry (CODATA 2022).
 * Stores name, symbol, exact or CODATA recommended value, SI unit, and documentation source.
 */
public final class PhysicalConstants {

    public static final class ConstantItem {
        public final String symbol;
        public final String name;
        public final double value;
        public final String unit;
        public final String uncertainty;
        public final String source;

        public ConstantItem(String symbol, String name, double value, String unit, String uncertainty, String source) {
            this.symbol = symbol;
            this.name = name;
            this.value = value;
            this.unit = unit;
            this.uncertainty = uncertainty;
            this.source = source;
        }

        @Override
        public String toString() {
            return symbol + " = " + value + " " + unit + " (" + name + ")";
        }
    }

    private static final Map<String, ConstantItem> CONSTANTS = new LinkedHashMap<>();

    static {
        // Universal & Mathematical Constants
        add(new ConstantItem("c", "Speed of light in vacuum", 299792458.0, "m/s", "Exact", "CODATA 2022 / SI"));
        add(new ConstantItem("h", "Planck constant", 6.62607015e-34, "J·s", "Exact", "CODATA 2022 / SI"));
        add(new ConstantItem("hbar", "Reduced Planck constant (h / 2π)", 1.0545718176461565e-34, "J·s", "Exact derived", "CODATA 2022"));
        add(new ConstantItem("G", "Newtonian constant of gravitation", 6.67430e-11, "m³/(kg·s²)", "±0.00015e-11", "CODATA 2022"));
        add(new ConstantItem("e_charge", "Elementary charge", 1.602176634e-19, "C", "Exact", "CODATA 2022 / SI"));
        add(new ConstantItem("k_B", "Boltzmann constant", 1.380649e-23, "J/K", "Exact", "CODATA 2022 / SI"));
        add(new ConstantItem("N_A", "Avogadro constant", 6.02214076e23, "mol⁻¹", "Exact", "CODATA 2022 / SI"));
        add(new ConstantItem("R", "Molar gas constant (N_A · k_B)", 8.31446261815324, "J/(mol·K)", "Exact derived", "CODATA 2022"));
        add(new ConstantItem("sigma", "Stefan-Boltzmann constant", 5.670374419e-8, "W/(m²·K⁴)", "Exact derived", "CODATA 2022"));
        add(new ConstantItem("g_0", "Standard acceleration of gravity", 9.80665, "m/s²", "Exact", "ISO 80000-3"));
        add(new ConstantItem("m_e", "Electron rest mass", 9.1093837015e-31, "kg", "±0.0000000028e-31", "CODATA 2022"));
        add(new ConstantItem("m_p", "Proton rest mass", 1.67262192369e-27, "kg", "±0.00000000051e-27", "CODATA 2022"));
        add(new ConstantItem("m_n", "Neutron rest mass", 1.67492749804e-27, "kg", "±0.00000000095e-27", "CODATA 2022"));
        add(new ConstantItem("mu_0", "Vacuum magnetic permeability", 1.25663706212e-6, "N/A²", "±0.00000000019e-6", "CODATA 2022"));
        add(new ConstantItem("eps_0", "Vacuum electric permittivity (1 / (μ₀ c²))", 8.8541878128e-12, "F/m", "±0.0000000013e-12", "CODATA 2022"));
        add(new ConstantItem("alpha", "Fine-structure constant", 7.2973525693e-3, "1", "±0.0000000011e-3", "CODATA 2022"));
        add(new ConstantItem("phi", "Golden ratio (φ)", 1.618033988749895, "1", "Mathematical Exact", "Algebraic"));
        add(new ConstantItem("tau", "Circle constant (τ = 2π)", 6.283185307179586, "1", "Mathematical Exact", "Geometric"));
        add(new ConstantItem("gamma", "Euler-Mascheroni constant (γ)", 0.5772156649015329, "1", "Mathematical Exact", "Analysis"));
    }

    private static void add(ConstantItem item) {
        CONSTANTS.put(item.symbol.toLowerCase(), item);
    }

    public static ConstantItem get(String symbol) {
        if (symbol == null) return null;
        return CONSTANTS.get(symbol.trim().toLowerCase());
    }

    public static List<ConstantItem> getAllConstants() {
        return Collections.unmodifiableList(new ArrayList<>(CONSTANTS.values()));
    }
}