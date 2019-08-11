/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;


public class KGramPostingsEntry {
    int tokenID;

    public KGramPostingsEntry(int tokenID) {
        this.tokenID = tokenID;
    }

    public KGramPostingsEntry(KGramPostingsEntry other) {
        this.tokenID = other.tokenID;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof KGramPostingsEntry))
                return false;

        if (tokenID == ((KGramPostingsEntry) other).tokenID)
            return true;
        else
            return false;
    }

    public String toString() {
        return tokenID + "";
    }
}
