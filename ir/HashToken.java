package ir;

import java.lang.Math;

public class HashToken{
    public static int hash(String s) {
        return Math.abs(s.hashCode()) % (int) PersistentHashedIndex.TABLESIZE;
    }
}