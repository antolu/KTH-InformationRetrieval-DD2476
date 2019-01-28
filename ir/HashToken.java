package ir;

import java.lang.Math;

public class HashToken{
    public static int hash(String s) {
        return Math.abs(s.hashCode()) % (int) PersistentHashedIndex.TABLESIZE;
    }

    public static int reverseHash(String s) {
        int h = 0;
        if (h == 0 && s.length() > 0) {
            char val[] = s.toCharArray();

            for (int i = s.length()-1; i > 0; i--) {
                h = 31 * h + val[i];
            }
        }
        return h;
    }
}