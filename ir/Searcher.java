/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.*;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    private static final String DATADIR = "data";
    HITSRanker HITSRanker;

    private static final Normalizer norm = Normalizer.EUCLIDEAN;

    /** How much the tfidf weigths during ranked query */
    private static final double RANK_WEIGHT = 0.1;

    /** Constructor */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;

        HITSRanker = new HITSRanker(DATADIR + "/linksDavis.txt", DATADIR + "/davisTitles.txt", index);
    }

    /**
     * Searches the index for postings matching the query.
     * 
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType) {

        if (query.queryterm.size() == 0)
            return null;

        try {
            if (!query.containsWildcards()) {
                if (queryType == QueryType.RANKED_QUERY) {
                    if (rankingType == RankingType.TF_IDF) {
                        return getTfidfQuery(query);
                    } else if (rankingType == RankingType.COMBINATION) {
                        return getCombinedQuery(query);
                    } else if (rankingType == RankingType.PAGERANK) {
                        return getPagerankQuery(query);
                    } else if (rankingType == RankingType.HITS) {
                        return getHITSQuery(query);
                    }
                }

                // Not sufficient number of words for other query
                if (query.queryterm.size() < 2) {
                    return index.getPostings(query.queryterm.get(0).term);
                }

                if (queryType == QueryType.INTERSECTION_QUERY) {
                    return getIntersectionQuery(query, null);
                } else if (queryType == QueryType.PHRASE_QUERY) {
                    return getPhraseQuery(query);
                } else {

                    String token = query.queryterm.get(0).term;

                    PostingsList list = index.getPostings(token);

                    return list;
                }

            } else {

                List<Query> queries = query.getWildcardQuery(kgIndex);

                PostingsList res = new PostingsList();
                if (queryType == QueryType.RANKED_QUERY) {
                    return getRankedWildcardQuery(queries, rankingType);
                }

                // Not sufficient number of words for other query
                if (query.queryterm.size() < 2) {
                    for (Query q: queries)
                        res.addAll(index.getPostings(q.queryterm.get(0).term));
                    return res;
                }

                if (queryType == QueryType.INTERSECTION_QUERY) {
                    for (Query q: queries)
                        res.addAll(getIntersectionQuery(q, null));
                } else if (queryType == QueryType.PHRASE_QUERY) {
                    for (Query q: queries)
                        res.addAll(getPhraseQuery(q));
                } else {

                    String token = query.queryterm.get(0).term;

                    PostingsList list = index.getPostings(token);

                    return list;
                }

                return removeDuplicates(res);
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return null;
        }

    }

    /**
     * Removes duplicates from a PostingsList
     * @param in The PostingsList which to remove duplicates from
     * @return A PostingsList without duplicates
     */
    private PostingsList removeDuplicates(PostingsList in) {
        HashSet<Integer> check = new HashSet<>();

        PostingsList out = new PostingsList();
        for (PostingsEntry pe: in) {
            if (check.contains(pe.docID)) continue;
            else {
                check.add(pe.docID);
                out.add(pe);
            }
        }

        return out;
    }

    private void mergeScores(HashMap<Integer, Integer> check, PostingsList in1, PostingsList in2) {
        /* <docID, index> */

        for (PostingsEntry pe: in2) {
            if (check.containsKey(pe.docID)) {
                in1.get(check.get(pe.docID)).score += pe.score;
            }
            else {
                check.put(pe.docID, in1.size());
                in1.add(pe);
            }
        }
    }

    private PostingsList getRankedWildcardQuery(List<Query> queries, RankingType rankingType) {

        HashMap<Integer, Integer> check = new HashMap<>();
        PostingsList out = new PostingsList();

        PostingsList next = null;

        for (Query q: queries) {
            if (rankingType == RankingType.TF_IDF) {
                next = getTfidfQuery(q);
            } else if (rankingType == RankingType.COMBINATION) {
                next = getCombinedQuery(q);
            } else if (rankingType == RankingType.PAGERANK) {
                next = getPagerankQuery(q);
            } else if (rankingType == RankingType.HITS) {
                next = getHITSQuery(q);
            }

            if (next != null)
                mergeScores(check, out, next);
        }

        Collections.sort(out);

        return out;
    }

    private PostingsList getTfidfQuery(Query query) {

        /** Build query vector */
        ArrayList<Double> q = new ArrayList<>();

        HashMap<String, Integer> tkns = new HashMap<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            if (!tkns.containsKey(query.queryterm.get(i).term))
                q.add(query.queryterm.get(i).weight);
            else {
                int idx = tkns.get(query.queryterm.get(i).term);
                q.set(idx, q.get(idx) + query.queryterm.get(i).weight);
            }
        }

        /** Normalize q */
        // int val = 0;
        // for (double d: q) {
        //     if (norm == Normalizer.EUCLIDEAN)
        //         val += Math.pow(d, 2);
        //     else if (norm == Normalizer.MANHATTAN)
        //         val += d;
        // }

        // for (int i = 0; i < q.size(); i++) {
        //     double denominator = 1.0;
        //     if (norm == Normalizer.EUCLIDEAN)
        //         denominator = Math.sqrt(val);
        //     else if (norm == Normalizer.MANHATTAN)
        //         denominator = val;
        //     q.set(i, q.get(i) / denominator);
        // }


        ArrayList<TokenIndexData> postingsLists = getPostingsLists(query);
        HashMap<Integer, PostingsEntry> scores = new HashMap<>();
        HashMap<Integer, Double> denom = new HashMap<>();

        /** <docID, index> */
        int i = 0;
        for (TokenIndexData pt: postingsLists) {
            PostingsList pl = pt.postingsList;
            // int termID = Index.tokenIndex.get(postingsLists.get(0).token);
            for (PostingsEntry pe: pl) {
                // double tf = 1.0 + Math.log10(pe.getOccurences());
                double tfidf = tfidf(pe, pl);

                double score = q.get(i) * tfidf;

                if (!scores.containsKey(pe.docID)) {
                    scores.put(pe.docID, new PostingsEntry(pe.docID, score));
                }
                else {
                    scores.get(pe.docID).score += score;
                }
            }
            i++;
        }

        PostingsList results = new PostingsList();

        /** Normalize score */
        for (int docID: scores.keySet()) {
            PostingsEntry pe = scores.get(docID);

            pe.score /= Index.docLengths.get(pe.docID);

            results.add(pe);
        }
        
        Collections.sort(results);

        return results;
    }

    private double tfidf(PostingsEntry pe, PostingsList pl) {
        double tf = pe.getOccurences();
        double idf = Math.log(Index.docNames.size() / pl.size());

        double tfidf = tf * idf;
        return tfidf;
    }

    private PostingsList getPagerankQuery(Query query) {
        ArrayList<TokenIndexData> postingsLists = getPostingsLists(query);

        HashSet<Integer> savedDocIDs = new HashSet<>();

        PostingsList results = new PostingsList();

        for (TokenIndexData pt: postingsLists) {
            PostingsList pl = pt.postingsList;
            for (PostingsEntry pe: pl) {
                if (!savedDocIDs.contains(pe.docID)) {
                    String docName = Index.docNames.get(pe.docID);
                    String strippedDocName = docName.substring(docName.indexOf("/")+1);
                    try {
                        pe.score = Index.pageranks.get(strippedDocName);
                    } catch (NullPointerException e) {
                        System.err.println(strippedDocName);
                    } 
                    
                    results.add(pe);

                    savedDocIDs.add(pe.docID);
                }
            }
        }

        Collections.sort(results);

        /** Normalize scores */
        double norm = 0.0;
        for (PostingsEntry pe: results) {
            norm += pe.score;
        }

        for (PostingsEntry pe: results) {
            pe.score /= norm;
        }

        return results;
    }

    private PostingsList getCombinedQuery(Query query) {
        PostingsList tfidf = getTfidfQuery(query);

        ArrayList<Double> pagerankScores = new ArrayList<>();
        pagerankScores.ensureCapacity(tfidf.size());

        double tfidfNorm = 0.0;
        double pagerankNorm = 0.0;
        for (PostingsEntry pe: tfidf) {
            tfidfNorm += pe.score;

            String docName = Index.docNames.get(pe.docID);
            String strippedDocName = docName.substring(docName.indexOf("/")+1);
            double pagerankScore = Index.pageranks.get(strippedDocName);
            pagerankScores.add(pagerankScore);
            pagerankNorm += pagerankScore;
        }

        int i = 0;
        for (PostingsEntry pe: tfidf) {
            pe.score = RANK_WEIGHT * pe.score/tfidfNorm + (1.0-RANK_WEIGHT) * pagerankScores.get(i) /pagerankNorm;
            i++;
        }

        Collections.sort(tfidf);

        return tfidf;
    }

    private PostingsList getHITSQuery(Query query) {
        ArrayList<TokenIndexData> postingsLists = getPostingsLists(query);

        HashSet<Integer> savedDocIDs = new HashSet<>();

        PostingsList results = new PostingsList();

        for (TokenIndexData pt: postingsLists) {
            PostingsList pl = pt.postingsList;
            for (PostingsEntry pe: pl) {
                if (!savedDocIDs.contains(pe.docID)) {
                    String docName = Index.docNames.get(pe.docID);
                    results.add(pe);

                    savedDocIDs.add(pe.docID);
                }
            }
        }

        return HITSRanker.rank(results);
    }

    /**
     * Computes the results of a phrase query
     * 
     * @param query The query to process
     * 
     * @return A PostingsList with the results
     */
    private PostingsList getPhraseQuery(Query query) {
        ArrayList<LinkedHashMap<Integer, Integer>> indexes = new ArrayList<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            indexes.add(new LinkedHashMap<Integer, Integer>());
        }
        try {
            getIntersectionQuery(query, indexes);

            ArrayList<PostingsList> orderPostingsList = getLists(query, indexes);

            Collections.reverse(orderPostingsList);

            PostingsList p1 = orderPostingsList.get(orderPostingsList.size() - 1);

            orderPostingsList.remove(orderPostingsList.size() - 1);

            return getRecursivePhrase(p1, orderPostingsList);
        } catch (IllegalArgumentException e) {
            return new PostingsList();
        }
    }

    /**
     * Finds the postings entries corresponding to the docIDs in `postingsList`.
     * 
     * @param query        The tokens to fetch postings entries for
     * @param postingsList A list of docIDs to find postings entries for
     * 
     * @return An ArrayList of PostingsLists with postings entries for each token in
     *         `query` and docID in `postingsList`.
     */
    private ArrayList<PostingsList> getLists(Query query, ArrayList<LinkedHashMap<Integer, Integer>> indexes) throws IllegalArgumentException {

        /** Allocate return variable */
        ArrayList<PostingsList> ret = new ArrayList<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            ret.add(new PostingsList());
        }

        /** Loop over tokens */
        for (int i = 0; i < query.queryterm.size(); i++) {
            LinkedHashMap<Integer, Integer> tokenIndexes = indexes.get(i);
            PostingsList list = ret.get(i);
            PostingsList fullList = index.getPostings(query.queryterm.get(i).term);

            if (tokenIndexes.isEmpty())
                throw new IllegalArgumentException("Token" + query.queryterm.get(i).term + " does not match a phrase query.");

            for (Map.Entry<Integer, Integer> entry : tokenIndexes.entrySet()) {
                list.add(fullList.get(entry.getValue()));
            }
        }
        return ret;
    }

    /**
     * A recursive implementation of phrase queries
     * 
     * @param p1            The intersection of queries from previous level
     * @param p2            The new list of documents to intersect with the first
     *                      one
     * @param postingsLists The remaining tokens to intersect with
     * @return A PostingsList with matching queries
     */
    private PostingsList getRecursivePhrase(PostingsList p1, ArrayList<PostingsList> postingsLists) {

        /** Increment */
        PostingsList p2 = postingsLists.get(postingsLists.size()-1);
        postingsLists.remove(postingsLists.size()-1);

        Iterator<PostingsEntry> itp1 = p1.iterator();
        Iterator<PostingsEntry> itp2 = p2.iterator();

        PostingsEntry docID1 = (PostingsEntry) itp1.next();
        PostingsEntry docID2 = (PostingsEntry) itp2.next();

        ArrayList<Integer> pList1;
        ArrayList<Integer> pList2;

        Iterator<Integer> itps1;
        Iterator<Integer> itps2;

        PostingsList intersection = new PostingsList();

        try {
            for (;;) {
                // Same documents
                if (docID1.docID == docID2.docID) {

                    /* Get positional indexes */
                    pList1 = docID1.getPositionList();
                    pList2 = docID2.getPositionList();
                    Collections.sort(pList1);
                    Collections.sort(pList2);

                    itps1 = pList1.iterator();
                    itps2 = pList2.iterator();

                    int ps1 = itps1.next();
                    int ps2 = itps2.next();

                    /* Compare indexes */
                    try {
                        for (;;) {
                            if (ps2 < ps1) {
                                ps2 = itps2.next();
                            } else if (ps2 == ps1 + 1) {
                                if (intersection.isEmpty() || !(intersection.get(intersection.size()-1).docID == docID1.docID)) {
                                    intersection.add(new PostingsEntry(docID1.docID, ps2));
                                } else {
                                    intersection.get(intersection.size()-1).addPosition(ps2);
                                }

                                ps1 = itps1.next();
                                ps2 = itps2.next();

                                if (postingsLists.isEmpty())
                                    break;
                            } else {
                                ps1 = itps1.next();
                            }
                        }
                    } catch (NoSuchElementException ex) {
                        // Do nothing
                    }

                    // int oldDocID = docID1.docID;

                    docID1 = (PostingsEntry) itp1.next();
                    // if (oldDocID != docID1.docID)
                        docID2 = (PostingsEntry) itp2.next();
                } else if (docID1.docID < docID2.docID) {
                    docID1 = (PostingsEntry) itp1.next();
                } else {
                    docID2 = (PostingsEntry) itp2.next();
                }
            }
        } catch (NoSuchElementException e) {
            // Do nothing
        }

        /** If no matches */
        if (intersection.isEmpty())
            return intersection;

        /** Recursion end */
        if (postingsLists.isEmpty())
            return intersection;

        return getRecursivePhrase(intersection, postingsLists);
    }

    /**
     * Retrieves the intersection query for the given query
     * 
     * @param query The query containing tokens
     * @return A PostingsList with the matches
     * @throws IllegalArgumentException When a token in the query does not exist in
     *                                  the index.
     */
    private PostingsList getIntersectionQuery(Query query, ArrayList<LinkedHashMap<Integer, Integer>> indexes) throws IllegalArgumentException {

        ArrayList<TokenIndexData> postingsLists = getPostingsLists(query);
        PostingsList intersection;

        if (indexes != null) {
            for (int i = 0; i < postingsLists.size(); i++) {
                postingsLists.get(i).indexes = indexes.get(i);
            }
        }

        Collections.sort(postingsLists, Collections.reverseOrder());

        /** Iterate all queries */
        PostingsList p1;
        PostingsList p2;

        intersection = postingsLists.get(0).postingsList;
        HashMap<Integer, Integer> intersectionIndexes = new HashMap<>();

        int j;
        int k;

        for (int i = 1; i < postingsLists.size(); i++) {
            j = 0;
            k = 0;

            p1 = intersection;
            p2 = postingsLists.get(i).postingsList;
            intersection = new PostingsList();

            PostingsEntry docID1 = p1.get(j);
            PostingsEntry docID2 = p2.get(k);

            try {
                for (;;) {
                    if (docID1.docID == docID2.docID) {
                        intersection.add(docID1);
                        if (indexes != null) {
                            intersectionIndexes.put(docID1.docID, j);
                            if (i == 1) {
                                postingsLists.get(i-1).indexes.put(docID1.docID, j);
                            }
                            postingsLists.get(i).indexes.put(docID1.docID, k);
                        }
                        docID1 = p1.get(++j);
                        docID2 = p2.get(++k);
                    } else if (docID1.docID < docID2.docID) {
                        docID1 = p1.get(++j);
                    } else {
                        docID2 = p2.get(++k);
                    }
                }
            } catch (Exception e) {
                // Do nothing
            }

            if (indexes != null) {
                /** Remove non-relevant indexes */
                for (int l = 0; l <= i; l++) {
                    HashMap<Integer, Integer> map = postingsLists.get(i).indexes;
                    for (int docID : map.keySet()) {
                        if (!intersectionIndexes.containsKey(docID)) {
                            map.remove(docID);
                        }
                    }
                }
                intersectionIndexes.clear();
            }
        }
        return intersection;
    }

    /**
     * Returns a list of PostingsLists for the given query
     * 
     * @param query The query
     * 
     * @return An ArrayList of PostingsLists
     * 
     * @throws IllegalArgumentException When a token does not exist in the index
     */
    private ArrayList<TokenIndexData> getPostingsLists(Query query) throws IllegalArgumentException {
        ArrayList<TokenIndexData> postingsLists = new ArrayList<>();

        // Retrieve all postingsLists for query tokens
        for (int i = 0; i < query.queryterm.size(); i++) {
            String token = query.queryterm.get(i).term;

            PostingsList tokenList = index.getPostings(token);

            // If one term does not exist, whole intersection query fails
            if (tokenList != null)
                postingsLists.add(new TokenIndexData(token, i, tokenList));
             else
                 throw new IllegalArgumentException("Token " + token + " has no matches");
        }

        return postingsLists;
    }
}