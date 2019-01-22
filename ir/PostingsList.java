/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.lang.Comparable;
import java.lang.Integer;

public class PostingsList extends ArrayList<PostingsEntry> implements Comparable<PostingsList> {

    public PostingsList() {
        super();
    }

    @Override
    public int compareTo(PostingsList o) {
        return Integer.compare(o.size(), this.size());
    }

    // 
    //  YOUR CODE HERE
    //
}

