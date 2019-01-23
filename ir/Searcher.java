/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    /** Constructor */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
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
            // Not sufficient number of words for other query
            if (query.queryterm.size() < 2) {
                return index.getPostings(query.queryterm.get(0).term);
            }

            if (queryType == QueryType.INTERSECTION_QUERY) {
                return getIntersectionQuery(query);
            } else if (queryType == QueryType.PHRASE_QUERY) {
                return getPhraseQuery(query);
            } else {

                String token = query.queryterm.get(0).term;

                PostingsList list = index.getPostings(token);

                return list;
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return null;
        }

    }

    private PostingsList getPhraseQuery(Query query) {
        ArrayList<PostingsList> postingsLists = getPostingsLists(query);

        // Collections.sort(postingsLists);

        HashMap<Integer, HashMap<Integer, Integer>> posIntersect = new HashMap<>();
        ArrayList<PostingsEntry> posKeys = new ArrayList<>();

        PostingsList p1;
        PostingsList p2;

        /* Get all biwords */
        for (int i = 1; i < postingsLists.size(); i++) {
            p1 = postingsLists.get(i - 1);
            p2 = postingsLists.get(i);

            getPositionalIntersect(posIntersect, posKeys, p1, p2);
        }

        /* Find all matching queries */
        PostingsList queryAnswers = new PostingsList();

        for (PostingsEntry doc : posKeys) {

            HashMap<Integer, Integer> mappedTuples = posIntersect.get(doc.docID);

            if (query.queryterm.size() == 2 && mappedTuples.size() >= 1) {
                queryAnswers.add(doc);
                continue;
            } else if (mappedTuples.size() < query.queryterm.size() - 1)
                continue; // Not enough tuples for phrase query

            int pos1;
            int pos2;
            int counter;

            loop: for (int key : mappedTuples.keySet()) {
                pos1 = key;
                pos2 = mappedTuples.get(pos1);
                counter = 0;

                for (;;) {
                    if (mappedTuples.containsKey(pos2)) {
                        pos1 = pos2;
                        pos2 = mappedTuples.get(pos2);
                        counter++;
                    } else
                        break;
                }

                if (counter == query.queryterm.size() - 2) {
                    queryAnswers.add(doc);
                    break loop; // Match found
                }
            }
        }

        return queryAnswers;
    }

    private PostingsList getIntersectionQuery(Query query) throws IllegalArgumentException {

        ArrayList<PostingsList> postingsLists = getPostingsLists(query);
        PostingsList intersection;

        Collections.sort(postingsLists);

        /** Iterate all queries */
        PostingsList p1;
        PostingsList p2;

        intersection = postingsLists.get(0);

        for (int i = 1; i < postingsLists.size(); i++) {
            p1 = intersection;
            p2 = postingsLists.get(i);
            intersection = new PostingsList();

            Iterator<PostingsEntry> itp1 = p1.iterator();
            Iterator<PostingsEntry> itp2 = p2.iterator();

            PostingsEntry docID1 = (PostingsEntry) itp1.next();
            PostingsEntry docID2 = (PostingsEntry) itp2.next();

            try {
                for (;;) {
                    if (docID1.docID == docID2.docID) {
                        intersection.add(docID1);
                        docID1 = (PostingsEntry) itp1.next();
                        docID2 = (PostingsEntry) itp2.next();
                    } else if (docID1.docID < docID2.docID) {
                        docID1 = (PostingsEntry) itp1.next();
                    } else {
                        docID2 = (PostingsEntry) itp2.next();
                    }
                }
            } catch (Exception e) {
                // Do nothing
            }
        }
        return intersection;
    }

    private void getPositionalIntersect(HashMap<Integer, HashMap<Integer, Integer>> posIntersect, ArrayList<PostingsEntry> posKeys, PostingsList p1, PostingsList p2) throws IllegalArgumentException {
        Iterator<PostingsEntry> itp1 = p1.iterator();
        Iterator<PostingsEntry> itp2 = p2.iterator();

        PostingsEntry docID1 = (PostingsEntry) itp1.next();
        PostingsEntry docID2 = (PostingsEntry) itp2.next();

        ArrayList<Integer> pList1;
        ArrayList<Integer> pList2;

        Iterator<Integer> itps1;
        Iterator<Integer> itps2;

        try {
            for (;;) {
                // Same documents
                if (docID1.docID == docID2.docID) {

                    /* Get positional indexes */
                    pList1 = docID1.getPositionList();
                    pList2 = docID2.getPositionList();

                    // Size discrepancy too large
                    if (!(pList2.get(pList2.size() - 1) < pList1.get(0)))
                    {
                        itps1 = pList1.iterator();
                        itps2 = pList2.iterator();

                        int ps1 = itps1.next();
                        int ps2 = itps2.next();

                        /* Compare indexes */
                        try {
                            for (;;) {
                                if (ps2 < ps1) {
                                    ps2 = itps2.next();
                                } 
                                else if (ps2 == ps1 + 1) {
                                    if (!posIntersect.containsKey(docID1.docID)) {
                                        HashMap<Integer, Integer> temp = new HashMap<>();
                                        posIntersect.put(docID1.docID, temp);
                                        posKeys.add(docID1);
                                    }
                                    posIntersect.get(docID1.docID).put(ps1, ps2);

                                    ps1 = itps1.next();
                                    ps2 = itps2.next();
                                }
                                else {
                                    ps1 = itps1.next();
                                }
                            }
                        } catch (NoSuchElementException ex) {
                            // Do nothinf
                        }
                    }

                    docID1 = (PostingsEntry) itp1.next();
                    docID2 = (PostingsEntry) itp2.next();
                } else if (docID1.docID < docID2.docID) {
                    docID1 = (PostingsEntry) itp1.next();
                } else {
                    docID2 = (PostingsEntry) itp2.next();
                }
            }
        } catch (Exception e) {
            // Do nothing
        }
    }

    /**
     * Returns a list of PostingsLists for the given query
     * 
     * @param query The query
     * 
     * @return An ArrayList of PostingsLists
     */
    private ArrayList<PostingsList> getPostingsLists(Query query) throws IllegalArgumentException {
        ArrayList<PostingsList> postingsLists = new ArrayList<>();

        // Retrive all postingsLists for query tokens
        for (Query.QueryTerm q : query.queryterm) {
            String token = q.term;

            PostingsList tokenList = index.getPostings(token);

            // If one term does not exist, whole intersection query fails
            if (tokenList != null)
                postingsLists.add(tokenList);
            else
                throw new IllegalArgumentException("Token " + token + " has no matches");
        }

        return postingsLists;
    }
}