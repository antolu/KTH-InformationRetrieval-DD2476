/**
 *   Computes the Hubs and Authorities for an every document in a query-specific
 *   link graph, induced by the base set of pages.
 *
 *   @author Dmytro Kalpakchi
 */

package ir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import ir.PostingsEntry;
public class HITSRanker {

    /**
     * Max number of iterations for HITS
     */
    final static int MAX_NUMBER_OF_STEPS = 1000;

    /**
     * Maximal number of documents. We're assuming here that we don't have more docs
     * than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     * Convergence criterion: hub and authority scores do not change more that
     * EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.00001;

    /**
     * The inverted index
     */
    Index index;

    /**
     * Mapping from the titles to internal document ids used in the links file
     */
    HashMap<String, Integer> titleToId = new HashMap<String, Integer>();
    HashMap<Integer, String> IDToTitle = new HashMap<>();

    /**
     * Sparse vector containing hub scores
     */
    SparseVector hubs;

    /**
     * Mapping from document names to document numbers.
     */
    HashMap<String, Integer> docNumber = new HashMap<String, Integer>();

    /**
     * Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**
     * Sparse vector containing authority scores
     */
    SparseVector authorities;

    SparseMatrix origA;
    SparseMatrix origAT;

    SparseMatrix A;
    SparseMatrix AAT;
    SparseMatrix ATA;
    SparseMatrix AT;

    private static final String INDEXDIR = "index/";
    private static final String DATADIR = "data/";

    private static int numberOfDocs = 0;

    /**
     * A memory-efficient representation of the transition matrix. The outlinks are
     * represented as a HashMap, whose keys are the numbers of the documents linked
     * from.
     * <p>
     *
     * The value corresponding to key i is a HashMap whose keys are all the numbers
     * of documents j that i links to.
     * <p>
     *
     * If there are no outlinks from i, then the value corresponding key i is null.
     */
    HashMap<Integer, HashMap<Integer, Boolean>> link = new HashMap<Integer, HashMap<Integer, Boolean>>();

    /* --------------------------------------------- */

    /**
     * Constructs the HITSRanker object
     * 
     * A set of linked documents can be presented as a graph. Each page is a node in
     * graph with a distinct nodeID associated with it. There is an edge between two
     * nodes if there is a link between two pages.
     * 
     * Each line in the links file has the following format:
     * nodeID;outNodeID1,outNodeID2,...,outNodeIDK This means that there are edges
     * between nodeID and outNodeIDi, where i is between 1 and K.
     * 
     * Each line in the titles file has the following format: nodeID;pageTitle
     * 
     * NOTE: nodeIDs are consistent between these two files, but they are NOT the
     * same as docIDs used by search engine's Indexer
     *
     * @param linksFilename  File containing the links of the graph
     * @param titlesFilename File containing the mapping between nodeIDs and pages
     *                       titles
     * @param index          The inverted index
     */
    public HITSRanker(String linksFilename, String titlesFilename, Index index) {
        this.index = index;
        numberOfDocs = readDocs(linksFilename, titlesFilename);

        // for (Map.Entry<String, Integer> e: docNumber.entrySet()) {
        //     System.err.println(e.getKey() + ": " + e.getValue());
        // }

        // System.err.println(titleToId.get("StevenWong.f"));

        initiateProbabilityMatrix(numberOfDocs);
    }

    /* --------------------------------------------- */

    /**
     * A utility function that gets a file name given its path. For example, given
     * the path "davisWiki/hello.f", the function will return "hello.f".
     *
     * @param path The file path
     *
     * @return The file name.
     */
    private String getFileName(String path) {
        String result = "";
        StringTokenizer tok = new StringTokenizer(path, "\\/");
        while (tok.hasMoreTokens()) {
            result = tok.nextToken();
        }
        return result;
    }

    /**
     * Reads the files describing the graph of the given set of pages.
     *
     * @param linksFilename  File containing the links of the graph
     * @param titlesFilename File containing the mapping between nodeIDs and pages
     *                       titles
     */
    int readDocs(String linksFilename, String titlesFilename) {
        int fileIndex = 0;
        try {
            System.err.print("Reading file... ");
            BufferedReader in = new BufferedReader(new FileReader(linksFilename));
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
                    // Set the probability to 0 for now, to indicate that there is
                    // a link from fromdoc to otherDoc.
                    if (link.get(fromdoc) == null) {
                        link.put(fromdoc, new HashMap<Integer, Boolean>());
                    }
                    if (link.get(fromdoc).get(otherDoc) == null) {
                        link.get(fromdoc).put(otherDoc, true);
                    }
                }
            }
            if (fileIndex >= MAX_NUMBER_OF_DOCS) {
                System.err.print("stopped reading since documents table is full. ");
            } else {
                System.err.print("done. ");
            }
            System.err.print("Reading titles... ");
            BufferedReader inTitles = new BufferedReader(new FileReader(titlesFilename));

            int i = 0;
            while ((line = inTitles.readLine()) != null) {
                int index = line.indexOf(";");
                String internalTitle = line.substring(0, index);
                String docName = line.substring(index+1);

                titleToId.put(docName, this.docNumber.get(internalTitle));
                IDToTitle.put(this.docNumber.get(internalTitle), docName);
                i++;
            }

        } catch (FileNotFoundException e) {
            System.err.println("File " + linksFilename + " not found!");
        } catch (IOException e) {
            System.err.println("Error reading file " + linksFilename);
        }
        System.err.println("Read " + fileIndex + " number of documents");
        return fileIndex;
    }

    /**
	 * Initializes the probability matrix G
	 * 
	 * @param numberOfDocs Number of documents (size of G matrix)
	 */
	void initiateProbabilityMatrix(int numberOfDocs) {
		origA = new SparseMatrix(numberOfDocs, numberOfDocs);

		/** Calculate non-zero entries */
        for (int i = 0; i < numberOfDocs; i++) {
			if (link.containsKey(i)) {
				LinkedHashMap<Integer, Double> row = origA.newRow(i);
				for (int j: link.get(i).keySet())
					row.put(j, 1.0);
			}
        }

        origAT = origA.getTransposed();
    }

    /**
     * Perform HITS iterations until convergence
     *
     * @param titles The titles of the documents in the root set
     */
    private void iterate(String[] titles) {
        SparseVector oldHubs = new SparseVector(numberOfDocs);
        SparseVector oldAuths = new SparseVector(numberOfDocs);

        hubs = new SparseVector(numberOfDocs);
        authorities = new SparseVector(numberOfDocs);

        for (int i = 0; i < numberOfDocs; i++) {
            hubs.put(i, 1.0);
            authorities.put(i, 1.0);
        }

        int i = 0;
        double err = 10;
        while (err > EPSILON && i < MAX_NUMBER_OF_STEPS) {
            System.err.println("Iteration: " + i);
            i++;
            oldHubs = hubs;
            oldAuths = authorities;
            hubs = A.multiplyVector(oldAuths);
            authorities = AT.multiplyVector(oldHubs);
            normalize(hubs);
            normalize(authorities);

            err = alignment(oldHubs, hubs) + alignment(oldAuths, authorities);
        }

        System.err.println("Iterations: " + i);

        displayTopResults(hubs);
        System.err.println();
        displayTopResults(authorities);
    }

    /**
     * Displays the top results of the pagerank
     * 
     * @param a The vector with pagerank values
     */
    void displayTopResults(SparseVector a) {
        ArrayList<Pair> results = new ArrayList<>();

        for (Map.Entry<Integer, Double> e : a.entrySet()) {
            results.add(new Pair(e.getKey(), e.getValue()));
        }

        Collections.sort(results, Collections.reverseOrder());

        for (int i = 0; i < 30; i++) {
            Pair pair = results.get(i);
            String name = docName[pair.first];

            System.err.format(name + " %.5f%n", pair.value);
        }
    }

    /**
     * Rank the documents in the subgraph induced by the documents present in the
     * postings list `post`.
     *
     * @param post The list of postings fulfilling a certain information need
     *
     * @return A list of postings ranked according to the hub and authority scores.
     */
    PostingsList rank(PostingsList post) {

        ArrayList<Integer> rootSet = new ArrayList<>();
        rootSet.ensureCapacity(post.size());

        String[] docTitles = new String[post.size()];

        int i = 0;
        for (PostingsEntry pe: post) {
            String docTitle = getFileName(Index.docNames.get(pe.docID));

            int internalID = titleToId.get(docTitle);

            docTitles[i++] = docTitle;
            rootSet.add(internalID);
        }

        HashSet<Integer> baseSet = new HashSet<>();

        for (int rootDoc: rootSet) {
            if (origA.containsKey(rootDoc)) {
                for (Map.Entry<Integer, Double> j: origA.get(rootDoc).entrySet()) {
                    baseSet.add(j.getKey());
                }
            }
            if (origAT.containsKey(rootDoc)) {
                for (Map.Entry<Integer, Double> j: origAT.get(rootDoc).entrySet()) {
                    baseSet.add(j.getKey());
                }
            }
        }

        A = new SparseMatrix(numberOfDocs, numberOfDocs);
        AT = new SparseMatrix(numberOfDocs, numberOfDocs);

        for (int baseDoc: baseSet) {
            if (origA.containsKey(baseDoc)) {
                A.put(baseDoc, origA.get(baseDoc));
            }
            if (origAT.containsKey(baseDoc)) {
                AT.put(baseDoc, origAT.get(baseDoc));
            }
        }

        iterate(docTitles);

        for (Map.Entry<Integer, Double> ID: hubs.entrySet()) {
            double hubScore = ID.getValue();
            double authScore = authorities.get(ID.getKey());

            // int searcherDocID = docName[ID.getKey()];
        }

        return post;
    }

    /**
     * Sort a hash map by values in the descending order
     *
     * @param map A hash map to sorted
     *
     * @return A hash map sorted by values
     */
    private HashMap<Integer, Double> sortHashMapByValue(HashMap<Integer, Double> map) {
        if (map == null) {
            return null;
        } else {
            List<Map.Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer, Double>>(map.entrySet());

            Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
                public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            });

            HashMap<Integer, Double> res = new LinkedHashMap<Integer, Double>();
            for (Map.Entry<Integer, Double> el : list) {
                res.put(el.getKey(), el.getValue());
            }

            return res;
        }
    }

    /**
     * Write the first `k` entries of a hash map `map` to the file `fname`.
     *
     * @param map   A hash map
     * @param fname The filename
     * @param k     A number of entries to write
     */
    void writeToFile(HashMap<Integer, Double> map, String fname, int k) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fname));

            if (map != null) {
                int i = 0;
                for (Map.Entry<Integer, Double> e : map.entrySet()) {
                    i++;
                    writer.write(e.getKey() + ": " + String.format("%.5g%n", e.getValue()));
                    if (i >= k)
                        break;
                }
            }
            writer.close();
        } catch (IOException e) {
        }
    }

    /**
     * Rank all the documents in the links file. Produces two files: hubs_top_30.txt
     * with documents containing top 30 hub scores authorities_top_30.txt with
     * documents containing top 30 authority scores
     */
    void rank() {
        initiateProbabilityMatrix(numberOfDocs);
        iterate(titleToId.keySet().toArray(new String[0]));
        HashMap<Integer, Double> sortedHubs = sortHashMapByValue(hubs);
        HashMap<Integer, Double> sortedAuthorities = sortHashMapByValue(authorities);
        writeToFile(sortedHubs, "hubs_top_30.txt", 30);
        writeToFile(sortedAuthorities, "authorities_top_30.txt", 30);
    }

    /**
     * Normalizes a vector with Manhattan length
     * 
     * @param a The vector to be normalized
     */
    private static void normalize(SparseVector a) {

        double norm = 0.0;
        for (Map.Entry<Integer, Double> e : a.entrySet()) {
            norm += Math.pow(e.getValue(), 2.0);
        }

        norm = Math.sqrt(norm);

        for (Map.Entry<Integer, Double> e : a.entrySet()) {
            e.setValue(e.getValue() / norm);
        }
    }

    /**
     * Measures the euclidean distance between two vectors
     * 
     * @param a The first vector
     * @param b The second vector
     * 
     * @throws IllegalArgumentException when vector dimensions are imcompatible
     * 
     * @return The euclidean distance between the two vectors
     */
    private static double alignment(SparseVector a, SparseVector b) throws IllegalArgumentException {

        if (a.m != b.m) {
            throw new IllegalArgumentException("Incompatible dimensions: " + a.m + ", " + b.m);
        }

        double alignment = 0.0;

        HashSet<Integer> processedIndices = new HashSet<>();
        for (Map.Entry<Integer, Double> e : a.entrySet()) {
            if (b.containsKey(e.getKey())) {
                alignment += Math.pow(e.getValue() - b.get(e.getKey()), 2.0);
            } else {
                alignment += Math.pow(e.getValue(), 2.0);
            }
            processedIndices.add(e.getKey());
        }

        for (Map.Entry<Integer, Double> e : b.entrySet()) {
            if (!processedIndices.contains(e.getKey()))
                alignment += Math.pow(e.getValue(), 2.0);
        }

        alignment = Math.sqrt(alignment);

        return alignment;
    }

    /** 
     * Implementation of pair class, contains an integer and double
     * value. Sorts according to the double value.
     */
    private class Pair implements Comparable<Pair> {
        public int first = 0;
        public double value = 0.0;

        public Pair(int docID, double value) {
            this.first = docID;
            this.value = value;
        }

        @Override
        public int compareTo(Pair other) {
            return Double.compare(value, other.value);
        }
    }

    /**
     * Implementation of sparse matrix using hashmaps
     * 
     * <p> The index of the upper most layer represents the row of matrix, 
     * and its value the row contents. </p>
     */
    protected class SparseMatrix extends HashMap<Integer, LinkedHashMap<Integer, Double>> {

        protected int m;
        protected int n;

        /** Value for row if it its empty */
        protected double emptyRowValue;

        /** Value for row if it has non-zero entries */
        protected double defaultRowValue;

        public SparseMatrix(int m, int n) {
            super();
            this.m = m;
            this.n = n;
        }

        public LinkedHashMap<Integer, Double> newRow(int i) {
            LinkedHashMap<Integer, Double> row = new LinkedHashMap<>();

            put(i, row);
            return row;
        }

        /**
         * Right multiplies the matrix with a vector
         * 
         * @param vector The vector
         * 
         * @return The product
         * 
         * @throws IllegalArgumentException When matrix-vector dimensions are
         *                                  incompatible.
         */
        public SparseVector multiplyVector(SparseVector vector) {
            if (this.n != vector.m) {
                throw new IllegalArgumentException(
                        "Bad matrix dimensions: " + this.m + "x" + this.n + " , " + 1 + "x" + vector.m);
            }

            SparseVector prod = new SparseVector(vector.m);

            for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> matrixRow : entrySet()) {
                double elem = 0.0;
                for (Map.Entry<Integer, Double> e : matrixRow.getValue().entrySet()) {
                    try {
                        elem += e.getValue() * vector.get(e.getKey());
                    } catch (NullPointerException ex) {
                        // Do nothing, element does not exist in vector
                    }
                }
                prod.put(matrixRow.getKey(), elem);
            }

            return prod;
        }

        public SparseMatrix getTransposed() {
            SparseMatrix transposed = new SparseMatrix(n, m);

            for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> row: entrySet()) {
                for (Map.Entry<Integer, Double> val: row.getValue().entrySet()) {
                    if (!transposed.containsKey(val.getKey())) {
                        LinkedHashMap<Integer, Double> transposedRow = transposed.newRow(val.getKey());
                        transposedRow.put(row.getKey(), val.getValue());
                    } {
                        transposed.get(val.getKey()).put(row.getKey(), val.getValue());
                    }
                }
            }

            return transposed;
        }

        public SparseMatrix multiply(SparseMatrix other) {

            if (n != other.m) {
                throw new IllegalArgumentException();
            }

            SparseMatrix prod = new SparseMatrix(m, other.n);

            for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> row: entrySet()) {
                double[] fastRow = new double[other.n];

                LinkedHashMap<Integer, Double> prodRow = prod.newRow(row.getKey());

                for (Map.Entry<Integer, Double> e: row.getValue().entrySet()) {
                    if (other.containsKey(e.getKey())) {
                        LinkedHashMap<Integer, Double> otherRow = other.get(e.getKey());

                        double eValue = e.getValue();
                        for (Map.Entry<Integer, Double> otherE: otherRow.entrySet()) {
                            fastRow[otherE.getKey()] += eValue * otherE.getValue();
                        }
                    }
                }

                for (int i = 0; i < fastRow.length; i++) {
                    if (fastRow[i] != 0) {
                        prodRow.put(i, fastRow[i]);
                    }
                }
            }

            return prod;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> i : entrySet()) {
                for (Map.Entry<Integer, Double> j : i.getValue().entrySet()) {
                    sb.append("(" + i.getKey() + ", " + j.getKey() + "):" + j.getValue() + " ");
                }
                sb.append("\n");
            }

            return sb.toString();
        }
    }

    /**
     * Implementation of a sparse vector using a hashmap
     */
    protected class SparseVector extends HashMap<Integer, Double> {

        protected int m;

        public SparseVector(int m) {
            this.m = m;
        }
    }

    /* --------------------------------------------- */

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Please give the names of the link and title files");
        } else {
            HITSRanker hr = new HITSRanker(args[0], args[1], null);
            hr.rank();
        }
    }
}