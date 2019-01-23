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


    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        // A PostingsList does not exist
        if (!index.containsKey(token)) {
            // Add to general purpose index
            PostingsList list = new PostingsList();
            list.add(new PostingsEntry(docID, offset));
            index.put(token, list);

        } else {
            PostingsList list = index.get(token);

            /** Add to general index only if not does not exist already */
            if (list.get(list.size()-1).docID != docID)
                list.add(new PostingsEntry(docID, offset));
            else if (list.get(list.size()-1).docID == docID)
                list.get(list.size()-1).addPosition(offset);
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
