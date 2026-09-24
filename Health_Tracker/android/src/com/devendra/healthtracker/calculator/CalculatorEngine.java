package com.devendra.healthtracker.calculator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Unified high-level coordinator and facade for the Calculator subsystem.
 * Bridges ExpressionParser, EvaluationContext, UnitRegistry, MatrixEngine,
 * ProgrammingEngine, and CalculatorHistoryManager.
 */
public class CalculatorEngine {

    private final EvaluationContext evalContext;
    private final MathContextConfig contextConfig;
    private final UnitRegistry unitRegistry;
    private final CalculatorHistoryManager historyManager;

    private BigDecimal memoryRegister = BigDecimal.ZERO;
    private boolean memoryHasValue = false;

    public CalculatorEngine(CalculatorHistoryManager historyManager) {
        this.contextConfig = new MathContextConfig();
        this.evalContext = new EvaluationContext(AngleMode.DEG, this.contextConfig);
        this.unitRegistry = UnitRegistry.getInstance();
        this.historyManager = historyManager;
    }

    public CalculatorEngine() {
        this(null);
    }

    public EvaluationContext getEvaluationContext() {
        return evalContext;
    }

    public MathContextConfig getContextConfig() {
        return contextConfig;
    }

    public UnitRegistry getUnitRegistry() {
        return unitRegistry;
    }

    public AngleMode getAngleMode() {
        return evalContext.getAngleMode();
    }

    public void setAngleMode(AngleMode mode) {
        evalContext.setAngleMode(mode);
    }

    // --- High-level Expression Evaluation ---

    public String evaluateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return "0";
        }
        try {
            ASTNodes.ExpressionNode ast = ExpressionParser.parse(expression);
            ComplexNumber result = ast.evaluate(evalContext);
            evalContext.setLastResult(result.real);

            String formattedResult = contextConfig.format(result);
            if (historyManager != null) {
                historyManager.addEntry(expression, formattedResult, "SCIENTIFIC");
            }
            return formattedResult;
        } catch (Exception e) {
            return "Error: " + (e.getMessage() != null ? e.getMessage() : "Calculation error");
        }
    }

    // --- Memory Operations ---

    public void memoryClear() {
        memoryRegister = BigDecimal.ZERO;
        memoryHasValue = false;
    }

    public BigDecimal memoryRecall() {
        return memoryRegister;
    }

    public void memoryStore(BigDecimal value) {
        if (value != null) {
            memoryRegister = value;
            memoryHasValue = true;
        }
    }

    public void memoryStore(double value) {
        memoryStore(BigDecimal.valueOf(value));
    }

    public void memoryAdd(BigDecimal value) {
        if (value != null) {
            memoryRegister = memoryRegister.add(value, contextConfig.getMathContext());
            memoryHasValue = true;
        }
    }

    public void memoryAdd(double value) {
        memoryAdd(BigDecimal.valueOf(value));
    }

    public void memorySubtract(BigDecimal value) {
        if (value != null) {
            memoryRegister = memoryRegister.subtract(value, contextConfig.getMathContext());
            memoryHasValue = true;
        }
    }

    public void memorySubtract(double value) {
        memorySubtract(BigDecimal.valueOf(value));
    }

    public boolean hasMemoryValue() {
        return memoryHasValue;
    }

    // --- Programming Mode Operations ---

    public ProgrammingEngine.EvaluationResult evaluateProgrammerOp(
            long operandA, long operandB, String operator,
            ProgrammingEngine.BitWidth width, boolean isSigned) {
        switch (operator.toUpperCase()) {
            case "+":
                return ProgrammingEngine.add(operandA, operandB, width, isSigned);
            case "-":
                return ProgrammingEngine.subtract(operandA, operandB, width, isSigned);
            case "*":
                return ProgrammingEngine.multiply(operandA, operandB, width, isSigned);
            case "/":
                return ProgrammingEngine.divide(operandA, operandB, width, isSigned);
            case "AND": {
                long res = ProgrammingEngine.bitwiseAnd(operandA, operandB, width);
                return new ProgrammingEngine.EvaluationResult(res, width, isSigned, false, null);
            }
            case "OR": {
                long res = ProgrammingEngine.bitwiseOr(operandA, operandB, width);
                return new ProgrammingEngine.EvaluationResult(res, width, isSigned, false, null);
            }
            case "XOR": {
                long res = ProgrammingEngine.bitwiseXor(operandA, operandB, width);
                return new ProgrammingEngine.EvaluationResult(res, width, isSigned, false, null);
            }
            case "NOT": {
                long res = ProgrammingEngine.bitwiseNot(operandA, width);
                return new ProgrammingEngine.EvaluationResult(res, width, isSigned, false, null);
            }
            case "SHL": {
                long res = ProgrammingEngine.shiftLeft(operandA, (int) operandB, width);
                return new ProgrammingEngine.EvaluationResult(res, width, isSigned, false, null);
            }
            case "SHR": {
                long res = ProgrammingEngine.shiftRightLogical(operandA, (int) operandB, width);
                return new ProgrammingEngine.EvaluationResult(res, width, isSigned, false, null);
            }
            case "SAR": {
                long res = ProgrammingEngine.shiftRightArithmetic(operandA, (int) operandB, width);
                return new ProgrammingEngine.EvaluationResult(res, width, isSigned, false, null);
            }
            case "ROL": {
                long res = ProgrammingEngine.rotateLeft(operandA, (int) operandB, width);
                return new ProgrammingEngine.EvaluationResult(res, width, isSigned, false, null);
            }
            case "ROR": {
                long res = ProgrammingEngine.rotateRight(operandA, (int) operandB, width);
                return new ProgrammingEngine.EvaluationResult(res, width, isSigned, false, null);
            }
            case "%":
            case "MOD": {
                long bMasked = isSigned ? ProgrammingEngine.clampToSigned(operandB, width) : (operandB & width.getMask());
                if (bMasked == 0) throw new ArithmeticException("Modulo by zero");
                long raw = isSigned ? (ProgrammingEngine.clampToSigned(operandA, width) % bMasked)
                                    : Long.remainderUnsigned(operandA & width.getMask(), bMasked);
                return new ProgrammingEngine.EvaluationResult(raw & width.getMask(), width, isSigned, false, null);
            }
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    // --- Unit & Dimensional Operations ---

    public String convertUnit(double value, String fromUnitSymbol, String toUnitSymbol) {
        UnitDefinition fromUnit = unitRegistry.findUnit(fromUnitSymbol);
        UnitDefinition toUnit = unitRegistry.findUnit(toUnitSymbol);

        if (fromUnit == null) throw new IllegalArgumentException("Unknown source unit: " + fromUnitSymbol);
        if (toUnit == null) throw new IllegalArgumentException("Unknown target unit: " + toUnitSymbol);

        double converted = unitRegistry.convert(value, fromUnit, toUnit);
        String resStr = contextConfig.format(converted) + " " + toUnit.getSymbol();
        if (historyManager != null) {
            historyManager.addEntry(value + " " + fromUnitSymbol + " -> " + toUnitSymbol, resStr, "UNIT");
        }
        return resStr;
    }

    public DimensionalQuantity evaluateDimensionalOperation(
            double val1, String unit1Symbol, String op, double val2, String unit2Symbol) {
        UnitDefinition u1 = unitRegistry.findUnit(unit1Symbol);
        UnitDefinition u2 = unitRegistry.findUnit(unit2Symbol);

        if (u1 == null) throw new IllegalArgumentException("Unknown unit: " + unit1Symbol);
        if (u2 == null) throw new IllegalArgumentException("Unknown unit: " + unit2Symbol);

        DimensionalQuantity q1 = new DimensionalQuantity(val1, u1);
        DimensionalQuantity q2 = new DimensionalQuantity(val2, u2);

        DimensionalQuantity result;
        switch (op.trim()) {
            case "+": result = q1.add(q2); break;
            case "-": result = q1.subtract(q2); break;
            case "*": result = q1.multiply(q2); break;
            case "/": result = q1.divide(q2); break;
            default: throw new IllegalArgumentException("Unsupported dimensional operator: " + op);
        }

        if (historyManager != null) {
            historyManager.addEntry(q1 + " " + op + " " + q2, result.toString(), "DIMENSION");
        }
        return result;
    }

    // --- Number Theory Helpers ---

    public BigInteger factorial(int n) {
        return NumberTheory.factorial(n);
    }

    public BigInteger nPr(int n, int r) {
        return NumberTheory.nPr(n, r);
    }

    public BigInteger nCr(int n, int r) {
        return NumberTheory.nCr(n, r);
    }

    public BigInteger gcd(BigInteger a, BigInteger b) {
        return NumberTheory.gcd(a, b);
    }

    public BigInteger lcm(BigInteger a, BigInteger b) {
        return NumberTheory.lcm(a, b);
    }

    public boolean isPrime(long n) {
        return NumberTheory.isPrime(n);
    }

    public List<BigInteger> primeFactors(BigInteger n) {
        return NumberTheory.primeFactors(n);
    }
}
