/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;
import java.lang.StringBuilder;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public static final String OFFSET_DELIM = ",";
    public int docID;
    public double score = 0;
    private ArrayList<Integer> positionList = new ArrayList<>();

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
       return Double.compare( other.score, score );
    }

    public PostingsEntry(int docID, int offset) {
        this.docID = docID;
        this.positionList.add(offset);
    }

    /**
     * @return the positionList
     */
    public ArrayList<Integer> getPositionList() {
        return positionList;
    }

    public void addPosition(int offset) {
        positionList.add(offset);
    }

    public void reserveOffsetCapacity(int cap) {
        positionList.ensureCapacity(cap);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(docID);
        sb.append(OFFSET_DELIM);

        for (int offset: positionList) {
            sb.append(offset);
            sb.append(OFFSET_DELIM);
        }

        sb.setLength(sb.length() - 1);

        return sb.toString();
    }
}

