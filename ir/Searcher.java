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
import java.util.HashSet;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    private static HashSet<Integer> commonIndices = new HashSet<>();

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
        PostingsList postingsLists = getIntersectionQuery(query);

        ArrayList<PostingsList> orderPostingsList = getOrderedPostingsLists(query, postingsLists);

        // Collections.sort(postingsLists);
        Collections.reverse(orderPostingsList);

        System.out.println(orderPostingsList.size());
        System.out.println(orderPostingsList.get(0).size());


        PostingsList p1 = orderPostingsList.get(orderPostingsList.size() - 1);

        orderPostingsList.remove(orderPostingsList.size() - 1);

        return getRecursivePhrase(p1, orderPostingsList);
    }

    private ArrayList<PostingsList> getOrderedPostingsLists(Query query, PostingsList postingsList) {

        ArrayList<PostingsList> ret = new ArrayList<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            ret.add(new PostingsList());
        }

        for (int i = 0; i < query.queryterm.size(); i++) {
            String token = query.queryterm.get(i).term;

            int k = 0;

            PostingsList tokenList = index.getPostings(token);
            for (int j = 0; j < postingsList.size(); j++) {
                PostingsEntry entry = postingsList.get(j);

                

                // Find its original entry
                for (; k < tokenList.size(); k++) {
                    if (tokenList.get(k).docID == entry.docID) {
                        ret.get(i).add(tokenList.get(k));
                        break;
                    }
                } 
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
        PostingsList p2 = postingsLists.get(postingsLists.size() - 1);

        postingsLists.remove(postingsLists.size() - 1);

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
                                intersection.add(new PostingsEntry(docID1.docID, ps2));

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
     * @param query The query containing tokens
     * @return A PostingsList with the matches
     * @throws IllegalArgumentException When a token in the query does not exist in the index.
     */
    private PostingsList getIntersectionQuery(Query query) throws IllegalArgumentException {

        commonIndices.clear();

        ArrayList<PhraseToken> postingsLists = getPostingsLists(query);
        PostingsList intersection;

        Collections.sort(postingsLists, Collections.reverseOrder());

        /** Iterate all queries */
        PostingsList p1;
        PostingsList p2;

        intersection = postingsLists.get(0).postingsList;

        int j;
        int k;

        for (int i = 1; i < postingsLists.size(); i++) {
            j = 0;
            k = 0;

            p1 = intersection;
            p2 = postingsLists.get(i).postingsList;
            intersection = new PostingsList();

            PostingsEntry docID1 = p1.get(j++);
            PostingsEntry docID2 = p2.get(k++);

            try {
                for (;;) {
                    if (docID1.docID == docID2.docID) {
                        intersection.add(docID1);
                        docID1 = p1.get(j++);
                        docID2 = p2.get(k++);
                    } else if (docID1.docID < docID2.docID) {
                        docID1 = p1.get(j++);
                    } else {
                        docID2 = p2.get(k++);
                    }
                }
            } catch (Exception e) {
                // Do nothing
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
     */
    private ArrayList<PhraseToken> getPostingsLists(Query query) throws IllegalArgumentException {
        ArrayList<PhraseToken> postingsLists = new ArrayList<>();

        // Retrive all postingsLists for query tokens
        for (int i = 0; i < query.queryterm.size(); i++) {
            String token = query.queryterm.get(i).term;

            PostingsList tokenList = index.getPostings(token);

            // If one term does not exist, whole intersection query fails
            if (tokenList != null)
                postingsLists.add(new PhraseToken(token, i,tokenList));
            else
                throw new IllegalArgumentException("Token " + token + " has no matches");
        }

        return postingsLists;
    }
}