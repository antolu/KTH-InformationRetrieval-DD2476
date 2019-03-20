/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class KGramIndex {

    /**
     * Mapping from term ids to actual term strings
     */
    HashMap<Integer, String> id2term = new HashMap<Integer, String>();

    /**
     * Mapping from term strings to term ids
     */
    HashMap<String, Integer> term2id = new HashMap<String, Integer>();

    /**
     * Index from k-grams to list of term ids that contain the k-gram
     */
    HashMap<String, List<KGramPostingsEntry>> index = new HashMap<String, List<KGramPostingsEntry>>();

    HashMap<String, Integer> numberOfKgrams = new HashMap<>();

    /**
     * The ID of the last processed term
     */
    int lastTermID = -1;

    /**
     * Number of symbols to form a K-gram
     */
    static int K;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /**
     * Generate the ID for an unknown term
     */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }


    /**
     * Get intersection of two postings lists
     */
    private List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {

        List<KGramPostingsEntry> intersection = new ArrayList<>();

        Iterator<KGramPostingsEntry> itp1 = p1.iterator();
        Iterator<KGramPostingsEntry> itp2 = p2.iterator();

        KGramPostingsEntry pe1 = itp1.next();
        KGramPostingsEntry pe2 = itp2.next();

        try {
            for (; ; ) {
                if (pe1.tokenID == pe2.tokenID) {
                    intersection.add(new KGramPostingsEntry(pe1.tokenID));

                    pe1 = itp1.next();
                    pe2 = itp2.next();
                } else if (pe1.tokenID < pe2.tokenID) {
                    pe1 = itp1.next();
                } else {
                    pe2 = itp2.next();
                }
            }
        } catch (NoSuchElementException e) {
            // Do nothing
        }

        return intersection;
    }

    static HashSet<String> getKGrams(String token) {
        HashSet<String> list = new HashSet<>();

        if (token.length() < 1) {
            return list;
        } else if (token.length() == K - 2) {
            list.add(token);
            return list;
        }

        list.add("^" + token.substring(0, K - 1));

        for (int i = 0; i < token.length() - (K - 1); i++) {
            list.add(token.substring(i, i + K));
        }

        list.add(token.substring(token.length() - K + 1, token.length()) + "$");

        return list;
    }

    /**
     * Gets all the words that have at least one of the kgrams in the input
     * @param kgrams The kgrams to base the search on
     * @return A HashSet containing the words that contain any of the kgrams
     */
    HashMap<String, MutableInteger> getTokensFromKgrams(Set<String> kgrams) {

        HashMap<String, MutableInteger> tokens = new HashMap<>();

        for (String kgram: kgrams) {
            List<KGramPostingsEntry> list = index.get(kgram);
            if (list == null) continue;

            for (KGramPostingsEntry pe: list) {
                String tkn = id2term.get(pe.tokenID);

                if (tokens.containsKey(tkn))
                    tokens.get(tkn).i++;
                else
                    tokens.put(tkn, new MutableInteger(1));
            }
        }

        return tokens;
    }

    static Pair<String, String> getWildcardKGrams(String token, HashSet<String> list) {

        if (token.length() < 1) {
            return new Pair<String, String>("", "");
        } else if (token.length() == K - 2) {
            list.add(token);
            return new Pair<String, String>("", "");
        }

        String first = token.substring(0, token.indexOf("*"));
        String second = token.substring(token.indexOf("*") + 1, token.length());

        if (first.length() >= K - 1)
            list.add("^" + first.substring(0, K - 1));

        for (int i = 0; i < first.length() - (K - 1); i++) {
            list.add(first.substring(i, i + K));
        }

        for (int i = 0; i < second.length() - (K - 1); i++) {
            list.add(second.substring(i, i + K));
        }

        if (second.length() >= K - 1)
            list.add(second.substring(second.length() - K + 1, second.length()) + "$");


        return new Pair<String, String>(first, second);
    }

    List<String> getWildcards(String token) {
        List<String> wildcards = new ArrayList<>();

        HashSet<String> kgrams = new HashSet<>();
        Pair<String, String> components = getWildcardKGrams(token, kgrams);

        List<KGramPostingsEntry> intersection = null;
        for (String kgram : kgrams) {

            try {
                if (intersection == null) {
                    intersection = getPostings(kgram);
                } else {
                    intersection = intersect(intersection, getPostings(kgram));
                }
            } catch (NullPointerException e) { // One kgram is missing
                intersection = null;
                break;
            }
        }

        /* No results for this token */
        if (intersection == null) return wildcards;

        /* Postfilering, check if token if really matches */
        for (KGramPostingsEntry match : intersection) {
            String s = id2term.get(match.tokenID);

            if (s.startsWith(components.first) && s.endsWith(components.second))
                wildcards.add(s);
        }

        return wildcards;
}

    /**
     * Inserts all k-grams from a token into the index.
     */
    public void insert(String token) {

        if (term2id.containsKey(token)) return;

        int tokenID = generateTermID();

        term2id.put(token, tokenID);
        id2term.put(tokenID, token);

        HashSet<String> kgrams = getKGrams(token);

        for (String kgram : kgrams) {
            if (index.containsKey(kgram)) {
                index.get(kgram).add(new KGramPostingsEntry(tokenID));
            } else {
                ArrayList<KGramPostingsEntry> list = new ArrayList<>();
                list.add(new KGramPostingsEntry(tokenID));
                index.put(kgram, list);
            }
        }

        numberOfKgrams.put(token, kgrams.size());
    }

    /**
     * Get postings for the given k-gram
     */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        return index.get(kgram);
    }

    /**
     * Get id of a term
     */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /**
     * Get a term by the given id
     */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    private static HashMap<String, String> decodeArgs(String[] args) {
        HashMap<String, String> decodedArgs = new HashMap<String, String>();
        int i = 0, j = 0;
        while (i < args.length) {
            if ("-p".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            } else if ("-f".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("file", args[i++]);
                }
            } else if ("-k".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("k", args[i++]);
                }
            } else if ("-kg".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("kgram", args[i++]);
                }
            } else {
                System.err.println("Unknown option: " + args[i]);
                break;
            }
        }
        return decodedArgs;
    }

    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String, String> args = decodeArgs(arguments);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        KGramIndex kgIndex = new KGramIndex(k);

        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
        Tokenizer tok = new Tokenizer(reader, true, false, true, args.get("patterns_file"));
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            kgIndex.insert(token);
        }

//        String tkn = "love";
//        HashSet<String> kgrams = kgIndex.getKGrams(tkn);
//
//        for (String kgram: kgrams) {
//            System.out.println(kgram);
//        }

        String[] kgrams = args.get("kgram").split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != k) {
                System.err.println("Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
    }

    static class Pair<K, V> {
        K first;
        V second;

        public Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }
    }

    static class MutableInteger {
        int i;

        public MutableInteger(int i) {
            this.i = i;
        }
    }
}
