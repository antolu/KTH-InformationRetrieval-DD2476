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
    static int editDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                }
                else if (j == 0) {
                    dp[i][j] = i;
                }
                else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] // substitute
                                    + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)), Math.min(
                            dp[i - 1][j] + 1, // insert
                            dp[i][j - 1] + 1)); // delete
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    private static int costOfSubstitution(char a, char b) {
        if (a == b)
            return 0;
        else
            return 2;
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

            /* Check which words pass the jaccard threshold */
            for (String kgramToken: words.keySet()) {
                double jScore = jaccard(kgrams.size(), kgIndex.numberOfKgrams.get(kgramToken), words.get(kgramToken).i);
                if (jScore >= JACCARD_THRESHOLD) {
                    passJaccard.add(new KGramStat(kgramToken, jScore));
                }
            }

            /* Check which words pass edit distance threshold */
            ArrayList<KGramStat> results = new ArrayList<>();
            for (KGramStat kstat: passJaccard) {
                int editDistance = editDistance(qt.term, kstat.token);
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

        /* Only for multiword queries */
        List<KGramStat> sortedResults = mergeCorrections(unsortedResults, limit);

        /* move results to array */
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

        /* Merge scores for several tokens and construct all queries */
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
