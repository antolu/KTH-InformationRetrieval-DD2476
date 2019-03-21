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
                int substitute = m[i-1][j-1];
                if (s1.charAt(i) != s2.charAt(j))
                    substitute += 2;
                int insert = m[i-1][j] + 1;
                int delete = m[i][j-1] + 1;

                int val = Math.min(substitute, insert);
                val = Math.min(val, delete);
                m[i][j] = val;
            }
        }

        return m[m.length - 1][m[0].length - 1];
    }

    private static int distance(String s1, String s2, int i, int j) {
        if (j == s2.length()) {
            return s1.length() - i;
        }
        if (i == s1.length()) {
            return s2.length() - j;
        }
        if (s1.charAt(i) == s2.charAt(j))
            return distance(s1, s2, i + 1, j + 1);
        int rep = distance(s1, s2, i + 1, j + 1) + 2;
        int del = distance(s1, s2, i, j + 1) + 1;
        int ins = distance(s1, s2, i + 1, j) + 1;
        return Math.min(del, Math.min(ins, rep));
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
                double jScore = jaccard(kgrams.size(), kgIndex.numberOfKgrams.get(kgramToken), words.get(kgramToken).i);
                if (jScore >= JACCARD_THRESHOLD) {
                    passJaccard.add(new KGramStat(kgramToken, jScore));
                }
            }

            ArrayList<KGramStat> results = new ArrayList<>();
            for (KGramStat kstat: passJaccard) {
                // int editDistance = editDistance(qt.term, kstat.token);
                int editDistance = distance(qt.term, kstat.token, 0, 0);
                if (editDistance <= MAX_EDIT_DISTANCE) {
                    results.add(kstat);
                }
            }

            /* Normalize */
            Collections.sort(results, Collections.reverseOrder());
            double denom = 0.0;
            for (KGramStat kstat: results){
                denom += kstat.score;
            }
            for (KGramStat kstat: results){
                kstat.score /= denom;
            }

            /* Get number of documents and normalize combine scores */
            denom = 0.0;
            int i = 0;
            double[] noDocsScores = new double[results.size()];
            for (KGramStat kstat: results){
                double score = index.getPostings(kstat.token).size();
                noDocsScores[i++] = score;
                denom += score;
            }

            i = 0;
            for (KGramStat kstat: results) {
                kstat.score += noDocsScores[i++] / denom;
            }

            unsortedResults.add(results);
        }

        List<KGramStat> sortedResults = mergeCorrections(unsortedResults, limit);

        int resultSize = Math.min(sortedResults.size(), limit);

        finalResults = new String[resultSize];

        for (int i = 0; i < resultSize; i++) {
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
        List<KGramStat> newResults;

        for (List<KGramStat> tknList: qCorrections) {
            newResults = new ArrayList<>();

            if (!results.isEmpty())
                for (KGramStat kstat: tknList) {
                    for (KGramStat kstatRes: results) {
                        KGramStat combined = new KGramStat(kstat.token + " " + kstatRes.token, kstat.score +  kstatRes.score);
                        newResults.add(combined);
                    }
                }
            else {
                newResults.addAll(tknList);
            }
            results = newResults;
        }

        Collections.sort(results, Collections.reverseOrder());

        if (results.size() > limit)
            return results.subList(0, limit);
        else
            return results;
    }
}
