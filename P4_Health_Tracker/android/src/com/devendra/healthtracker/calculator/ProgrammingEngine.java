package com.devendra.healthtracker.calculator;

import java.math.BigInteger;

/**
 * Programmer Computational Engine.
 * Supports explicit 8, 16, 32, and 64-bit signed/unsigned integer arithmetic,
 * bitwise transformations, shift/rotate operations, and explicit overflow detection.
 */
public final class ProgrammingEngine {

    public enum BitWidth {
        BYTE(8, "8-bit (Byte)"),
        WORD(16, "16-bit (Word)"),
        DWORD(32, "32-bit (Dword)"),
        QWORD(64, "64-bit (Qword)");

        public final int bits;
        public final String label;

        BitWidth(int bits, String label) {
            this.bits = bits;
            this.label = label;
        }

        public long getMask() {
            if (bits == 64) return -1L;
            return (1L << bits) - 1L;
        }

        public long getMinSigned() {
            if (bits == 64) return Long.MIN_VALUE;
            return -(1L << (bits - 1));
        }

        public long getMaxSigned() {
            if (bits == 64) return Long.MAX_VALUE;
            return (1L << (bits - 1)) - 1L;
        }

        public long getMaxUnsigned() {
            if (bits == 64) return -1L; // 0xFFFFFFFFFFFFFFFF in two's complement
            return (1L << bits) - 1L;
        }
    }

    public static class EvaluationResult {
        public final long value;
        public final BitWidth width;
        public final boolean isSigned;
        public final boolean hasOverflow;
        public final String overflowMessage;

        public EvaluationResult(long value, BitWidth width, boolean isSigned, boolean hasOverflow, String overflowMessage) {
            this.value = value;
            this.width = width;
            this.isSigned = isSigned;
            this.hasOverflow = hasOverflow;
            this.overflowMessage = overflowMessage;
        }

        public String toDecimalString() {
            if (isSigned) {
                return String.valueOf(clampToSigned(value, width));
            } else {
                if (width == BitWidth.QWORD) {
                    return Long.toUnsignedString(value);
                } else {
                    return String.valueOf(value & width.getMask());
                }
            }
        }

        public String toHexString() {
            long masked = value & width.getMask();
            String hex = Long.toHexString(masked).toUpperCase();
            int chars = width.bits / 4;
            while (hex.length() < chars) hex = "0" + hex;
            return "0x" + hex;
        }

        public String toBinaryString() {
            long masked = value & width.getMask();
            StringBuilder sb = new StringBuilder();
            for (int i = width.bits - 1; i >= 0; i--) {
                sb.append((masked & (1L << i)) != 0 ? '1' : '0');
                if (i > 0 && i % 8 == 0) sb.append(' ');
            }
            return sb.toString();
        }

        public String toOctalString() {
            long masked = value & width.getMask();
            return "0o" + Long.toOctalString(masked);
        }
    }

    public static long clampToSigned(long val, BitWidth width) {
        long masked = val & width.getMask();
        if (width.bits < 64) {
            long signBit = 1L << (width.bits - 1);
            if ((masked & signBit) != 0) {
                // Negative in two's complement
                return masked - (1L << width.bits);
            }
        }
        return masked;
    }

    public static EvaluationResult add(long a, long b, BitWidth width, boolean isSigned) {
        long raw = a + b;
        long masked = raw & width.getMask();
        boolean overflow = false;
        String msg = null;

        if (isSigned) {
            long aSigned = clampToSigned(a, width);
            long bSigned = clampToSigned(b, width);
            long resSigned = clampToSigned(raw, width);
            if ((aSigned > 0 && bSigned > 0 && resSigned <= 0) || (aSigned < 0 && bSigned < 0 && resSigned >= 0)) {
                overflow = true;
                msg = "Signed integer overflow in " + width.bits + "-bit add";
            }
        } else {
            BigInteger bA = BigInteger.valueOf(a & width.getMask());
            BigInteger bB = BigInteger.valueOf(b & width.getMask());
            BigInteger bSum = bA.add(bB);
            BigInteger bMax = BigInteger.valueOf(2).pow(width.bits);
            if (bSum.compareTo(bMax) >= 0) {
                overflow = true;
                msg = "Unsigned integer overflow (exceeded 2^" + width.bits + " - 1)";
            }
        }

        return new EvaluationResult(masked, width, isSigned, overflow, msg);
    }

    public static EvaluationResult subtract(long a, long b, BitWidth width, boolean isSigned) {
        long raw = a - b;
        long masked = raw & width.getMask();
        boolean overflow = false;
        String msg = null;

        if (isSigned) {
            long aSigned = clampToSigned(a, width);
            long bSigned = clampToSigned(b, width);
            long resSigned = clampToSigned(raw, width);
            if ((aSigned > 0 && bSigned < 0 && resSigned <= 0) || (aSigned < 0 && bSigned > 0 && resSigned >= 0)) {
                overflow = true;
                msg = "Signed integer underflow/overflow in " + width.bits + "-bit sub";
            }
        } else {
            long uA = a & width.getMask();
            long uB = b & width.getMask();
            if (Long.compareUnsigned(uA, uB) < 0) {
                overflow = true;
                msg = "Unsigned underflow below 0";
            }
        }

        return new EvaluationResult(masked, width, isSigned, overflow, msg);
    }

    public static EvaluationResult multiply(long a, long b, BitWidth width, boolean isSigned) {
        long raw = a * b;
        long masked = raw & width.getMask();
        boolean overflow = false;

        BigInteger bA = isSigned ? BigInteger.valueOf(clampToSigned(a, width)) : BigInteger.valueOf(a & width.getMask());
        BigInteger bB = isSigned ? BigInteger.valueOf(clampToSigned(b, width)) : BigInteger.valueOf(b & width.getMask());
        BigInteger prod = bA.multiply(bB);

        BigInteger minVal = isSigned ? BigInteger.valueOf(width.getMinSigned()) : BigInteger.ZERO;
        BigInteger maxVal = isSigned ? BigInteger.valueOf(width.getMaxSigned()) : BigInteger.valueOf(2).pow(width.bits).subtract(BigInteger.ONE);

        if (prod.compareTo(minVal) < 0 || prod.compareTo(maxVal) > 0) {
            overflow = true;
        }

        return new EvaluationResult(masked, width, isSigned, overflow, overflow ? "Integer multiplication overflow" : null);
    }

    public static EvaluationResult divide(long a, long b, BitWidth width, boolean isSigned) {
        long uB = isSigned ? clampToSigned(b, width) : (b & width.getMask());
        if (uB == 0) {
            throw new ArithmeticException("Division by zero");
        }
        long raw;
        if (isSigned) {
            raw = clampToSigned(a, width) / uB;
        } else {
            raw = Long.divideUnsigned(a & width.getMask(), uB);
        }
        return new EvaluationResult(raw & width.getMask(), width, isSigned, false, null);
    }

    public static long bitwiseAnd(long a, long b, BitWidth width) {
        return (a & b) & width.getMask();
    }

    public static long bitwiseOr(long a, long b, BitWidth width) {
        return (a | b) & width.getMask();
    }

    public static long bitwiseXor(long a, long b, BitWidth width) {
        return (a ^ b) & width.getMask();
    }

    public static long bitwiseNot(long a, BitWidth width) {
        return (~a) & width.getMask();
    }

    public static long shiftLeft(long a, int shift, BitWidth width) {
        shift = shift % width.bits;
        return (a << shift) & width.getMask();
    }

    public static long shiftRightLogical(long a, int shift, BitWidth width) {
        shift = shift % width.bits;
        long masked = a & width.getMask();
        return (masked >>> shift) & width.getMask();
    }

    public static long shiftRightArithmetic(long a, int shift, BitWidth width) {
        shift = shift % width.bits;
        long signed = clampToSigned(a, width);
        return (signed >> shift) & width.getMask();
    }

    public static long rotateLeft(long a, int shift, BitWidth width) {
        shift = shift % width.bits;
        long masked = a & width.getMask();
        return ((masked << shift) | (masked >>> (width.bits - shift))) & width.getMask();
    }

    public static long rotateRight(long a, int shift, BitWidth width) {
        shift = shift % width.bits;
        long masked = a & width.getMask();
        return ((masked >>> shift) | (masked << (width.bits - shift))) & width.getMask();
    }

    public static long toggleBit(long a, int bitIndex, BitWidth width) {
        if (bitIndex < 0 || bitIndex >= width.bits) {
            throw new IllegalArgumentException("Bit index must be between 0 and " + (width.bits - 1));
        }
        return (a ^ (1L << bitIndex)) & width.getMask();
    }

    public static long setBit(long a, int bitIndex, BitWidth width) {
        if (bitIndex < 0 || bitIndex >= width.bits) {
            throw new IllegalArgumentException("Bit index must be between 0 and " + (width.bits - 1));
        }
        return (a | (1L << bitIndex)) & width.getMask();
    }

    public static long clearBit(long a, int bitIndex, BitWidth width) {
        if (bitIndex < 0 || bitIndex >= width.bits) {
            throw new IllegalArgumentException("Bit index must be between 0 and " + (width.bits - 1));
        }
        return (a & ~(1L << bitIndex)) & width.getMask();
    }
}