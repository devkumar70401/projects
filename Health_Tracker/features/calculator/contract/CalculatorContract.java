package com.devendra.healthtracker.calculator;

import java.math.BigDecimal;

/**
 * Public Contract interface for the Calculator Capability.
 * Mandated by MODULAR_ARCHITECTURE_CONSTITUTION.md (Rule 9).
 */
public interface CalculatorContract {

    /**
     * Evaluates a mathematical or scientific expression string.
     */
    String evaluate(String expression) throws Exception;

    /**
     * Converts a value from one unit to another within the same physical dimension.
     */
    BigDecimal convertUnit(BigDecimal value, String fromUnit, String toUnit) throws Exception;
}
