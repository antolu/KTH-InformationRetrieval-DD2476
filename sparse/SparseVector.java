package sparse;

import java.util.HashMap;

/**
 * Implementation of a sparse vector using a hashmap
 */
public class SparseVector extends HashMap<Integer, Double> {

    public int m;

    public SparseVector(int m) {
        this.m = m;
    }
}