package pagerank;

import java.util.*;
import java.io.*;

public class PageRank {

    /**
     * Maximal number of documents. We're assuming here that we don't have more docs
     * than we can keep in main memory;
     */
    final static int MAX_NUMBER_OF_DOCS = 1000;

    /**
     * Mapping from document names to document numbers.
     */
    Hashtable<String, Integer> docNumber = new Hashtable<String, Integer>();

    /**
     * Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**
     * The transition matrix. p[i][j] = the probability that the random surfer
     * clicks from page i to page j.
     */
    Matrix p = new Matrix(MAX_NUMBER_OF_DOCS, MAX_NUMBER_OF_DOCS);
    Matrix J = new Matrix(MAX_NUMBER_OF_DOCS, MAX_NUMBER_OF_DOCS);
    Matrix G = new Matrix(MAX_NUMBER_OF_DOCS, MAX_NUMBER_OF_DOCS);

    /**
     * The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     * The probability that the surfer will be bored, stop following links, and take
     * a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     * In the initializaton phase, we use a negative number to represent that there
     * is a direct link from a document to another.
     */
    final static double LINK = -1.0;

    /**
     * Convergence criterion: Transition probabilities do not change more that
     * EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.000001;

    private int fileIndex = 0;

    private class Pair implements Comparable<Pair> {
        public int docID = 0;
        public double value = 0.0;

        public Pair(int docID, double value) {
            this.docID = docID;
            this.value = value;
        }

        @Override
        public int compareTo(Pair other) {
            return Double.compare(value, other.value);
        }
    }

    /* --------------------------------------------- */

    public PageRank(String filename) {
        int noOfDocs = readDocs(filename);
        initiateProbabilityMatrix(noOfDocs);
        iterate(noOfDocs, 100);
    }

    /* --------------------------------------------- */

    /**
     * Reads the documents and fills the data structures. When this method finishes
     * executing, <code>p[i][j] = LINK</code> if there is a direct link from i to j,
     * and <code>p[i][j] = 0</code> otherwise.
     * <p>
     *
     * @return the number of documents read.
     */
    int readDocs(String filename) {
        fileIndex = 0;
        try {
            System.err.print("Reading file... ");
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS) {
                int index = line.indexOf(";");
                String title = line.substring(0, index);
                Integer fromdoc = docNumber.get(title);
                // Have we seen this document before?
                if (fromdoc == null) {
                    // This is a previously unseen doc, so add it to the table.
                    fromdoc = fileIndex++;
                    docNumber.put(title, fromdoc);
                    docName[fromdoc] = title;
                }
                // Check all outlinks.
                StringTokenizer tok = new StringTokenizer(line.substring(index + 1), ",");
                while (tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS) {
                    String otherTitle = tok.nextToken();
                    Integer otherDoc = docNumber.get(otherTitle);
                    if (otherDoc == null) {
                        // This is a previousy unseen doc, so add it to the table.
                        otherDoc = fileIndex++;
                        docNumber.put(otherTitle, otherDoc);
                        docName[otherDoc] = otherTitle;
                    }
                    // Set the probability to LINK for now, to indicate that there is
                    // a link from d to otherDoc.
                    if (p.mtx[fromdoc][otherDoc] >= 0) {
                        p.mtx[fromdoc][otherDoc] = LINK;
                        out[fromdoc]++;
                    }
                }
            }
            if (fileIndex >= MAX_NUMBER_OF_DOCS) {
                System.err.print("stopped reading since documents table is full. ");
            } else {
                System.err.print("done. ");
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + filename + " not found!");
        } catch (IOException e) {
            System.err.println("Error reading file " + filename);
        }
        System.err.println("Read " + fileIndex + " number of documents");
        return fileIndex;
    }

    /* --------------------------------------------- */

    /*
     * Initiates the probability matrix.
     */
    void initiateProbabilityMatrix(int numberOfDocs) {
        Matrix J = Matrix.fillMatrix(MAX_NUMBER_OF_DOCS, MAX_NUMBER_OF_DOCS, BORED/(fileIndex-1));

        for (int i = 0; i < fileIndex-1; i++) {
            for (int j = 0; j < fileIndex-1; j++) {
                if (p.mtx[i][j] == -1 && out[i] != 0) {
                    p.mtx[i][j] = 1.0 / out[i];
                }
                else if (out[i] == 0 ) {
                    p.mtx[i][j] = 1.0 / (fileIndex-1);
                }
            }

            for (int j = fileIndex; j < MAX_NUMBER_OF_DOCS; j++) {
                J.mtx[i][j] = 0.0;
            }
        }

        for (int i = fileIndex; i < MAX_NUMBER_OF_DOCS; i++) {
            for (int j = 0; j < MAX_NUMBER_OF_DOCS; j++) {
                J.mtx[i][j] = 0.0;
            }
        }

        Matrix.scalarMult(p, 1.0 - BORED);

        G = Matrix.add(p, J);
    }

    /* --------------------------------------------- */

    /*
     * Chooses a probability vector a, and repeatedly computes aP, aP^2, aP^3...
     * until aP^i = aP^(i+1).
     */
    void iterate(int numberOfDocs, int maxIterations) {

        Matrix a_old = Matrix.fillMatrix(1, MAX_NUMBER_OF_DOCS, 10);
        Matrix a = new Matrix(1, MAX_NUMBER_OF_DOCS);
        a.mtx[0][0] = 1;

        int i = 0;
        double err = 10;
        while (err > EPSILON) {
            i++;
            a_old = a;
            a = Matrix.multiply(a, G);
            Matrix.normalize(a);

            err = Matrix.distance(a_old, a);
        }

        System.err.println("Iterations: " + i);

        getResults(a);
    }

    void getResults(Matrix a) {
        ArrayList<Pair> results = new ArrayList<>();

        for (int i = 0; i < a.n; i++) {
            results.add(new Pair(i, a.mtx[0][i]));
        }

        Collections.sort(results, Collections.reverseOrder());

        for (int i = 0; i < 30; i++) {
            Pair pair = results.get(i);
            String name = docName[pair.docID];

            System.err.format(name + " %.5f%n", pair.value);
        }
    }

    /* --------------------------------------------- */

    public static void main(String[] args) {   
        if (args.length != 1) {
            System.err.println("Please give the name of the link file");
        } else {
            new PageRank(args[0]);
        }
    }
}