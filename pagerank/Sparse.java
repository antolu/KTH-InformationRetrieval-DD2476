package pagerank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public class Sparse extends HashMap<Integer, Double> {

    private static final Normalizer norm = Normalizer.EUCLIDEAN;

    private final HashSet<Integer> rowHasData = new HashSet<>();

    public int m = 0;
    public int n = 0;
    public double defaultValue = 0.0;

    public Sparse(int m, int n) {
        this.m = m;
        this.n = n;
    }

    public Sparse(int m, int n, double defaultValue) {
        this.m = m;
        this.n = n;
        this.defaultValue = defaultValue;
    }

    public void add(int i, int j, double val) {
        put(i * (m-1) + j, val);
        rowHasData.add(i);
    }

    public boolean contains(int i, int j) {
        return containsKey(i * (m-1) + j);
    }

    public void normalize() {
        double num = 0.0;
        Set<Map.Entry<Integer, Double>> entrySet = entrySet();

        int numElements = 0;
        for (Map.Entry<Integer, Double> entry : entrySet) {
            if (norm == Normalizer.EUCLIDEAN) {
                num += Math.pow(entry.getValue(), 2);
            } else if (norm == Normalizer.MANHATTAN) {
                num += Math.abs(entry.getValue());
            }
            numElements++;
        }

        num += (m * n - numElements) * Math.pow(defaultValue, 2.0);

        if (norm == Normalizer.EUCLIDEAN) {
            num = Math.sqrt(num);
        }
        
        for (Map.Entry<Integer, Double> entry : entrySet) {
            entry.setValue(entry.getValue() / num);
        }
    }

    public static Sparse multiply(Sparse a, Sparse b) throws IllegalArgumentException {
        
        int m = a.m;
        int n = b.n;

        if (a.n != b.n) {
            throw new IllegalArgumentException("Incompatible matrix dimensions: " + a.m + " x " + a.n + " " + b.m + "x" + b.n);
        }

        Sparse prod = new Sparse(m, n, a.defaultValue * b.defaultValue);
        
        int product_val;
        for (int i = 0; i < m; i++){
            for (int j = 0; j < n; j++){
                product_val = 0;
                for( int k = 0; k < b.m; k++) {
                    System.err.println(i + " " + j + " " + k);
                    if (a.contains(i, k) && b.contains(k, j))
                        product_val += a.get(i * (a.m - 1) +k) * b.get(k* (b.m-1) + j);
                    else if (!a.contains(i, k)) 
                        product_val += a.defaultValue * b.get(k* (b.m-1) + j);
                    else if (!b.contains(k, j)) 
                        product_val += a.get(i * (a.m - 1) +k) * b.defaultValue;
                } 
                prod.add(i, j, product_val);
            }
        }

        return prod;
    }

    public static Sparse add(Sparse a, Sparse b) throws IllegalArgumentException {
        
        int m = a.m;
        int n = b.n;

        if (a.m != b.m || a.n != b.n) {
            throw new IllegalArgumentException("Incompatible matrix dimensions: " + a.m + " x " + a.n + " " + b.m + "x" + b.n);
        }

        Sparse ret = new Sparse(m, n);

        HashSet<Integer> writtenIndexes = new HashSet<>();
        
        double val;
        for (Map.Entry<Integer, Double> entry: a.entrySet()) {
            int key = entry.getKey();
            if (b.containsKey(key)) {
                val = entry.getValue() + b.get(key);
                ret.put(key, val);
            } else {
                ret.put(key, entry.getValue());
            }
            writtenIndexes.add(key);
        }

        for (Map.Entry<Integer, Double> entry: b.entrySet()) {
            int key = entry.getKey();
            if (!writtenIndexes.contains(key)) {
                ret.put(key, entry.getValue());
            }
        }

        a.defaultValue += b.defaultValue;

        return ret;
    }

    public static Sparse subtract(Sparse a, Sparse b) throws IllegalArgumentException {
        
        int m = a.m;
        int n = b.n;

        if (a.m != b.m || a.n != b.n) {
            throw new IllegalArgumentException("Incompatible matrix dimensions: " + a.m + " x " + a.n + " " + b.m + "x" + b.n);
        }

        Sparse ret = new Sparse(m, n);

        HashSet<Integer> writtenIndexes = new HashSet<>();
        
        double val;
        for (Map.Entry<Integer, Double> entry: a.entrySet()) {
            int key = entry.getKey();
            if (b.containsKey(key)) {
                val = entry.getValue() - b.get(key);
                ret.put(key, val);
            } else {
                ret.put(key, entry.getValue());
            }
            writtenIndexes.add(key);
        }

        for (Map.Entry<Integer, Double> entry: b.entrySet()) {
            int key = entry.getKey();
            if (!writtenIndexes.contains(key)) {
                val = entry.getValue() + b.get(key);
                ret.put(key, -entry.getValue());
            }
        }

        a.defaultValue -= b.defaultValue;

        return ret;
    }

    public static double distance(Sparse a, Sparse b) throws IllegalArgumentException {
        
        int m = a.m;
        int n = b.n;

        if (m*n + 1 > m + n)
            throw new IllegalArgumentException("Not a vector!" + m + "x" + n);

        if (a.m != b.m || a.n != b.n) {
            throw new IllegalArgumentException("Incompatible matrix dimensions: " + a.m + " x " + a.n + " " + b.m + "x" + b.n);
        }

        double alignment = 0.0;
        
        HashSet<Integer> writtenIndexes = new HashSet<>();

        for (Map.Entry<Integer, Double> entry: a.entrySet()) {
            int key = entry.getKey();
            if (b.containsKey(key)) {
                alignment += Math.pow(entry.getValue() - b.get(key), 2);
            } else {
                alignment += Math.pow(entry.getValue(), 2);
            }
            writtenIndexes.add(key);
        }

        for (Map.Entry<Integer, Double> entry: b.entrySet()) {
            int key = entry.getKey();
            if (!writtenIndexes.contains(key)) {
                alignment += Math.pow(entry.getValue(), 2);
            }
        }

        alignment = Math.sqrt(alignment);

        return alignment;
    }

    public static double sum(Sparse a) {
        double sum = 0.0;

        for (Map.Entry<Integer, Double> entry: a.entrySet()) {
            sum += entry.getValue();
        }

        return sum;
    }

    public static void scalarMult(Sparse a, double c) throws IllegalArgumentException {
        
        int m = a.m;
        int n = a.n;
        
        for (Map.Entry<Integer, Double> entry: a.entrySet()) {
            entry.setValue(c * entry.getValue());
        }

        a.defaultValue *= c;
    }

    public static void normalize(Sparse a) {
        
        int m = a.m;
        int n = a.n;

        if (m*n + 1 > m + n)
            throw new IllegalArgumentException("Not a vector!" + m + "x" + n);
        
        double num = 0.0;
        Set<Map.Entry<Integer, Double>> entrySet = a.entrySet();

        int numElements = 0;
        for (Map.Entry<Integer, Double> entry : entrySet) {
            if (norm == Normalizer.EUCLIDEAN) {
                num += Math.pow(entry.getValue(), 2);
            } else if (norm == Normalizer.MANHATTAN) {
                num += Math.abs(entry.getValue());
            }
            numElements++;
        }

        num += (m * n - numElements) * Math.pow(a.defaultValue, 2.0);

        if (norm == Normalizer.EUCLIDEAN) {
            num = Math.sqrt(num);
        }
        
        for (Map.Entry<Integer, Double> entry : entrySet) {
            entry.setValue(entry.getValue() / num);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(); 

        for (Map.Entry<Integer, Double> entry : entrySet()) {
            sb.append("(" + entry.getKey() + ": " + entry.getValue() + "), ");
        }
        sb.append("\nDefault value: " + defaultValue);

        return sb.toString();
    }
}