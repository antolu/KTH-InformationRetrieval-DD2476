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
import java.util.Set;
import java.util.StringTokenizer;

import ir.PostingsEntry;
import sparse.SparseMatrix;
import sparse.SparseVector;

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

    SparseVector oldHubs;
    SparseVector oldAuths;

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

        oldHubs = new SparseVector(numberOfDocs);
        oldAuths = new SparseVector(numberOfDocs);

        hubs = new SparseVector(numberOfDocs);
        authorities = new SparseVector(numberOfDocs);

        for (int i = 0; i < numberOfDocs; i++) {
            hubs.put(i, 1.0);
            authorities.put(i, 1.0);
        }

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
                String docName = line.substring(index + 1);

                titleToId.put(docName, this.docNumber.get(internalTitle));
                IDToTitle.put(this.docNumber.get(internalTitle), docName);
                i++;
            }

            in.close();
            inTitles.close();

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
                for (int j : link.get(i).keySet())
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
    private void iterate(int maxIterations) {

        for (int i = 0; i < numberOfDocs; i++) {
            hubs.put(i, 1.0);
            authorities.put(i, 1.0);
        }

        int i = 0;
        double err = 10;
        while (err > EPSILON && i < maxIterations) {
            i++;

            oldHubs = hubs;
            oldAuths = authorities;

            hubs = A.multiplyVector(oldAuths);
            authorities = AT.multiplyVector(oldHubs);

            normalize(hubs);
            normalize(authorities);

            /** only calculate alignment if not doing it realtime */
            if (maxIterations > 100)
                err = alignment(oldHubs, hubs) + alignment(oldAuths, authorities);
        }

        System.err.println("Iterations: " + i);

        if (maxIterations > 100) {
            displayTopResults(hubs);
            System.err.println();
            displayTopResults(authorities);
        }
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

        /** Construct root set */
        ArrayList<Integer> rootSet = new ArrayList<>();
        rootSet.ensureCapacity(post.size());

        for (PostingsEntry pe : post) {
            String docTitle = getFileName(Index.docNames.get(pe.docID));

            try {
                int internalID = titleToId.get(docTitle);
                rootSet.add(internalID);
            } catch (NullPointerException e) {
            }

        }

        /** Construct baseset */
        HashSet<Integer> baseSet = new HashSet<>();

        for (int rootDoc : rootSet) {
            baseSet.add(rootDoc);

            /** All documents that link from rootDoc */
            if (origA.containsKey(rootDoc)) {
                for (Map.Entry<Integer, Double> j : origA.get(rootDoc).entrySet()) {
                    baseSet.add(j.getKey());
                }
            }

            /** All documents that link to rootDoc */
            if (origAT.containsKey(rootDoc)) {
                for (Map.Entry<Integer, Double> j : origAT.get(rootDoc).entrySet()) {
                    baseSet.add(j.getKey());
                }
            }
        }

        System.err.println("Base set size: " + baseSet.size());

        /** Construct the adjacency matrix and its transpose */
        A = new SparseMatrix(numberOfDocs, numberOfDocs);
        AT = new SparseMatrix(numberOfDocs, numberOfDocs);

        for (int baseDoc : baseSet) {
            if (origA.containsKey(baseDoc)) {
                A.put(baseDoc, origA.get(baseDoc));
            }
            if (origAT.containsKey(baseDoc)) {
                AT.put(baseDoc, origAT.get(baseDoc));
            }
        }

        iterate(5);

        /** Extract results */
        PostingsList results = new PostingsList();

        int k = 0;

        double score = 0.0;
        double authScore = 0.0;
        double hubScore = 0.0;
        for (int ID : baseSet) {
            try {
                hubScore = hubs.get(ID);
            } catch (NullPointerException e) {
            }
            try {
                authScore = authorities.get(ID);
            } catch (NullPointerException e) {
            }

            if (hubScore != authScore)
                score = (hubScore > authScore) ? hubScore : authScore;

            try {
                int docID = Index.docNamesToID.get("davisWiki/" + IDToTitle.get(ID));
                results.add(new PostingsEntry(docID, score));
            } catch (NullPointerException e) {
                k++;
            }

            score = 0.0;
            hubScore = 0.0;
            authScore = 0.0;
        }

        System.err.println("Number of lost documents: " + k);
        System.err.println("Results size: " + results.size() + "\n");

        Collections.sort(results);

        return results;
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
                    writer.write(docName[e.getKey()] + ": " + String.format("%.5g%n", e.getValue()));
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
        A = origA;
        AT = origAT;
        iterate(MAX_NUMBER_OF_STEPS);
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
     * Implementation of pair class, contains an integer and double value. Sorts
     * according to the double value.
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