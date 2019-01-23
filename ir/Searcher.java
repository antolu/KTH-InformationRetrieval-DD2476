/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.Iterator;
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

        // Not sufficient number of words for other query
        if (query.queryterm.size() < 2) {
            return index.getPostings(query.queryterm.get(0).term);
        }

        if (queryType == QueryType.INTERSECTION_QUERY) {
            return getIntersectionQuery(query);
        }
        else if (queryType == QueryType.PHRASE_QUERY) {
            return getPhraseQuery(query);
        } else {

            String token = query.queryterm.get(0).term;

            PostingsList list = index.getPostings(token);

            return list;
        }

    }

    private PostingsList getPhraseQuery(Query query) {
        ArrayList<PostingsList> postingsLists = getPostingsLists(query);

        // Collections.sort(postingsLists);

        HashMap<Integer, HashMap<Integer, Integer>> posIntersect = new HashMap<>();
        ArrayList<PostingsEntry> posKeys = new ArrayList<>();

        PostingsList p1;
        PostingsList p2;

        ArrayList<Triple> answer;

        /* Get all biwords */
        for (int i = 1; i < postingsLists.size(); i++) {
            p1 = postingsLists.get(i-1);
            p2 = postingsLists.get(i);
            
            answer = getPositionalIntersect(p1, p2);

            for (int j = 0; j < answer.size(); j++) {
                Triple triple = answer.get(j);
                if (!posIntersect.containsKey(triple.doc.docID)) {
                    HashMap<Integer, Integer> temp = new HashMap<>();
                    posIntersect.put(triple.doc.docID, temp);
                    posKeys.add(triple.doc);
                }
                posIntersect.get(triple.doc.docID).put(triple.p1, triple.p2);
            }
        }

        /* Find all matching queries */
        PostingsList queryAnswers = new PostingsList();

        for (PostingsEntry doc : posKeys) {

            HashMap<Integer,Integer> mappedTuples = posIntersect.get(doc.docID);

            if (query.queryterm.size() == 2 && mappedTuples.size() >= 1) {
                queryAnswers.add(doc);
                continue;
            } else if (mappedTuples.size() < query.queryterm.size() - 1)
                continue; // Not enough tuples for phrase query

            int pos1;
            int pos2;

            loop:
            for (int key : mappedTuples.keySet()) {
                pos1 = key;
                pos2 = mappedTuples.get(pos1);
                int counter = 0;

                for (;;)
                    if (mappedTuples.containsKey(pos2)) {
                        pos1 = pos2;
                        pos2 = mappedTuples.get(pos2);
                        counter++;
                    } else break;
                // System.out.println(counter);
                if (counter == query.queryterm.size()-2) {
                    queryAnswers.add(doc);
                    break loop; // Match found
                }
            }
        }

        return queryAnswers;
    }

    private PostingsList getIntersectionQuery(Query query) {

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

    private ArrayList<Triple> getPositionalIntersect(PostingsList p1, PostingsList p2) {
        Iterator<PostingsEntry> itp1 = p1.iterator();
        Iterator<PostingsEntry> itp2 = p2.iterator();

        PostingsEntry docID1 = (PostingsEntry) itp1.next();
        PostingsEntry docID2 = (PostingsEntry) itp2.next();

        ArrayList<Triple> answer = new ArrayList<>();

        try {
            for (;;) {
                // Same documents
                if (docID1.docID == docID2.docID) {

                    /* Get positional indexes */
                    ArrayList<Integer> pList1 = docID1.getPositionList();
                    ArrayList<Integer> pList2 = docID2.getPositionList();

                    /* Compare indexes */
                    for (int pos1 = 0; pos1 < pList1.size(); pos1++) {
                        for (int pos2 = 0; pos2 < pList2.size(); pos2++) {
                            if (pList2.get(pos2) < pList1.get(pos1)) 
                                continue;

                            if (pList2.get(pos2) == pList1.get(pos1) + 1) {
                                answer.add(new Triple(docID1, pList1.get(pos1), pList2.get(pos2)));
                            }
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

        return answer;
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