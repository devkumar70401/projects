package com.devendra.healthtracker.calculator;

import java.util.Arrays;

/**
 * Numerically Stable Matrix and Linear Algebra Engine.
 * Employs Doolittle / Crout LU Decomposition with Partial Pivoting (row swapping)
 * to compute determinants, inverses, and system solutions without numerical instability.
 */
public final class MatrixEngine {
    public final int rows;
    public final int cols;
    private final double[][] data;

    public MatrixEngine(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Matrix dimensions must be positive");
        }
        this.rows = rows;
        this.cols = cols;
        this.data = new double[rows][cols];
    }

    public MatrixEngine(double[][] values) {
        if (values == null || values.length == 0 || values[0].length == 0) {
            throw new IllegalArgumentException("Matrix data cannot be empty");
        }
        this.rows = values.length;
        this.cols = values[0].length;
        this.data = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            if (values[i].length != cols) {
                throw new IllegalArgumentException("All matrix rows must have the same number of columns");
            }
            System.arraycopy(values[i], 0, this.data[i], 0, cols);
        }
    }

    public static MatrixEngine identity(int n) {
        MatrixEngine I = new MatrixEngine(n, n);
        for (int i = 0; i < n; i++) {
            I.data[i][i] = 1.0;
        }
        return I;
    }

    public double get(int r, int c) {
        return data[r][c];
    }

    public void set(int r, int c, double val) {
        data[r][c] = val;
    }

    public double[][] getDataCopy() {
        double[][] copy = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(data[i], 0, copy[i], 0, cols);
        }
        return copy;
    }

    public boolean isSquare() {
        return rows == cols;
    }

    public MatrixEngine add(MatrixEngine other) {
        if (this.rows != other.rows || this.cols != other.cols) {
            throw new IllegalArgumentException("Matrix dimension mismatch for addition: (" +
                    rows + "x" + cols + ") vs (" + other.rows + "x" + other.cols + ")");
        }
        MatrixEngine res = new MatrixEngine(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                res.data[i][j] = this.data[i][j] + other.data[i][j];
            }
        }
        return res;
    }

    public MatrixEngine subtract(MatrixEngine other) {
        if (this.rows != other.rows || this.cols != other.cols) {
            throw new IllegalArgumentException("Matrix dimension mismatch for subtraction: (" +
                    rows + "x" + cols + ") vs (" + other.rows + "x" + other.cols + ")");
        }
        MatrixEngine res = new MatrixEngine(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                res.data[i][j] = this.data[i][j] - other.data[i][j];
            }
        }
        return res;
    }

    public MatrixEngine multiply(MatrixEngine other) {
        if (this.cols != other.rows) {
            throw new IllegalArgumentException("Matrix dimension mismatch for multiplication: (" +
                    rows + "x" + cols + ") * (" + other.rows + "x" + other.cols + ")");
        }
        MatrixEngine res = new MatrixEngine(this.rows, other.cols);
        for (int i = 0; i < this.rows; i++) {
            for (int k = 0; k < this.cols; k++) {
                double a_ik = this.data[i][k];
                if (Math.abs(a_ik) < 1e-15) continue;
                for (int j = 0; j < other.cols; j++) {
                    res.data[i][j] += a_ik * other.data[k][j];
                }
            }
        }
        return res;
    }

    public MatrixEngine multiply(double scalar) {
        MatrixEngine res = new MatrixEngine(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                res.data[i][j] = this.data[i][j] * scalar;
            }
        }
        return res;
    }

    public MatrixEngine transpose() {
        MatrixEngine res = new MatrixEngine(cols, rows);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                res.data[j][i] = this.data[i][j];
            }
        }
        return res;
    }

    public double trace() {
        if (!isSquare()) {
            throw new IllegalArgumentException("Trace is only defined for square matrices");
        }
        double tr = 0.0;
        for (int i = 0; i < rows; i++) {
            tr += data[i][i];
        }
        return tr;
    }

    /**
     * LU Decomposition with Partial Pivoting.
     * Decomposes P * A = L * U.
     */
    public static class LUDecomposition {
        public final double[][] lu;
        public final int[] pivot;
        public final int pivotSign;
        public final boolean isSingular;

        public LUDecomposition(MatrixEngine matrix) {
            if (!matrix.isSquare()) {
                throw new IllegalArgumentException("LU decomposition requires a square matrix");
            }
            int n = matrix.rows;
            this.lu = matrix.getDataCopy();
            this.pivot = new int[n];
            for (int i = 0; i < n; i++) pivot[i] = i;

            int pSign = 1;
            boolean singular = false;

            for (int j = 0; j < n; j++) {
                // Find pivot in column j
                int maxRow = j;
                double maxVal = Math.abs(lu[j][j]);
                for (int i = j + 1; i < n; i++) {
                    double val = Math.abs(lu[i][j]);
                    if (val > maxVal) {
                        maxVal = val;
                        maxRow = i;
                    }
                }

                // Row exchange if necessary
                if (maxRow != j) {
                    double[] temp = lu[maxRow];
                    lu[maxRow] = lu[j];
                    lu[j] = temp;

                    int pTemp = pivot[maxRow];
                    pivot[maxRow] = pivot[j];
                    pivot[j] = pTemp;

                    pSign = -pSign;
                }

                if (Math.abs(lu[j][j]) < 1e-14) {
                    singular = true;
                } else {
                    for (int i = j + 1; i < n; i++) {
                        lu[i][j] /= lu[j][j];
                        for (int k = j + 1; k < n; k++) {
                            lu[i][k] -= lu[i][j] * lu[j][k];
                        }
                    }
                }
            }

            this.pivotSign = pSign;
            this.isSingular = singular;
        }

        public double determinant() {
            if (isSingular) return 0.0;
            int n = lu.length;
            double det = pivotSign;
            for (int i = 0; i < n; i++) {
                det *= lu[i][i];
            }
            return det;
        }

        /**
         * Solves A * X = B for X using forward and backward substitution.
         */
        public MatrixEngine solve(MatrixEngine B) {
            int n = lu.length;
            if (B.rows != n) {
                throw new IllegalArgumentException("Matrix row dimensions must agree");
            }
            if (isSingular) {
                throw new ArithmeticException("Matrix is singular (determinant is 0), system has no unique solution");
            }

            int nColsB = B.cols;
            double[][] X = new double[n][nColsB];

            // Permute B according to pivot
            for (int i = 0; i < n; i++) {
                int p = pivot[i];
                for (int j = 0; j < nColsB; j++) {
                    X[i][j] = B.data[p][j];
                }
            }

            // Forward substitution: L * Y = P * B
            for (int k = 0; k < n; k++) {
                for (int i = k + 1; i < n; i++) {
                    for (int j = 0; j < nColsB; j++) {
                        X[i][j] -= X[k][j] * lu[i][k];
                    }
                }
            }

            // Backward substitution: U * X = Y
            for (int k = n - 1; k >= 0; k--) {
                for (int j = 0; j < nColsB; j++) {
                    X[k][j] /= lu[k][k];
                }
                for (int i = 0; i < k; i++) {
                    for (int j = 0; j < nColsB; j++) {
                        X[i][j] -= X[k][j] * lu[i][k];
                    }
                }
            }

            return new MatrixEngine(X);
        }
    }

    public double determinant() {
        if (!isSquare()) {
            throw new IllegalArgumentException("Determinant is only defined for square matrices");
        }
        if (rows == 1) return data[0][0];
        if (rows == 2) return data[0][0] * data[1][1] - data[0][1] * data[1][0];
        LUDecomposition lu = new LUDecomposition(this);
        return lu.determinant();
    }

    public MatrixEngine inverse() {
        if (!isSquare()) {
            throw new IllegalArgumentException("Matrix inverse requires a square matrix");
        }
        LUDecomposition lu = new LUDecomposition(this);
        if (lu.isSingular || Math.abs(lu.determinant()) < 1e-14) {
            throw new ArithmeticException("Matrix is singular and cannot be inverted");
        }
        return lu.solve(identity(rows));
    }

    public int rank() {
        double[][] mat = getDataCopy();
        int r = 0;
        int m = rows;
        int n = cols;

        for (int col = 0; col < n && r < m; col++) {
            int pivotRow = r;
            double maxVal = Math.abs(mat[r][col]);
            for (int i = r + 1; i < m; i++) {
                if (Math.abs(mat[i][col]) > maxVal) {
                    maxVal = Math.abs(mat[i][col]);
                    pivotRow = i;
                }
            }

            if (maxVal < 1e-12) continue; // Column is zero, skip

            // Swap rows
            double[] temp = mat[r];
            mat[r] = mat[pivotRow];
            mat[pivotRow] = temp;

            // Eliminate
            for (int i = r + 1; i < m; i++) {
                double factor = mat[i][col] / mat[r][col];
                for (int j = col; j < n; j++) {
                    mat[i][j] -= factor * mat[r][j];
                }
            }
            r++;
        }
        return r;
    }

    public String format(int decimals) {
        StringBuilder sb = new StringBuilder();
        String fmt = "%." + decimals + "f";
        sb.append("[");
        for (int i = 0; i < rows; i++) {
            if (i > 0) sb.append("; ");
            sb.append("[");
            for (int j = 0; j < cols; j++) {
                if (j > 0) sb.append(", ");
                String v = String.format(java.util.Locale.US, fmt, data[i][j]);
                if (v.contains(".")) {
                    while (v.endsWith("0")) v = v.substring(0, v.length() - 1);
                    if (v.endsWith(".")) v = v.substring(0, v.length() - 1);
                }
                sb.append(v);
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    public double[] solve(double[] b) {
        if (!isSquare()) throw new IllegalArgumentException("Matrix must be square to solve linear system");
        if (b == null || b.length != rows) throw new IllegalArgumentException("Vector length must equal matrix rows");
        double[][] bMat = new double[b.length][1];
        for (int i = 0; i < b.length; i++) bMat[i][0] = b[i];
        LUDecomposition lu = new LUDecomposition(this);
        MatrixEngine xMat = lu.solve(new MatrixEngine(bMat));
        double[] result = new double[rows];
        for (int i = 0; i < rows; i++) result[i] = xMat.data[i][0];
        return result;
    }

    @Override
    public String toString() {
        return format(4);
    }
}