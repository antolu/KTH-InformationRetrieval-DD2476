package montecarlo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;

public class MonteCarlo {

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

	/** Number of walks */
	final static int NO_WALKS = 10000000;

	/* --------------------------------------------- */

	public MonteCarlo(String filename) {
		int noOfDocs = readDocs(filename);
		initiateProbabilityMatrix(noOfDocs);

		long startTime = System.nanoTime();
		ArrayList<double[]> results =  iterate(noOfDocs, NO_WALKS);
		long endTime = System.nanoTime();

		double duration = ((double)(endTime - startTime))/1000000000.0;
		System.err.printf("Duration: %fs%n", duration);
		// System.err.printf("Writing pageranks to file...%n");
		// try {
		// 	writePageranks(docName, pagerank);
		// } catch (IOException e) {
		// 	System.err.println("IOException! Write failed.");
		// }
		int i = 0;
		for (double[] res: results) {
			System.err.println("Monte Carlo " + ++i);
			displayTopResults(res);
			System.err.print("\n\n");
		}

		System.err.println("Done!");
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
				ArrayList<Integer> row = G.newRow(i);
				for (int j: link.get(i).keySet())
					row.add(j);
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
	private ArrayList<double[]> iterate(int numberOfDocs, int N) {
		ArrayList<double[]> results = new ArrayList<>();

		double[] mon1 = monteCarlo1(numberOfDocs, N);
		double[] mon2 = monteCarlo2(numberOfDocs, N);
		double[] mon4 = monteCarlo4(numberOfDocs, N);
		double[] mon5 = monteCarlo5(numberOfDocs, N);

		results.add(mon1);
		results.add(mon2);
		results.add(mon4);
		results.add(mon5);
		
        return results;
	}

	private double[] monteCarlo1(int numberOfDocs, int N) {
		double[] a = new double[numberOfDocs];

		Random rand = new Random();

		for (int i = 0; i < N; i++) {
			int start = rand.nextInt(numberOfDocs);
			randomWalk(a, start);
		}

		normalize(a);

		return a;
	}
	private void randomWalk(double[] a, int from) {

		Random rand = new Random();

		if (rand.nextDouble() > BORED) {
			if (!G.mtx.containsKey(from)) {
				a[from]++;
				return;
			}

			ArrayList<Integer> row = G.mtx.get(from);

			int next = row.get(rand.nextInt(row.size()));
	
			randomWalk(a, next);
			return;
		} else {
			a[from]++;
			return;
		}
	}

	private double[] monteCarlo2(int numberOfDocs, int N) {
		double a[] = new double[numberOfDocs];

		int m = N / numberOfDocs;
		int n = numberOfDocs;

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				randomWalk(a, i);
			}
		}

		normalize(a);

		return a;
	}

	private double[] monteCarlo4(int numberOfDocs, int N) {
		double a[] = new double[numberOfDocs];

		int m = N / numberOfDocs;
		int n = numberOfDocs;

		MutableInt totalVisited = new MutableInt();

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				randomWalkMod(a, i, totalVisited);
			}
		}

		for (int i = 0; i < numberOfDocs; i++) {
			a[i] /= totalVisited.i;
		}

		return a;
	}

	private void randomWalkMod(double[] a, int from, MutableInt totalVisited) {

		Random rand = new Random();
		totalVisited.i++;
		a[from]++;

		if (rand.nextDouble() > BORED) {
			if (!G.mtx.containsKey(from)) {
				return;
			}

			ArrayList<Integer> row = G.mtx.get(from);

			int next = row.get(rand.nextInt(row.size()));
	
			randomWalkMod(a, next, totalVisited);
			return;
		} else {
			return;
		}
	}

	private double[] monteCarlo5(int numberOfDocs, int N) {
		double[] a = new double[numberOfDocs];

		Random rand = new Random();

		MutableInt totalVisited = new MutableInt();

		for (int i = 0; i < N; i++) {
			int start = rand.nextInt(numberOfDocs);
			randomWalkMod(a, start, totalVisited);
		}

		for (int i = 0; i < numberOfDocs; i++) {
			a[i] /= totalVisited.i;
		}

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
	public static void writePageranks(String[] docNames, double[] a, String fileName) throws IOException {

		/** 1;blabla.f */
		HashMap<String, String> realDocNames = new HashMap<>();
        File file = new File(INDEXDIR + "/SvwikiTitles.txt");
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                realDocNames.put(data[0], data[1]);
            }
        }
		freader.close();


        FileOutputStream fout = new FileOutputStream(INDEXDIR + "/" + fileName);
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
    public static void readPageranks(HashMap<String, Double> map, String fileName) throws IOException {

        File file = new File(INDEXDIR + "/" + fileName);
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                map.put(data[0], Double.parseDouble(data[1]));
            }
        }
		freader.close();
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

	private class MutableInt {
		public int i = 0;
	}

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
		protected HashMap<Integer, ArrayList<Integer>> mtx = new HashMap<>();

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

		public ArrayList<Integer> newRow(int i) {
			ArrayList<Integer> row = new ArrayList<>();

			mtx.put(i, row);
			return row;
		}
	}

	/* --------------------------------------------- */

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Please give the name of the link file");
		} else {
			new MonteCarlo(args[0]);
		}
	}
}