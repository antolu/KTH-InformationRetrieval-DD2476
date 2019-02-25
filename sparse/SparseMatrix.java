package sparse;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import sparse.SparseVector;

/**
 * Implementation of sparse matrix using hashmaps
 * 
 * <p>
 * The index of the upper most layer represents the row of matrix, and its value
 * the row contents.
 * </p>
 */
public class SparseMatrix extends HashMap<Integer, LinkedHashMap<Integer, Double>> {

    public int m;
    public int n;

    public SparseMatrix(int m, int n) {
        super();
        this.m = m;
        this.n = n;
    }

    public LinkedHashMap<Integer, Double> newRow(int i) {
        LinkedHashMap<Integer, Double> row = new LinkedHashMap<>();

        put(i, row);
        return row;
    }

    /**
     * Right multiplies the matrix with a vector
     * 
     * @param vector The vector
     * 
     * @return The product
     * 
     * @throws IllegalArgumentException When matrix-vector dimensions are
     *                                  incompatible.
     */
    public SparseVector multiplyVector(SparseVector vector) {
        if (this.n != vector.m) {
            throw new IllegalArgumentException(
                    "Bad matrix dimensions: " + this.m + "x" + this.n + " , " + 1 + "x" + vector.m);
        }

        SparseVector prod = new SparseVector(vector.m);

        for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> matrixRow : entrySet()) {
            double elem = 0.0;
            for (Map.Entry<Integer, Double> e : matrixRow.getValue().entrySet()) {
                try {
                    elem += e.getValue() * vector.get(e.getKey());
                } catch (NullPointerException ex) {
                    // Do nothing, element does not exist in vector
                }
            }
            prod.put(matrixRow.getKey(), elem);
        }

        return prod;
    }

    public SparseMatrix getTransposed() {
        SparseMatrix transposed = new SparseMatrix(n, m);

        for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> row : entrySet()) {
            for (Map.Entry<Integer, Double> val : row.getValue().entrySet()) {
                if (!transposed.containsKey(val.getKey())) {
                    LinkedHashMap<Integer, Double> transposedRow = transposed.newRow(val.getKey());
                    transposedRow.put(row.getKey(), val.getValue());
                }
                {
                    transposed.get(val.getKey()).put(row.getKey(), val.getValue());
                }
            }
        }

        return transposed;
    }

    public SparseMatrix multiply(SparseMatrix other) {

        if (n != other.m) {
            throw new IllegalArgumentException();
        }

        SparseMatrix prod = new SparseMatrix(m, other.n);

        for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> row : entrySet()) {
            double[] fastRow = new double[other.n];

            LinkedHashMap<Integer, Double> prodRow = prod.newRow(row.getKey());

            for (Map.Entry<Integer, Double> e : row.getValue().entrySet()) {
                if (other.containsKey(e.getKey())) {
                    LinkedHashMap<Integer, Double> otherRow = other.get(e.getKey());

                    double eValue = e.getValue();
                    for (Map.Entry<Integer, Double> otherE : otherRow.entrySet()) {
                        fastRow[otherE.getKey()] += eValue * otherE.getValue();
                    }
                }
            }

            for (int i = 0; i < fastRow.length; i++) {
                if (fastRow[i] != 0) {
                    prodRow.put(i, fastRow[i]);
                }
            }
        }

        return prod;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> i : entrySet()) {
            for (Map.Entry<Integer, Double> j : i.getValue().entrySet()) {
                sb.append("(" + i.getKey() + ", " + j.getKey() + "):" + j.getValue() + " ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
