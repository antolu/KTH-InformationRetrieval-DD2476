package pagerank;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class PageRankSparse {

	/**
	 * Maximal number of documents. We're assuming here that we don't have more docs
	 * than we can keep in main memory.
	 */
	final static int MAX_NUMBER_OF_DOCS = 2000000;

	/**
	 * Mapping from document names to document numbers.
	 */
	HashMap<String, Integer> docNumber = new HashMap<String, Integer>();

	/**
	 * Mapping from document numbers to document names
	 */
	String[] docName = new String[MAX_NUMBER_OF_DOCS];

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

	/**
	 * The number of outlinks from each node.
	 */
	int[] out = new int[MAX_NUMBER_OF_DOCS];
	
    HashMap<Integer, LinkedHashMap<Integer, Double>> G = new HashMap<>();

	/**
	 * The probability that the surfer will be bored, stop following links, and take
	 * a random jump somewhere.
	 */
	final static double BORED = 0.15;

	/**
	 * Convergence criterion: Transition probabilities do not change more that
	 * EPSILON from one iteration to another.
	 */
	final static double EPSILON = 0.00001;

	private int fileIndex = 0;

	/**
	 * A Pair implementation so one can sort
	 * a list with regard to one value while
	 * retaining reference to the second one. 
	*/
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

	public PageRankSparse(String filename) {
		int noOfDocs = readDocs(filename);
		initiateProbabilityMatrix(noOfDocs);
		// compareMatrices(G_mat, sparseToMatrix(1.0/noOfDocs));
		iterate(noOfDocs, 1000);
	}

	/* --------------------------------------------- */

	/**
	 * Reads the documents and fills the data structures.
	 *
	 * @return the number of documents read.
	 */
	private int readDocs(String filename) {
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
					// Set the probability to 0 for now, to indicate that there is
					// a link from fromdoc to otherDoc.
					if (link.get(fromdoc) == null) {
						link.put(fromdoc, new HashMap<Integer, Boolean>());
					}
					if (link.get(fromdoc).get(otherDoc) == null) {
						link.get(fromdoc).put(otherDoc, true);
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

	/**
	 * Initializes the probability matrix p
	 * 
	 * @param int numberOfDocs
	 */
	void initiateProbabilityMatrix(int numberOfDocs) {
		final double NOT_BORED = 1.0 - BORED;

        for (int i = 0; i < numberOfDocs; i++) {
			if (out[i] != 0) {
				LinkedHashMap<Integer, Double> row = new LinkedHashMap<>();
				for (int j: link.get(i).keySet())
					row.put(j, NOT_BORED / out[i]);
				G.put(i, row);
			}
		}
    }

	/*
	 * Chooses a probability vector a, and repeatedly computes aP, aP^2, aP^3...
	 * until aP^i = aP^(i+1).
	 */
	private void iterate(int numberOfDocs, int maxIterations) {
		double[] a_old = new double[numberOfDocs];
		double[] a = new double[numberOfDocs];
        a[0] = 1.0;

        int i = 0;
        double err = 10;
        while (err > EPSILON && i < maxIterations) {
		// while (i < 30) {
			System.err.println("Iteration: " + i);
            i++;
            a_old = a;
            a = multiply(a, 1.0/numberOfDocs);
			normalize(a);
			// for (int j = 0; j < 30; j++) 
			// 	System.err.println(a[j]);

            err = distance(a_old, a);
        }

        System.err.println("Iterations: " + i);

        getResults(a);
	}

    void getResults(double[] a) {
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

	private double[][] sparseToMatrix(double defaultValue) {
		double[][] mat = new double[fileIndex][fileIndex];

		double rowDefault = BORED /fileIndex;

		for (int j = 0; j < fileIndex; j++) {
			if (G.containsKey(j)) {
				LinkedHashMap<Integer, Double> row = G.get(j);
				for (Map.Entry<Integer, Double> e: row.entrySet()) {
					mat[j][e.getKey()] = e.getValue(); 
				}
				for (int i = 0; i < fileIndex; i++) {
					mat[j][i] += rowDefault;
				}
			} else {
				for (int i = 0; i < fileIndex; i++) {
					mat[j][i] = defaultValue;
				}
			}
		}

		return mat;
	}
	
	private double[] multiply(double[] vec, double defaultValue) {
		double[] prod = new double[vec.length];

		double rowDefault = BORED /fileIndex;

		for (int j = 0; j < vec.length; j++) {
			if (G.containsKey(j)) {
				LinkedHashMap<Integer, Double> row = G.get(j);
				for (Map.Entry<Integer, Double> e: row.entrySet()) {
					prod[e.getKey()] += vec[j] * e.getValue();
				}
				for (int i = 0; i < vec.length; i++) {
					prod[i] += vec[j] * rowDefault;
				}
			} else {
				double val = vec[j] * defaultValue;
				for (int i = 0; i < vec.length; i++) {
					prod[i] += val;
				}
			}
		}

		return prod;
	}
	private static void normalize(double[] a) {
        
		double norm = 0.0;
        for (int i = 0; i < a.length; i++){
			norm += a[i];
		}
		
		for (int i = 0; i < a.length; i++){
			a[i] /= norm;
		}
	}
	
	private static double distance(double[] a, double[] b) throws IllegalArgumentException {

        if (a.length != b.length) {
            throw new IllegalArgumentException("Incompatible dimensions: " + a.length + ", " + b.length);
        }

        double alignment = 0.0;
        
        for (int i = 0; i < a.length; i++){
            alignment += Math.pow(a[i] - b[i], 2);
        }

        alignment = Math.sqrt(alignment);

        return alignment;
	}
	
	private void compareMatrices(Matrix a, double[][] b) {
		for (int i = 0; i < a.m; i++) {
			for (int j = 0; j < a.m; j++) {
				if (Math.abs(a.mtx[i][j] - b[i][j]) > 0.0000001) {
					System.err.println("(" + i + ", " + j + ")");
					System.err.println(a.mtx[i][j] + " " + b[i][j]);
					return;
				}
			}
		}
	}


	/* --------------------------------------------- */

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Please give the name of the link file");
		} else {
			new PageRankSparse(args[0]);
		}
	}
}