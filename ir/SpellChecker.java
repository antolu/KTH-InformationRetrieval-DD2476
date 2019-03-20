/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.util.*;


public class SpellChecker {
    /** The regular inverted index to be used by the spell checker */
    Index index;

    /** K-gram index to be used by the spell checker */
    KGramIndex kgIndex;

    /** The auxiliary class for containing the value of your ranking function for a token */
    class KGramStat implements Comparable {
        double score;
        String token;

        KGramStat(String token, double score) {
            this.token = token;
            this.score = score;
        }

        public String getToken() {
            return token;
        }

        public int compareTo(Object other) {
            if (this.score == ((KGramStat)other).score) return 0;
            return this.score < ((KGramStat)other).score ? -1 : 1;
        }

        public String toString() {
            return token + ";" + score;
        }
    }

    /**
     * The threshold for Jaccard coefficient; a candidate spelling
     * correction should pass the threshold in order to be accepted
     */
    private static final double JACCARD_THRESHOLD = 0.4;

    /**
      * The threshold for edit distance for a candidate spelling
      * correction to be accepted.
      */
    private static final int MAX_EDIT_DISTANCE = 2;


    public SpellChecker(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     *  Computes the Jaccard coefficient for two sets A and B, where the size of set A is 
     *  <code>szA</code>, the size of set B is <code>szB</code> and the intersection 
     *  of the two sets contains <code>intersection</code> elements.
     */
    private double jaccard(int szA, int szB, int intersection) {
        return intersection * 1.0 / (szA + szB - intersection);
    }

    /**
     * Computing Levenshtein edit distance using dynamic programming.
     * Allowed operations are:
     *      => insert (cost 1)
     *      => delete (cost 1)
     *      => substitute (cost 2)
     */
    private int editDistance(String s1, String s2) {

        int[][] m = new int[s1.length()][s2.length()];

        for (int i = 1; i < s1.length(); i++) {
            m[i][0] = i;
        }
        for (int i = 1; i < s2.length(); i++) {
            m[0][i] = i;
        }
        for (int i = 1; i < s1.length(); i++) {
            for (int j = 1; j < s2.length(); j++) {
                int n = m[i-1][j-1];
                if (s1.charAt(i) != s2.charAt(j)) n += 2;

                n = Math.min(n, m[i-1][j] + 1);
                n = Math.min(n, m[i][j-1] + 1);
                m[i][j] = n;
            }
        }

        return m[s1.length() - 1][s2.length() - 1];
    }

    private int getNoKgrams(String s, int K) {
        return (int) Math.ceil((s.length() + 1.0) / K);
    }

    /**
     *  Checks spelling of all terms in <code>query</code> and returns up to
     *  <code>limit</code> ranked suggestions for spelling correction.
     */
    public String[] check(Query query, int limit) {
        String[] finalResults;

        List<List<KGramStat>> unsortedResults = new ArrayList<>();

        for (Query.QueryTerm qt: query.queryterm) {
            Set<String> kgrams = KGramIndex.getKGrams(qt.term);

            HashMap<String, KGramIndex.MutableInteger> words = kgIndex.getTokensFromKgrams(kgrams);
            List<KGramStat> passJaccard = new ArrayList<>();

            for (String kgramToken: words.keySet()) {
                double jScore = jaccard(getNoKgrams(qt.term, kgIndex.K), kgIndex.numberOfKgrams.get(kgramToken), words.get(kgramToken).i);
                if (jScore > JACCARD_THRESHOLD) {
                    passJaccard.add(new KGramStat(kgramToken, jScore));
                }
            }

            ArrayList<KGramStat> results = new ArrayList<>();
            for (KGramStat kstat: passJaccard) {
                int editDistance = editDistance(qt.term, kstat.token);
                if (editDistance <= MAX_EDIT_DISTANCE) {
                    kstat.score += editDistance / 2.0;
                    results.add(kstat);
                }
            }

            Collections.sort(results, Collections.reverseOrder());
            unsortedResults.add(results);
        }

        List<KGramStat> sortedResults = mergeCorrections(unsortedResults, limit);

        finalResults = new String[limit];

        for (int i = 0; i < limit || i < sortedResults.size(); i++) {
            finalResults[i] = sortedResults.get(i).token;
        }

        return finalResults;
    }

    /**
     *  Merging ranked candidate spelling corrections for all query terms available in
     *  <code>qCorrections</code> into one final merging of query phrases. Returns up
     *  to <code>limit</code> corrected phrases.
     */
    private List<KGramStat> mergeCorrections(List<List<KGramStat>> qCorrections, int limit) {
        List<KGramStat> results = new ArrayList<>();

        for (List<KGramStat> tknList: qCorrections) {
            results.addAll(tknList);
        }

        Collections.sort(results, Collections.reverseOrder());

        if (results.size() > limit)
            return results.subList(0, limit);
        else
            return results;
    }
}
