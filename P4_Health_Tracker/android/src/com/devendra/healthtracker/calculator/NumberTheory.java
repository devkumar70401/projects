package com.devendra.healthtracker.calculator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Number Theory & Combinatorics Computational Engine.
 * Supports exact arbitrary-precision arithmetic for integer operations
 * and Lanczos Gamma approximation for real factorial.
 */
public final class NumberTheory {

    private NumberTheory() {}

    public static long gcd(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    public static BigInteger gcd(BigInteger a, BigInteger b) {
        return a.abs().gcd(b.abs());
    }

    public static long lcm(long a, long b) {
        if (a == 0 || b == 0) return 0;
        return Math.abs(a / gcd(a, b) * b);
    }

    public static BigInteger lcm(BigInteger a, BigInteger b) {
        if (a.signum() == 0 || b.signum() == 0) return BigInteger.ZERO;
        return a.abs().divide(a.gcd(b)).multiply(b.abs());
    }

    public static boolean isPrime(long n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;

        // Deterministic Miller-Rabin test for 64-bit integers
        // Bases: 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37
        long[] bases = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};
        long d = n - 1;
        int s = 0;
        while (d % 2 == 0) {
            d /= 2;
            s++;
        }

        for (long a : bases) {
            if (n <= a) break;
            BigInteger bA = BigInteger.valueOf(a);
            BigInteger bN = BigInteger.valueOf(n);
            BigInteger bD = BigInteger.valueOf(d);
            BigInteger x = bA.modPow(bD, bN);

            if (x.equals(BigInteger.ONE) || x.equals(BigInteger.valueOf(n - 1))) {
                continue;
            }

            boolean composite = true;
            for (int r = 1; r < s; r++) {
                x = x.modPow(BigInteger.valueOf(2), bN);
                if (x.equals(BigInteger.valueOf(n - 1))) {
                    composite = false;
                    break;
                }
            }
            if (composite) return false;
        }
        return true;
    }

    public static List<Long> primeFactors(long n) {
        if (n <= 1) throw new IllegalArgumentException("Prime factorization requires integer > 1");
        List<Long> factors = new ArrayList<>();
        long d = 2;
        while (d * d <= n) {
            while (n % d == 0) {
                factors.add(d);
                n /= d;
            }
            d = (d == 2) ? 3 : d + 2;
        }
        if (n > 1) {
            factors.add(n);
        }
        return factors;
    }

    public static List<BigInteger> primeFactors(BigInteger n) {
        if (n == null || n.compareTo(BigInteger.ONE) <= 0) {
            throw new IllegalArgumentException("Prime factorization requires integer > 1");
        }
        List<BigInteger> factors = new ArrayList<>();
        BigInteger d = BigInteger.valueOf(2);
        while (d.multiply(d).compareTo(n) <= 0) {
            while (n.mod(d).equals(BigInteger.ZERO)) {
                factors.add(d);
                n = n.divide(d);
            }
            d = d.equals(BigInteger.valueOf(2)) ? BigInteger.valueOf(3) : d.add(BigInteger.valueOf(2));
        }
        if (n.compareTo(BigInteger.ONE) > 0) {
            factors.add(n);
        }
        return factors;
    }

    public static BigInteger factorial(int n) {
        if (n < 0) throw new ArithmeticException("Factorial is not defined for negative integers");
        if (n > 5000) throw new ArithmeticException("Factorial operand too large (limit 5000)");
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    public static BigInteger doubleFactorial(int n) {
        if (n < 0) throw new ArithmeticException("Double factorial is not defined for negative integers");
        if (n > 5000) throw new ArithmeticException("Double factorial operand too large (limit 5000)");
        BigInteger result = BigInteger.ONE;
        for (int i = n; i > 1; i -= 2) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    public static BigInteger permutations(int n, int r) {
        if (n < 0 || r < 0 || r > n) {
            throw new IllegalArgumentException("Permutations require 0 <= r <= n");
        }
        BigInteger result = BigInteger.ONE;
        for (int i = n; i > n - r; i--) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    public static BigInteger combinations(int n, int r) {
        if (n < 0 || r < 0 || r > n) {
            throw new IllegalArgumentException("Combinations require 0 <= r <= n");
        }
        if (r > n / 2) r = n - r; // Symmetry optimization
        BigInteger numer = BigInteger.ONE;
        BigInteger denom = BigInteger.ONE;
        for (int i = 1; i <= r; i++) {
            numer = numer.multiply(BigInteger.valueOf(n - i + 1));
            denom = denom.multiply(BigInteger.valueOf(i));
        }
        return numer.divide(denom);
    }

    public static BigInteger nPr(int n, int r) {
        return permutations(n, r);
    }

    public static BigInteger nCr(int n, int r) {
        return combinations(n, r);
    }

    /**
     * Lanczos approximation for Gamma function (Gamma(x) = (x-1)!).
     * High precision for real values.
     */
    public static double gamma(double x) {
        if (x <= 0.0 && Math.floor(x) == x) {
            throw new ArithmeticException("Domain error: Gamma function is undefined for non-positive integers");
        }
        // Reflection formula for x < 0.5
        if (x < 0.5) {
            return Math.PI / (Math.sin(Math.PI * x) * gamma(1.0 - x));
        }

        // Lanczos coefficients for g=7, N=9
        double[] p = {
                0.99999999999980993,
                676.5203681218851,
                -1259.1392167224028,
                771.32342877765313,
                -176.61502916214059,
                12.507343278686905,
                -0.138571095836524,
                9.9843695780195716e-6,
                1.5056327351493116e-7
        };

        double z = x - 1.0;
        double a = p[0];
        double t = z + 7.5;
        for (int i = 1; i < p.length; i++) {
            a += p[i] / (z + i);
        }
        return Math.sqrt(2 * Math.PI) * Math.pow(t, z + 0.5) * Math.exp(-t) * a;
    }
}