package com.devendra.healthtracker.calculator;

import java.util.Arrays;

/**
 * Statistical and Probability Analysis Engine.
 * Implements two-pass / Welford algorithm to prevent catastrophic numerical cancellation.
 */
public final class StatisticsEngine {

    private StatisticsEngine() {}

    public static double sum(double[] values) {
        if (values == null || values.length == 0) return 0.0;
        // Kahan summation algorithm for precision
        double sum = 0.0;
        double c = 0.0;
        for (double v : values) {
            double y = v - c;
            double t = sum + y;
            c = (t - sum) - y;
            sum = t;
        }
        return sum;
    }

    public static double mean(double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Cannot compute mean of empty dataset");
        }
        return sum(values) / values.length;
    }

    public static double weightedMean(double[] values, double[] weights) {
        if (values == null || weights == null || values.length != weights.length || values.length == 0) {
            throw new IllegalArgumentException("Values and weights must be non-empty and have matching lengths");
        }
        double sumW = 0.0;
        double sumVW = 0.0;
        for (int i = 0; i < values.length; i++) {
            if (weights[i] < 0) throw new IllegalArgumentException("Weights cannot be negative");
            sumVW += values[i] * weights[i];
            sumW += weights[i];
        }
        if (sumW == 0.0) throw new ArithmeticException("Sum of weights cannot be zero");
        return sumVW / sumW;
    }

    public static double median(double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Cannot compute median of empty dataset");
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        if (n % 2 == 1) {
            return sorted[n / 2];
        } else {
            return (sorted[(n / 2) - 1] + sorted[n / 2]) / 2.0;
        }
    }

    /**
     * Sample variance with Bessel's correction (n - 1 denominator).
     * Uses Welford's algorithm to eliminate catastrophic cancellation.
     */
    public static double sampleVariance(double[] values) {
        if (values == null || values.length < 2) {
            throw new IllegalArgumentException("Sample variance requires at least 2 data points");
        }
        double mean = 0.0;
        double m2 = 0.0;
        for (int i = 0; i < values.length; i++) {
            double delta = values[i] - mean;
            mean += delta / (i + 1);
            double delta2 = values[i] - mean;
            m2 += delta * delta2;
        }
        return m2 / (values.length - 1);
    }

    public static double populationVariance(double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Population variance requires at least 1 data point");
        }
        double mean = 0.0;
        double m2 = 0.0;
        for (int i = 0; i < values.length; i++) {
            double delta = values[i] - mean;
            mean += delta / (i + 1);
            double delta2 = values[i] - mean;
            m2 += delta * delta2;
        }
        return m2 / values.length;
    }

    public static double sampleStdDev(double[] values) {
        return Math.sqrt(sampleVariance(values));
    }

    public static double populationStdDev(double[] values) {
        return Math.sqrt(populationVariance(values));
    }

    public static double min(double[] values) {
        if (values == null || values.length == 0) throw new IllegalArgumentException("Empty dataset");
        double m = values[0];
        for (double v : values) if (v < m) m = v;
        return m;
    }

    public static double max(double[] values) {
        if (values == null || values.length == 0) throw new IllegalArgumentException("Empty dataset");
        double m = values[0];
        for (double v : values) if (v > m) m = v;
        return m;
    }

    /**
     * Standard Normal Cumulative Distribution Function (CDF) using Abramowitz & Stegun rational approximation.
     */
    public static double standardNormalCdf(double z) {
        if (z < -8.0) return 0.0;
        if (z > 8.0) return 1.0;

        double t = 1.0 / (1.0 + 0.2316419 * Math.abs(z));
        double d = 0.3989422804014327 * Math.exp(-z * z / 2.0); // 1 / sqrt(2*pi) * exp(-z^2/2)
        double prob = d * t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))));

        return (z > 0) ? (1.0 - prob) : prob;
    }

    public static double normalCdf(double x, double mean, double stdDev) {
        if (stdDev <= 0) throw new IllegalArgumentException("Standard deviation must be positive");
        return standardNormalCdf((x - mean) / stdDev);
    }

    /**
     * Binomial probability: P(X = k) = (n choose k) * p^k * (1-p)^(n-k).
     */
    public static double binomialProbability(int n, int k, double p) {
        if (n < 0 || k < 0 || k > n || p < 0.0 || p > 1.0) {
            throw new IllegalArgumentException("Invalid binomial parameters");
        }
        double nCk = NumberTheory.combinations(n, k).doubleValue();
        return nCk * Math.pow(p, k) * Math.pow(1.0 - p, n - k);
    }
}