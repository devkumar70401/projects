package com.devendra.healthtracker.calculator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sandboxed Execution Environment for Expression Evaluation.
 * Stores variables, user-defined functions, constants, and angle mode.
 * Completely isolated from system resources, network, reflection, and file I/O.
 */
public final class EvaluationContext {
    private final Map<String, ComplexNumber> variables = new HashMap<>();
    private final Map<String, UserFunction> userFunctions = new HashMap<>();
    private AngleMode angleMode = AngleMode.RAD;

    public interface UserFunction {
        ComplexNumber apply(List<ComplexNumber> args);
    }

    public EvaluationContext() {
        resetDefaults();
    }

    public EvaluationContext(AngleMode angleMode, MathContextConfig config) {
        resetDefaults();
        if (angleMode != null) this.angleMode = angleMode;
    }

    public void resetDefaults() {
        variables.clear();
        userFunctions.clear();

        // Standard Mathematical Constants
        variables.put("pi", ComplexNumber.ofReal(Math.PI));
        variables.put("π", ComplexNumber.ofReal(Math.PI));
        variables.put("e", ComplexNumber.ofReal(Math.E));
        variables.put("tau", ComplexNumber.ofReal(2.0 * Math.PI));
        variables.put("τ", ComplexNumber.ofReal(2.0 * Math.PI));
        variables.put("phi", ComplexNumber.ofReal(1.618033988749895));
        variables.put("φ", ComplexNumber.ofReal(1.618033988749895));
        variables.put("gamma", ComplexNumber.ofReal(0.5772156649015329));
        variables.put("γ", ComplexNumber.ofReal(0.5772156649015329));
        variables.put("i", ComplexNumber.I);
        variables.put("ans", ComplexNumber.ZERO);
    }

    public AngleMode getAngleMode() {
        return angleMode;
    }

    public void setAngleMode(AngleMode mode) {
        if (mode == null) throw new IllegalArgumentException("AngleMode cannot be null");
        this.angleMode = mode;
    }

    public void setVariable(String name, ComplexNumber value) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Variable name cannot be empty");
        }
        variables.put(name.trim().toLowerCase(), value);
    }

    public void setVariable(String name, double value) {
        setVariable(name, ComplexNumber.ofReal(value));
    }

    public ComplexNumber resolveVariable(String name) {
        String key = name.trim().toLowerCase();
        ComplexNumber val = variables.get(key);
        if (val != null) return val;

        // Check physical constants registry
        PhysicalConstants.ConstantItem c = PhysicalConstants.get(key);
        if (c != null) {
            return ComplexNumber.ofReal(c.value);
        }

        throw new IllegalArgumentException("Undefined variable or constant: '" + name + "'");
    }

    public void setLastResult(double res) {
        setVariable("ans", res);
    }

    public void setUserFunction(String name, String paramName, String expression) {
        defineUserFunction(name, paramName, ExpressionParser.parse(expression));
    }

    public void defineUserFunction(String name, String paramName, ASTNodes.ExpressionNode bodyNode) {
        String key = name.trim().toLowerCase();
        userFunctions.put(key, args -> {
            if (args.size() != 1) {
                throw new IllegalArgumentException("Function '" + key + "' expects 1 argument, but got " + args.size());
            }
            EvaluationContext subContext = new EvaluationContext();
            subContext.variables.putAll(this.variables);
            subContext.angleMode = this.angleMode;
            subContext.setVariable(paramName, args.get(0));
            return bodyNode.evaluate(subContext);
        });
    }

    public ComplexNumber callFunction(String name, List<ASTNodes.ExpressionNode> argNodes) {
        String fn = name.trim().toLowerCase();

        // Check user functions
        UserFunction uf = userFunctions.get(fn);
        if (uf != null) {
            List<ComplexNumber> evaluated = new java.util.ArrayList<>();
            for (ASTNodes.ExpressionNode node : argNodes) {
                evaluated.add(node.evaluate(this));
            }
            return uf.apply(evaluated);
        }

        // Standard built-in scientific functions
        int argCount = argNodes.size();

        // 1-arg functions
        if (argCount == 1) {
            ComplexNumber z = argNodes.get(0).evaluate(this);

            switch (fn) {
                // Trigonometry
                case "sin":
                    if (z.isReal()) return ComplexNumber.ofReal(angleMode.sin(z.real));
                    return z.sin();
                case "cos":
                    if (z.isReal()) return ComplexNumber.ofReal(angleMode.cos(z.real));
                    return z.cos();
                case "tan":
                    if (z.isReal()) return ComplexNumber.ofReal(angleMode.tan(z.real));
                    return z.tan();
                case "cot":
                    double t = angleMode.tan(z.real);
                    if (Math.abs(t) < 1e-15) throw new ArithmeticException("Domain error: cot is undefined");
                    return ComplexNumber.ofReal(1.0 / t);
                case "sec":
                    double c = angleMode.cos(z.real);
                    if (Math.abs(c) < 1e-15) throw new ArithmeticException("Domain error: sec is undefined");
                    return ComplexNumber.ofReal(1.0 / c);
                case "csc":
                    double s = angleMode.sin(z.real);
                    if (Math.abs(s) < 1e-15) throw new ArithmeticException("Domain error: csc is undefined");
                    return ComplexNumber.ofReal(1.0 / s);

                // Inverse Trigonometry
                case "asin":
                    if (z.isReal() && Math.abs(z.real) <= 1.0) {
                        return ComplexNumber.ofReal(angleMode.fromRadians(Math.asin(z.real)));
                    }
                    // Complex asin: -i * ln(i*z + sqrt(1 - z^2))
                    ComplexNumber oneMinusZ2 = ComplexNumber.ONE.subtract(z.multiply(z)).sqrt();
                    return ComplexNumber.I.negate().multiply(ComplexNumber.I.multiply(z).add(oneMinusZ2).ln());
                case "acos":
                    if (z.isReal() && Math.abs(z.real) <= 1.0) {
                        return ComplexNumber.ofReal(angleMode.fromRadians(Math.acos(z.real)));
                    }
                    // Complex acos: -i * ln(z + i*sqrt(1 - z^2))
                    ComplexNumber root = ComplexNumber.ONE.subtract(z.multiply(z)).sqrt();
                    return ComplexNumber.I.negate().multiply(z.add(ComplexNumber.I.multiply(root)).ln());
                case "atan":
                    if (z.isReal()) {
                        return ComplexNumber.ofReal(angleMode.fromRadians(Math.atan(z.real)));
                    }
                    // Complex atan: (i / 2) * ln((1 - i*z) / (1 + i*z))
                    ComplexNumber iz = ComplexNumber.I.multiply(z);
                    return ComplexNumber.I.multiply(0.5).multiply(ComplexNumber.ONE.subtract(iz).divide(ComplexNumber.ONE.add(iz)).ln());
                case "acot":
                    if (Math.abs(z.real) < 1e-15 && z.isReal()) {
                        return ComplexNumber.ofReal(angleMode.fromRadians(Math.PI / 2.0));
                    }
                    return callFunction("atan", java.util.Collections.singletonList(new ASTNodes.NumberNode(1.0 / z.real)));
                case "asec":
                    if (Math.abs(z.real) < 1.0 && z.isReal()) {
                        throw new ArithmeticException("Domain error: asec requires |x| >= 1");
                    }
                    return callFunction("acos", java.util.Collections.singletonList(new ASTNodes.NumberNode(1.0 / z.real)));
                case "acsc":
                    if (Math.abs(z.real) < 1.0 && z.isReal()) {
                        throw new ArithmeticException("Domain error: acsc requires |x| >= 1");
                    }
                    return callFunction("asin", java.util.Collections.singletonList(new ASTNodes.NumberNode(1.0 / z.real)));

                // Hyperbolic
                case "sinh":
                    return z.sinh();
                case "cosh":
                    return z.cosh();
                case "tanh":
                    return z.tanh();
                case "coth":
                    return ComplexNumber.ONE.divide(z.tanh());
                case "sech":
                    return ComplexNumber.ONE.divide(z.cosh());
                case "csch":
                    return ComplexNumber.ONE.divide(z.sinh());

                // Inverse Hyperbolic
                case "asinh":
                    if (z.isReal()) {
                        double x = z.real;
                        return ComplexNumber.ofReal(Math.log(x + Math.sqrt(x * x + 1.0)));
                    }
                    return z.add(z.multiply(z).add(ComplexNumber.ONE).sqrt()).ln();
                case "acosh":
                    if (z.isReal() && z.real >= 1.0) {
                        double x = z.real;
                        return ComplexNumber.ofReal(Math.log(x + Math.sqrt(x * x - 1.0)));
                    }
                    return z.add(z.multiply(z).subtract(ComplexNumber.ONE).sqrt()).ln();
                case "atanh":
                    if (z.isReal() && Math.abs(z.real) < 1.0) {
                        double x = z.real;
                        return ComplexNumber.ofReal(0.5 * Math.log((1.0 + x) / (1.0 - x)));
                    }
                    ComplexNumber onePlusZ = ComplexNumber.ONE.add(z);
                    ComplexNumber oneMinusZ = ComplexNumber.ONE.subtract(z);
                    return onePlusZ.divide(oneMinusZ).ln().multiply(0.5);

                // Logarithms & Exponential
                case "ln":
                    return z.ln();
                case "log":
                case "log10":
                    return z.ln().divide(Math.log(10.0));
                case "log2":
                    return z.ln().divide(Math.log(2.0));
                case "exp":
                    return z.exp();
                case "sqrt":
                    return z.sqrt();
                case "cbrt":
                    return z.pow(1.0 / 3.0);

                // Real Number Operations
                case "abs":
                    return ComplexNumber.ofReal(z.abs());
                case "arg":
                    return ComplexNumber.ofReal(angleMode.fromRadians(z.arg()));
                case "conj":
                    return z.conjugate();
                case "real":
                case "re":
                    return ComplexNumber.ofReal(z.real);
                case "imag":
                case "im":
                    return ComplexNumber.ofReal(z.imag);
                case "floor":
                    return ComplexNumber.ofReal(Math.floor(z.real));
                case "ceil":
                case "ceiling":
                    return ComplexNumber.ofReal(Math.ceil(z.real));
                case "round":
                    return ComplexNumber.ofReal(Math.round(z.real));
                case "sign":
                case "sgn":
                    return ComplexNumber.ofReal(Math.signum(z.real));

                // Number Theory
                case "fact":
                case "factorial":
                    if (z.real < 0 || Math.floor(z.real) != z.real) {
                        throw new ArithmeticException("Factorial requires non-negative integer");
                    }
                    return ComplexNumber.ofReal(NumberTheory.factorial((int) z.real).doubleValue());
                case "dfact":
                    if (z.real < 0 || Math.floor(z.real) != z.real) {
                        throw new ArithmeticException("Double factorial requires non-negative integer");
                    }
                    return ComplexNumber.ofReal(NumberTheory.doubleFactorial((int) z.real).doubleValue());
                case "isprime":
                    long nVal = Math.round(z.real);
                    return ComplexNumber.ofReal(NumberTheory.isPrime(nVal) ? 1.0 : 0.0);
            }
        }

        // 2-arg functions
        if (argCount == 2) {
            ComplexNumber a = argNodes.get(0).evaluate(this);
            ComplexNumber b = argNodes.get(1).evaluate(this);

            switch (fn) {
                case "pow":
                    return a.pow(b);
                case "root":
                    return b.pow(ComplexNumber.ONE.divide(a));
                case "log":
                    // log(base, value) = ln(value) / ln(base)
                    return b.ln().divide(a.ln());
                case "gcd":
                    return ComplexNumber.ofReal(NumberTheory.gcd(Math.round(a.real), Math.round(b.real)));
                case "lcm":
                    return ComplexNumber.ofReal(NumberTheory.lcm(Math.round(a.real), Math.round(b.real)));
                case "mod":
                    return ComplexNumber.ofReal(a.real % b.real);
                case "npr":
                case "perm":
                    return ComplexNumber.ofReal(NumberTheory.permutations((int) a.real, (int) b.real).doubleValue());
                case "ncr":
                case "comb":
                    return ComplexNumber.ofReal(NumberTheory.combinations((int) a.real, (int) b.real).doubleValue());
                case "min":
                    return ComplexNumber.ofReal(Math.min(a.real, b.real));
                case "max":
                    return ComplexNumber.ofReal(Math.max(a.real, b.real));
                case "atan2":
                    return ComplexNumber.ofReal(angleMode.fromRadians(Math.atan2(a.real, b.real)));
            }
        }

        // 3-arg functions
        if (argCount == 3) {
            ComplexNumber a = argNodes.get(0).evaluate(this);
            ComplexNumber b = argNodes.get(1).evaluate(this);
            ComplexNumber c = argNodes.get(2).evaluate(this);

            if ("clamp".equals(fn)) {
                double val = a.real;
                double min = b.real;
                double max = c.real;
                return ComplexNumber.ofReal(Math.max(min, Math.min(max, val)));
            }
            if ("binom".equals(fn) || "binomial".equals(fn)) {
                return ComplexNumber.ofReal(StatisticsEngine.binomialProbability((int) a.real, (int) b.real, c.real));
            }
        }

        // Variable-arg statistical functions: mean, median, variance, stddev, sum
        double[] vals = new double[argCount];
        for (int i = 0; i < argCount; i++) {
            vals[i] = argNodes.get(i).evaluate(this).real;
        }

        switch (fn) {
            case "mean":
            case "avg":
                return ComplexNumber.ofReal(StatisticsEngine.mean(vals));
            case "median":
                return ComplexNumber.ofReal(StatisticsEngine.median(vals));
            case "var":
            case "variance":
                return ComplexNumber.ofReal(StatisticsEngine.sampleVariance(vals));
            case "std":
            case "stddev":
                return ComplexNumber.ofReal(StatisticsEngine.sampleStdDev(vals));
            case "sum":
                return ComplexNumber.ofReal(StatisticsEngine.sum(vals));
            case "min":
                return ComplexNumber.ofReal(StatisticsEngine.min(vals));
            case "max":
                return ComplexNumber.ofReal(StatisticsEngine.max(vals));
        }

        throw new IllegalArgumentException("Unknown function '" + name + "' with " + argCount + " argument(s)");
    }
}