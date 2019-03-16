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

    /**
     * The ID of the last processed term
     */
    int lastTermID = -1;

    /**
     * Number of symbols to form a K-gram
     */
    int K = 3;

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

    private HashSet<String> getKGrams(String token) {
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
}
