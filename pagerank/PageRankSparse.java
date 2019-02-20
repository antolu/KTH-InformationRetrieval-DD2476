package pagerank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

	private static final String INDEXDIR = "index/";

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
	
    Sparse G;

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
	
	protected class Sparse {
		protected HashMap<Integer, LinkedHashMap<Integer, Double>> mtx = new HashMap<>();

		protected int m;
		protected int n;

		/** Value for row if it its empty */
		protected double emptyRowValue;

		/** Value for row if it has non-zero entries */
		protected double defaultRowValue;

		public Sparse(int m, int n, double emptyRowValue, double defaultRowValue) {
			this.emptyRowValue = emptyRowValue;
			this.defaultRowValue = defaultRowValue;
		}

		public LinkedHashMap<Integer, Double> newRow(int i) {
			LinkedHashMap<Integer, Double> row = new LinkedHashMap<>();

			mtx.put(i, row);
			return row;
		}
	}

	/* --------------------------------------------- */

	public PageRankSparse(String filename) {
		int noOfDocs = readDocs(filename);
		initiateProbabilityMatrix(noOfDocs);

		long startTime = System.nanoTime();
		double[] pagerank =  iterate(noOfDocs, 1000);
		long endTime = System.nanoTime();

		double duration = ((double)(endTime - startTime))/1000000000.0;
		System.err.printf("Duration: %fs%n", duration);
		System.err.printf("Writing pageranks to file...%n");
		try {
			writePageRanks(docName, pagerank);
		} catch (IOException e) {
			System.err.println("IOException! Write failed.");
		}
		System.err.println("Done!");
	}

	/* --------------------------------------------- */

	/**
	 * Reads the documents and fills the data structures.
	 *
	 * @return the number of documents read.
	 */
	private int readDocs(String filename) {
		int fileIndex = 0;
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
	 * Initializes the probability matrix G
	 * 
	 * @param numberOfDocs Number of documents (size of G matrix)
	 */
	void initiateProbabilityMatrix(int numberOfDocs) {
		final double NOT_BORED = 1.0 - BORED;

		G = new Sparse(numberOfDocs, numberOfDocs, 1.0/numberOfDocs, BORED/numberOfDocs);

		/** Calculate non-zero entries */
        for (int i = 0; i < numberOfDocs; i++) {
			if (out[i] != 0) {
				LinkedHashMap<Integer, Double> row = G.newRow(i);
				for (int j: link.get(i).keySet())
					row.put(j, NOT_BORED / out[i]);
			}
		}
    }

	/** 
	 * Chooses a probability vector a, and repeatedly computes aP, aP^2, aP^3...
	 * until aP^i = aP^(i+1).
	 * 
	 * @param numberOfDocs Number of documents (size of matrix)
	 * @param maxIterations Maximum number of iterations
	 * 
	 * @return The resulting vector from power iteration
	 */
	private double[] iterate(int numberOfDocs, int maxIterations) {
		double[] a_old;
		double[] a = new double[numberOfDocs];
        a[0] = 1.0;

        int i = 0;
        double err = 10;
        while (err > EPSILON && i < maxIterations) {
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
	 * Writes the pageranks in vector a to disk
	 * 
	 * @param a A vector
	 */
	static void writePageRanks(String[] docNames, double[] a) throws IOException {

		/** 1;blabla.f */
		HashMap<String, String> realDocNames = new HashMap<>();
        File file = new File(INDEXDIR + "/davisTitles.txt");
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                realDocNames.put(data[0], data[1]);
            }
        }
		freader.close();


        FileOutputStream fout = new FileOutputStream(INDEXDIR + "/pageranks");
        for (int i = 0; i < a.length; i++) {
            String docName = realDocNames.get(docNames[i]);
            String docInfoEntry = docName + ";" + a[i] + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }

    /**
     * Reads the document names and document lengths from file, and put them in the
     * appropriate data structures.
     *
     * @throws IOException { exception_description }
     */
    static HashMap<String, Double> readDocInfo() throws IOException {
		HashMap<String, Double> pageranks = new HashMap<>();

        File file = new File(INDEXDIR + "/pageranks");
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                pageranks.put(data[0], Double.parseDouble(data[1]));
            }
        }
		freader.close();
		
		return pageranks;
    }
	/**
	 * Left multiplies a vector with a matrix
	 * 
	 * @param vec A one dimensional vector
	 * @param G A sparse vector
	 * 
	 * @return The product of vec*G
	 */
	private static double[] multiply(double[] vec, Sparse G) {
		double[] prod = new double[vec.length];

		for (int j = 0; j < vec.length; j++) {
			if (G.mtx.containsKey(j)) {
				LinkedHashMap<Integer, Double> row = G.mtx.get(j);
				for (Map.Entry<Integer, Double> e: row.entrySet()) {
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
        for (int i = 0; i < a.length; i++){
			norm += a[i];
		}
		
		for (int i = 0; i < a.length; i++){
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
        
        for (int i = 0; i < a.length; i++){
            alignment += Math.pow(a[i] - b[i], 2);
        }

        alignment = Math.sqrt(alignment);

        return alignment;
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