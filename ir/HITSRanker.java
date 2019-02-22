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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
    final static double EPSILON = 0.001;

    /**
     * The inverted index
     */
    Index index;

    /**
     * Mapping from the titles to internal document ids used in the links file
     */
    HashMap<String, Integer> titleToId = new HashMap<String, Integer>();

    /**
     * Sparse vector containing hub scores
     */
    HashMap<Integer, Double> hubs;

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
    HashMap<Integer, Double> authorities;

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
        readDocs(linksFilename, titlesFilename);
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
		} catch (FileNotFoundException e) {
			System.err.println("File " + linksFilename + " not found!");
		} catch (IOException e) {
			System.err.println("Error reading file " + linksFilename);
		}
		System.err.println("Read " + fileIndex + " number of documents");
		return fileIndex;
    }

    /**
     * Perform HITS iterations until convergence
     *
     * @param titles The titles of the documents in the root set
     */
    private void iterate(String[] titles) {
        SparseVector oldHubs = new SparseVector(numberOfDocs);
        SparseVector oldAuths = new SparseVector(numberOfDocs);
		double[] a = new double[numberOfDocs];
        a[0] = 1.0;

        int i = 0;
        double err = 10;
        while (err > EPSILON && i < MAX_NUMBER_OF_STEPS) {
			System.err.println("Iteration: " + i);
            i++;
            a_old = a;
            a = multiply(a, G);
			normalize(a);

            err = distance(a_old, a);
        }

        System.err.println("Iterations: " + i);

		displayTopResults(a);
        return a;
    }

    /**
	 * Displays the top results of the pagerank
	 * 
	 * @param a The vector with pagerank values
	 */
    void displayTopResults(double[] a) {
        ArrayList<Pair> results = new ArrayList<>();

        for (int i = 0; i < a.length; i++) {
            results.add(new Pair(i, a[i]));
        }

        Collections.sort(results, Collections.reverseOrder());

        for (int i = 0; i < 30; i++) {
            Pair pair = results.get(i);
            String name = docName[pair.docID];

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
        //
        // YOUR CODE HERE
        //
        return null;
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
        iterate(titleToId.keySet().toArray(new String[0]));
        HashMap<Integer, Double> sortedHubs = sortHashMapByValue(hubs);
        HashMap<Integer, Double> sortedAuthorities = sortHashMapByValue(authorities);
        writeToFile(sortedHubs, "hubs_top_30.txt", 30);
        writeToFile(sortedAuthorities, "authorities_top_30.txt", 30);
    }

    /**
     * Left multiplies a vector with a matrix
     * 
     * @param vec A one dimensional vector
     * @param G   A sparse vector
     * 
     * @return The product of vec*G
     */
    private static double[] multiply(double[] vec, SparseMatrix G) {
        double[] prod = new double[vec.length];

        for (int j = 0; j < vec.length; j++) {
            if (G.mtx.containsKey(j)) {
                LinkedHashMap<Integer, Double> row = G.mtx.get(j);
                for (Map.Entry<Integer, Double> e : row.entrySet()) {
                    prod[e.getKey()] += vec[j] * e.getValue();
                }
                for (int i = 0; i < vec.length; i++) {
                    prod[i] += vec[j] * G.defaultRowValue;
                }
            } else {
                double val = vec[j] * G.emptyRowValue;
                for (int i = 0; i < vec.length; i++) {
                    prod[i] += val;
                }
            }
        }

        return prod;
    }

    /**
     * Normalizes a vector with Manhattan length
     * 
     * @param a The vector to be normalized
     */
    private static void normalize(double[] a) {

        double norm = 0.0;
        for (int i = 0; i < a.length; i++) {
            norm += a[i];
        }

        for (int i = 0; i < a.length; i++) {
            a[i] /= norm;
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
    private static double distance(double[] a, double[] b) throws IllegalArgumentException {

        if (a.length != b.length) {
            throw new IllegalArgumentException("Incompatible dimensions: " + a.length + ", " + b.length);
        }

        double alignment = 0.0;

        for (int i = 0; i < a.length; i++) {
            alignment += Math.pow(a[i] - b[i], 2);
        }

        alignment = Math.sqrt(alignment);

        return alignment;
    }

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
	
	protected class SparseMatrix {
		protected HashMap<Integer, LinkedHashMap<Integer, Double>> mtx = new HashMap<>();

		protected int m;
		protected int n;

		/** Value for row if it its empty */
		protected double emptyRowValue;

		/** Value for row if it has non-zero entries */
		protected double defaultRowValue;

		public SparseMatrix(int m, int n, double emptyRowValue, double defaultRowValue) {
			this.emptyRowValue = emptyRowValue;
			this.defaultRowValue = defaultRowValue;
		}

		public LinkedHashMap<Integer, Double> newRow(int i) {
			LinkedHashMap<Integer, Double> row = new LinkedHashMap<>();

			mtx.put(i, row);
			return row;
		}
    }
    
    protected class SparseVector extends HashMap<Integer, Double> {
		protected HashMap<Integer, Double> mtx = new HashMap<>();

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