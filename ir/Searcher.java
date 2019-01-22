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

        if (queryType == QueryType.INTERSECTION_QUERY) {
            return getIntersectionQuery(query);
        } else {

            String token = query.queryterm.get(0).term;

            PostingsList list = index.getPostings(token);

            return list;
        }

    }

    private PostingsList getIntersectionQuery(Query query) {

        ArrayList<PostingsList> postingsLists = new ArrayList<>();
        PostingsList intersection;

        // Not sufficient number of words for intersection query
        if (query.queryterm.size() < 2) {
            return index.getPostings(query.queryterm.get(0).term);
        }

        // Retrive all postingsLists for query tokens
        for (Query.QueryTerm q : query.queryterm) {
            String token = q.term;

            PostingsList tokenList = index.getPostings(token);

            // If one term does not exist, whole intersection query fails
            if (tokenList != null)
                postingsLists.add(tokenList);
            else
                return new PostingsList();
        }

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
}