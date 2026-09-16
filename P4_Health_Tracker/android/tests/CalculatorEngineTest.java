package com.devendra.healthtracker.tests;

import com.devendra.healthtracker.calculator.*;

import java.math.BigInteger;
import java.util.List;

public class CalculatorEngineTest {

    public static void runAll() {
        System.out.println("--- Starting Calculator Subsystem Tests ---");

        testArithmeticPrecedenceAndAssociativity();
        testTrigonometryAndAngleModes();
        testComplexNumbers();
        testMatrixEngine();
        testVector3D();
        testNumberTheory();
        testStatisticsEngine();
        testProgrammingEngine();
        testUnitRegistryAndDimensionalAnalysis();
        testParserSafetyAndFunctions();

        System.out.println("✅ ALL Calculator Subsystem Tests Passed!");
    }

    private static void assertEquals(double expected, double actual, double epsilon, String msg) {
        if (Double.isNaN(expected) && Double.isNaN(actual)) return;
        if (Math.abs(expected - actual) > epsilon) {
            throw new AssertionError(msg + " -> Expected: " + expected + ", got: " + actual);
        }
    }

    private static void assertEquals(long expected, long actual, String msg) {
        if (expected != actual) {
            throw new AssertionError(msg + " -> Expected: " + expected + ", got: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + msg);
        }
    }

    private static void testArithmeticPrecedenceAndAssociativity() {
        System.out.println("  Testing Arithmetic Precedence & Associativity...");
        CalculatorEngine engine = new CalculatorEngine();

        // 2 + 3 * 4 = 14 (not 20)
        String r1 = engine.evaluateExpression("2 + 3 * 4");
        assertEquals(14.0, Double.parseDouble(r1), 1e-9, "2 + 3 * 4 != 14");

        // (2 + 3) * 4 = 20
        String r2 = engine.evaluateExpression("(2 + 3) * 4");
        assertEquals(20.0, Double.parseDouble(r2), 1e-9, "(2 + 3) * 4 != 20");

        // Exponentiation right-associative: 2 ^ 3 ^ 2 = 2 ^ 9 = 512 (not 64)
        String r3 = engine.evaluateExpression("2 ^ 3 ^ 2");
        assertEquals(512.0, Double.parseDouble(r3), 1e-9, "2 ^ 3 ^ 2 != 512");

        // Postfix factorial: 5! = 120
        String r4 = engine.evaluateExpression("5!");
        assertEquals(120.0, Double.parseDouble(r4), 1e-9, "5! != 120");

        // 5! + 3 = 123
        String r5 = engine.evaluateExpression("5! + 3");
        assertEquals(123.0, Double.parseDouble(r5), 1e-9, "5! + 3 != 123");

        // Implicit multiplication: 2(3 + 4) = 14
        String r6 = engine.evaluateExpression("2(3 + 4)");
        assertEquals(14.0, Double.parseDouble(r6), 1e-9, "2(3 + 4) != 14");

        // Unary minus with power: -3^2 = -(3^2) = -9
        String r7 = engine.evaluateExpression("-3^2");
        assertEquals(-9.0, Double.parseDouble(r7), 1e-9, "-3^2 != -9");

        // (-3)^2 = 9
        String r8 = engine.evaluateExpression("(-3)^2");
        assertEquals(9.0, Double.parseDouble(r8), 1e-9, "(-3)^2 != 9");
    }

    private static void testTrigonometryAndAngleModes() {
        System.out.println("  Testing Trigonometry & Angle Modes...");
        CalculatorEngine engine = new CalculatorEngine();

        // DEG mode
        engine.setAngleMode(AngleMode.DEG);
        assertEquals(1.0, Double.parseDouble(engine.evaluateExpression("sin(90)")), 1e-9, "sin(90 deg) != 1");
        assertEquals(0.0, Double.parseDouble(engine.evaluateExpression("cos(90)")), 1e-9, "cos(90 deg) != 0");
        assertEquals(1.0, Double.parseDouble(engine.evaluateExpression("tan(45)")), 1e-9, "tan(45 deg) != 1");

        // RAD mode
        engine.setAngleMode(AngleMode.RAD);
        assertEquals(0.0, Double.parseDouble(engine.evaluateExpression("sin(pi)")), 1e-9, "sin(pi) != 0");
        assertEquals(-1.0, Double.parseDouble(engine.evaluateExpression("cos(pi)")), 1e-9, "cos(pi) != -1");

        // GRAD mode
        engine.setAngleMode(AngleMode.GRAD);
        assertEquals(1.0, Double.parseDouble(engine.evaluateExpression("sin(100)")), 1e-9, "sin(100 grad) != 1");
    }

    private static void testComplexNumbers() {
        System.out.println("  Testing Complex Numbers...");
        ComplexNumber z1 = new ComplexNumber(3, 4); // 3 + 4i
        ComplexNumber z2 = new ComplexNumber(1, -2); // 1 - 2i

        ComplexNumber add = z1.add(z2);
        assertEquals(4.0, add.getReal(), 1e-9, "z1 + z2 real");
        assertEquals(2.0, add.getImaginary(), 1e-9, "z1 + z2 imag");

        ComplexNumber mul = z1.multiply(z2); // (3+4i)(1-2i) = 3 - 6i + 4i + 8 = 11 - 2i
        assertEquals(11.0, mul.getReal(), 1e-9, "z1 * z2 real");
        assertEquals(-2.0, mul.getImaginary(), 1e-9, "z1 * z2 imag");

        ComplexNumber div = mul.divide(z2); // Should recover z1 = 3 + 4i
        assertEquals(3.0, div.getReal(), 1e-9, "div real");
        assertEquals(4.0, div.getImaginary(), 1e-9, "div imag");

        assertEquals(5.0, z1.abs(), 1e-9, "|3 + 4i| != 5");
    }

    private static void testMatrixEngine() {
        System.out.println("  Testing Matrix Operations & LU Decomposition...");
        double[][] dataA = {
            {2, 1, -1},
            {-3, -1, 2},
            {-2, 1, 2}
        };
        MatrixEngine A = new MatrixEngine(dataA);

        // Determinant of A = -1
        double det = A.determinant();
        assertEquals(-1.0, det, 1e-9, "Matrix det != -1");

        // Inverse A * A^-1 == Identity
        MatrixEngine invA = A.inverse();
        MatrixEngine identity = A.multiply(invA);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double expected = (i == j) ? 1.0 : 0.0;
                assertEquals(expected, identity.get(i, j), 1e-9, "A * inv(A) at (" + i + "," + j + ")");
            }
        }

        // Linear system solve: Ax = b where b = [8, -11, -3]^T
        // Expected solution: x = [2, 3, -1]^T
        double[] b = {8, -11, -3};
        double[] x = A.solve(b);
        assertEquals(2.0, x[0], 1e-7, "solve x[0]");
        assertEquals(3.0, x[1], 1e-7, "solve x[1]");
        assertEquals(-1.0, x[2], 1e-7, "solve x[2]");

        // Rank and Trace
        assertEquals(3, A.rank(), "Rank of A != 3");
        assertEquals(3.0, A.trace(), 1e-9, "Trace of A (2 + -1 + 2 = 3)");
    }

    private static void testVector3D() {
        System.out.println("  Testing 3D Vector Operations...");
        Vector3D v1 = new Vector3D(1, 2, 3);
        Vector3D v2 = new Vector3D(4, 5, 6);

        // Dot product: 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        assertEquals(32.0, v1.dot(v2), 1e-9, "v1 . v2 != 32");

        // Cross product: (2*6 - 3*5, 3*4 - 1*6, 1*5 - 2*4) = (-3, 6, -3)
        Vector3D cross = v1.cross(v2);
        assertEquals(-3.0, cross.getX(), 1e-9, "cross.x");
        assertEquals(6.0, cross.getY(), 1e-9, "cross.y");
        assertEquals(-3.0, cross.getZ(), 1e-9, "cross.z");

        // Normalization
        Vector3D vNorm = v1.normalize();
        assertEquals(1.0, vNorm.magnitude(), 1e-9, "norm magnitude");
    }

    private static void testNumberTheory() {
        System.out.println("  Testing Number Theory & Combinatorics...");
        // Factorials
        assertEquals(120L, NumberTheory.factorial(5).longValue(), "5! != 120");
        assertEquals(15L, NumberTheory.doubleFactorial(5).longValue(), "5!! != 15 (5*3*1)");
        assertEquals(48L, NumberTheory.doubleFactorial(6).longValue(), "6!! != 48 (6*4*2)");

        // nPr & nCr
        assertEquals(60L, NumberTheory.nPr(5, 3).longValue(), "5 P 3 != 60");
        assertEquals(10L, NumberTheory.nCr(5, 3).longValue(), "5 C 3 != 10");

        // GCD & LCM
        assertEquals(6L, NumberTheory.gcd(BigInteger.valueOf(54), BigInteger.valueOf(24)).longValue(), "gcd(54, 24) != 6");
        assertEquals(216L, NumberTheory.lcm(BigInteger.valueOf(54), BigInteger.valueOf(24)).longValue(), "lcm(54, 24) != 216");

        // Primality (Deterministic Miller-Rabin)
        assertTrue(NumberTheory.isPrime(2), "2 is prime");
        assertTrue(NumberTheory.isPrime(104729), "104729 is prime");
        assertTrue(!NumberTheory.isPrime(104728), "104728 not prime");

        // Prime Factors of 60 = 2^2 * 3 * 5
        List<BigInteger> factors = NumberTheory.primeFactors(BigInteger.valueOf(60));
        assertEquals(4, factors.size(), "primeFactors size");
        assertEquals(2L, factors.get(0).longValue(), "f0");
        assertEquals(2L, factors.get(1).longValue(), "f1");
        assertEquals(3L, factors.get(2).longValue(), "f2");
        assertEquals(5L, factors.get(3).longValue(), "f3");
    }

    private static void testStatisticsEngine() {
        System.out.println("  Testing Statistics Engine...");
        double[] sample = {10.0, 12.0, 23.0, 23.0, 16.0, 23.0, 21.0, 16.0};
        // Sum = 144, Count = 8, Mean = 18.0
        assertEquals(18.0, StatisticsEngine.mean(sample), 1e-9, "Mean != 18.0");
        // Sorted: 10, 12, 16, 16, 21, 23, 23, 23. Median = (16+21)/2 = 18.5
        assertEquals(18.5, StatisticsEngine.median(sample), 1e-9, "Median != 18.5");

        // Sample Variance: sum((x - 18)^2) / 7 = 192 / 7
        assertEquals(192.0 / 7.0, StatisticsEngine.sampleVariance(sample), 1e-9, "Sample Variance");
        assertEquals(Math.sqrt(192.0 / 7.0), StatisticsEngine.sampleStdDev(sample), 1e-9, "Sample StdDev");

        // Normal CDF at 0 (mean=0, std=1) -> 0.5
        assertEquals(0.5, StatisticsEngine.normalCdf(0.0, 0.0, 1.0), 1e-5, "Normal CDF at mean");
    }

    private static void testProgrammingEngine() {
        System.out.println("  Testing Programming Mode Bitwise & Overflow...");
        ProgrammingEngine.BitWidth b8 = ProgrammingEngine.BitWidth.BYTE;

        // Masking
        assertEquals(0xFE, 0x1FE & b8.getMask(), "Mask 0x1FE to 8-bit != 0xFE");

        // Bitwise AND/OR/XOR/NOT
        assertEquals(0x0C, ProgrammingEngine.bitwiseAnd(0x0F, 0x1C, b8), "AND");
        assertEquals(0x1F, ProgrammingEngine.bitwiseOr(0x0F, 0x10, b8), "OR");
        assertEquals(0x13, ProgrammingEngine.bitwiseXor(0x0F, 0x1C, b8), "XOR");
        assertEquals(0xF0, ProgrammingEngine.bitwiseNot(0x0F, b8), "NOT 0x0F");

        // Shifts
        assertEquals(0x20, ProgrammingEngine.shiftLeft(0x04, 3, b8), "SHL 4 by 3");
        assertEquals(0x02, ProgrammingEngine.shiftRightLogical(0x10, 3, b8), "SHR 16 by 3");

        // Bit toggling
        assertEquals(0x05, ProgrammingEngine.setBit(0x01, 2, b8), "Set bit 2");
        assertEquals(0x01, ProgrammingEngine.clearBit(0x05, 2, b8), "Clear bit 2");
        assertEquals(0x05, ProgrammingEngine.toggleBit(0x01, 2, b8), "Toggle bit 2");

        // Overflow detection: 127 + 1 in 8-bit signed overflows to -128
        ProgrammingEngine.EvaluationResult resAddOverflow = ProgrammingEngine.add(127, 1, b8, true);
        assertTrue(resAddOverflow.hasOverflow, "127 + 1 signed 8-bit overflow");

        ProgrammingEngine.EvaluationResult resAddNormal = ProgrammingEngine.add(50, 50, b8, true);
        assertTrue(!resAddNormal.hasOverflow, "50 + 50 signed 8-bit no overflow");
    }

    private static void testUnitRegistryAndDimensionalAnalysis() {
        System.out.println("  Testing Unit Registry & Dimensional Rigor...");
        UnitRegistry reg = UnitRegistry.getInstance();

        // 1. Length conversions: 1 km = 1000 m
        UnitDefinition km = reg.findUnit("km");
        UnitDefinition m = reg.findUnit("m");
        double mInKm = reg.convert(1.0, km, m);
        assertEquals(1000.0, mInKm, 1e-9, "1 km in m");

        // 2. Affine conversions: 100 C = 212 F
        UnitDefinition degC = reg.findUnit("°C");
        UnitDefinition degF = reg.findUnit("°F");
        double fFromC = reg.convert(100.0, degC, degF);
        assertEquals(212.0, fFromC, 1e-9, "100 C in F");

        // 0 C = 32 F
        double fFromC0 = reg.convert(0.0, degC, degF);
        assertEquals(32.0, fFromC0, 1e-9, "0 C in F");

        // 3. Dimensional Quantity Addition: 5 m + 200 cm = 7 m
        UnitDefinition cm = reg.findUnit("cm");
        DimensionalQuantity q1 = new DimensionalQuantity(5.0, m);
        DimensionalQuantity q2 = new DimensionalQuantity(200.0, cm);
        DimensionalQuantity qAdd = q1.add(q2);
        assertEquals(7.0, qAdd.getValue(), 1e-9, "5m + 200cm");

        // 4. Dimensional Quantity Multiplication: 10 m * 5 m = 50 m^2
        DimensionalQuantity qMul = q1.multiply(new DimensionalQuantity(10.0, m));
        assertEquals(50.0, qMul.getValue(), 1e-9, "5m * 10m");
        assertTrue(qMul.getDimension().equals(Dimension.AREA), "Dimension should be Area");

        // 5. Dimensional Quantity Division: 100 m / 20 s = 5 m/s
        UnitDefinition s = reg.findUnit("s");
        DimensionalQuantity qDist = new DimensionalQuantity(100.0, m);
        DimensionalQuantity qTime = new DimensionalQuantity(20.0, s);
        DimensionalQuantity qSpeed = qDist.divide(qTime);
        assertEquals(5.0, qSpeed.getValue(), 1e-9, "100m / 20s");
        assertTrue(qSpeed.getDimension().equals(Dimension.VELOCITY), "Dimension should be Velocity");

        // 6. Dimensional Safety Rejection: 5 m + 3 kg MUST THROW EXCEPTION
        UnitDefinition kg = reg.findUnit("kg");
        DimensionalQuantity qMass = new DimensionalQuantity(3.0, kg);
        boolean caught = false;
        try {
            q1.add(qMass);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assertTrue(caught, "Adding 5 m and 3 kg MUST throw IllegalArgumentException");
    }

    private static void testParserSafetyAndFunctions() {
        System.out.println("  Testing Parser Safety & User Functions...");
        CalculatorEngine engine = new CalculatorEngine();
        EvaluationContext ctx = engine.getEvaluationContext();

        // Variables
        ctx.setVariable("x", 7.0);
        String resVar = engine.evaluateExpression("x * 2 + 1");
        assertEquals(15.0, Double.parseDouble(resVar), 1e-9, "x * 2 + 1 with x=7");

        // User defined function: f(t) = 3*t + 5
        ctx.setUserFunction("f", "t", "3*t + 5");
        String resFunc = engine.evaluateExpression("f(4)");
        assertEquals(17.0, Double.parseDouble(resFunc), 1e-9, "f(4) with f(t)=3*t+5");

        // Depth limit safety
        StringBuilder deepExpr = new StringBuilder();
        for (int i = 0; i < 70; i++) deepExpr.append("(");
        deepExpr.append("1");
        for (int i = 0; i < 70; i++) deepExpr.append(")");
        String errRes = engine.evaluateExpression(deepExpr.toString());
        assertTrue(errRes.startsWith("Error"), "Deep expression should trigger safety error");
    }
}
