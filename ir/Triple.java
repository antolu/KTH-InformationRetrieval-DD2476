package ir;

public class Triple {
    public int docID;
    public int p1;
    public int p2;

    public Triple(int docID, int p1, int p2) {
        this.docID = docID;
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public String toString() {
        return "(" + docID + ", " + p1 + ", " + p2 + ")";
    }
}