package ir;

import java.lang.Math;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.ArrayList;

public class Utils{

    private static MessageDigest messageDigest;

    public static void initialize() {
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static int hash(String s) {
        return Math.abs(improvedHash(s)) % (int) PersistentHashedIndex.TABLESIZE;
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

    public static int improvedHash(String s) {
        messageDigest.update(s.getBytes());
        int hash = Arrays.hashCode(messageDigest.digest());
        messageDigest.reset();
        return hash;
    }

    public static ArrayList<String> splitByDelim(String s, String delim) {
        s += delim;
        ArrayList<String> split = new ArrayList<>();

        int off = 0;
        int next = 0;
        while ((next = s.indexOf(delim, off)) != -1) {
            split.add(s.substring(off, next));
            off = next + 1;
        }

        return split;
    }
}