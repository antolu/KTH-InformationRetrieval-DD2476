package ir;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Sparse extends HashMap<Integer, Double> {

    private static final Normalizer norm = Normalizer.EUCLIDEAN;

    public double score = 0.0;
    public int docID = -1;

    public Sparse() {

    }

    public Sparse(int docID) {
        this.docID = docID;
    }

    public double cosineSimilarity(Sparse other) {
        
        double nom = 0.0;
        double denom = 0.0;

        for (int ID : this.keySet()) {
            if (other.containsKey(ID)) {
                nom += this.get(ID) * other.get(ID);
            }
        }

        /** Denominator */
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (Map.Entry<Integer,Double> val : this.entrySet()) {
            norm1 += Math.pow(val.getValue(), 2);
        }
        norm1 = Math.sqrt(norm1);

        for (Map.Entry<Integer,Double> val : other.entrySet()) {
            norm2 += Math.pow(val.getValue(), 2);
        }
        norm2 = Math.sqrt(norm2);
        
        denom = norm1 * norm2;

        return nom / denom;
    }

    public void normalize() {
        double num = 0.0;
        Set<Map.Entry<Integer, Double>> entrySet = entrySet();

        for (Map.Entry<Integer, Double> entry : entrySet) {
            if (norm == Normalizer.EUCLIDEAN) {
                num += Math.pow(entry.getValue(), 2);
            } else if (norm == Normalizer.MANHATTAN) {
                num += Math.abs(entry.getValue());
            }
        }

        if (norm == Normalizer.EUCLIDEAN) {
            num = Math.sqrt(num);
        }
        
        for (Map.Entry<Integer, Double> entry : entrySet) {
            entry.setValue(entry.getValue() / num);
        }
    }
}