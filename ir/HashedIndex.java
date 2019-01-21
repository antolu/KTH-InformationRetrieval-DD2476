/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {


    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();
    private HashMap<String,HashMap<Integer,Boolean>> existenceIndex = new HashMap<String,HashMap<Integer,Boolean>>();


    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        // A PostingsList does not exist
        if (index.get(token) == null) {
            PostingsList list = new PostingsList();
            list.add(new PostingsEntry(docID));
            index.put(token, list);

            HashMap<Integer,Boolean> exIdx = new HashMap<>();
            exIdx.put(docID, true);
            existenceIndex.put(token,exIdx);
        } else {
            PostingsList list = index.get(token);
            if (existenceIndex.get(token).get(docID) != null)
                list.add(new PostingsEntry(docID));
                existenceIndex.get(token).put(docID,true);
        }
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        return index.get(token);
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
