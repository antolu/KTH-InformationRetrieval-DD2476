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
import java.lang.StringBuilder;

public class PostingsList extends ArrayList<PostingsEntry> implements Comparable<PostingsList> {

    public PostingsList() {
        super();
    }

    @Override
    public int compareTo(PostingsList o) {
        return Integer.compare(o.size(), this.size());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (PostingsEntry pe: this) {
            sb.append(pe.toString());
            sb.append(":");
        }

        /** Remove overflowing semicolon */
        sb.setLength(sb.length() - 1);
        sb.append("\n");
        
        return sb.toString();
    }
}

