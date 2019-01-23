package ir;

public class Triple {
    public PostingsEntry doc;
    public int p1;
    public int p2;

    public Triple(PostingsEntry doc, int p1, int p2) {
        this.doc = doc;
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public String toString() {
        return "(" + doc.docID + ", " + p1 + ", " + p2 + ")";
    }
}