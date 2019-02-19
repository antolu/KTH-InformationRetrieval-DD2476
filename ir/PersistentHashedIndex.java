/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;
import java.lang.Long;
import java.lang.Integer;
import java.util.zip.DataFormatException;
import java.util.Arrays;
import java.nio.ByteBuffer;

/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The dictionary file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    // public static final long TABLESIZE = 3509s827L;
    public static final long TABLESIZE = 611953L;

    /** Byte size of a long */
    protected static final int ENTRY_SIZE = 16;

    protected static final ByteBuffer inBuffer = ByteBuffer.allocate(ENTRY_SIZE);
    protected static final ByteBuffer outBuffer = ByteBuffer.allocate(ENTRY_SIZE);

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    protected int collisions = 0;

    // HashMap<Integer, Long> dictionary = new HashMap<Integer, Long>();

    /** The cache as a main-memory hash map. */
    HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();
    protected int noUniqueTokens = 0;

    // ===================================================================

    /**
     * A helper class representing one entry in the dictionary hashtable.
     */
    public class Entry {
        public long start = 0L;
        public int size = 0;
        public int shash = 0;

        public Entry(long start, int size, int shash) {
            this(start, size);
            this.shash = shash;
        }

        public Entry(long start, int size) {
            this.start = start;
            this.size = size;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Entry))
                return false;
            return ((Entry) obj).shash == this.shash;
        }
    }

    // ==================================================================

    /**
     * Constructor. Opens the dictionary file and the data file. If these files
     * don't exist, they will be created.
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME, "rw");
            // dictionaryFile.setLength(TABLESIZE * (LONG_SIZE + INT_SIZE));
            dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
            readTokenIndex();
        } catch (FileNotFoundException e) {
        } catch (ArrayIndexOutOfBoundsException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }

        Utils.initialize();
    }

    /**
     * Writes data to the data file at a specified place.
     *
     * @return The number of bytes written.
     */
    protected int writeData(RandomAccessFile file, String dataString, long ptr) {
        try {
            file.seek(ptr);
            byte[] data = dataString.getBytes("UTF-8");
            file.write(data);
            return data.length;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Reads data from the data file
     */
    protected String readData(RandomAccessFile file, long ptr, int size) {
        try {
            file.seek(ptr);
            byte[] data = new byte[size];
            file.readFully(data);
            return new String(data, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ==================================================================
    //
    // Reading and writing to the dictionary file.

    /**
     * Writes an entry to the dictionary hash table file.
     *
     * @param entry The key of this entry is assumed to have a fixed length
     * 
     * @param ptr   The place in the dictionary file to store the entry
     */
    protected void writeEntry(RandomAccessFile file, Entry entry, long ptr) {
        ptr = ptr * (long) ENTRY_SIZE;
        outBuffer.putLong(0, entry.start);
        outBuffer.putInt(8, entry.size);
        outBuffer.putInt(12, entry.shash);

        try {
            file.seek(ptr);
            file.write(outBuffer.array());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(ptr);
            System.err.println(entry);
        }
    }

    /**
     * Reads an entry from the dictionary file.
     *
     * @param ptr The place in the dictionary file where to start reading.
     */
    protected Entry readEntry(RandomAccessFile file, long ptr) throws DataFormatException {
        ptr = ptr * (long) ENTRY_SIZE;
        byte[] bytes = new byte[ENTRY_SIZE];

        try {
            file.seek(ptr);
            file.readFully(bytes);
            inBuffer.put(bytes, 0, bytes.length);
            inBuffer.flip();
            long pos = inBuffer.getLong(0);
            int size = inBuffer.getInt(8);
            int shash = inBuffer.getInt(12);

            if (pos == 0L && size == 0) {
                throw new DataFormatException("Hash at location" + ptr + "does not exist.");
            }

            return new Entry(pos, size, shash);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // ==================================================================

    /**
     * Writes the document names and document lengths to file.
     *
     * @throws IOException { exception_description }
     */
    protected void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream(INDEXDIR + "/docInfo");
        for (Map.Entry<Integer, String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
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
    protected void readDocInfo() throws IOException {
        File file = new File(INDEXDIR + "/docInfo");
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(Integer.parseInt(data[0]), data[1]);
                docLengths.put(Integer.parseInt(data[0]), Integer.parseInt(data[2]));
            }
        }
        freader.close();
    }

    /**
     * Write the index to files.
     */
    public void writeIndex() {
        HashMap<Integer, Long> dictionary = new HashMap<Integer, Long>();
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            for (Map.Entry<String, PostingsList> entry : index.entrySet()) {

                int hash = Utils.hash(entry.getKey());

                int size = writeData(dataFile, entry.getValue().toString(), free);
                for (;;) {
                    if (dictionary.containsKey(hash)) {
                        hash++;
                        collisions++;
                        continue;
                    }
                    dictionary.put(hash, free);
                    break;
                }
                int shash = Utils.reverseHash(entry.getKey());
                writeEntry(dictionaryFile, new Entry(free, size, shash), hash);
                free += size;
                // System.err.println(entry.getKey());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        index.clear();
        System.err.println("[INFO]" + collisions + " collisions.");
    }

    /**
     * Writes the document names and document lengths to file.
     *
     * @throws IOException { exception_description }
     */
    protected void writeTokenIndex() throws IOException {
        FileOutputStream fout = new FileOutputStream(INDEXDIR + "/tokenIndex");
        for (Map.Entry<String, Integer> entry : tokenIndex.entrySet()) {
            String key = entry.getKey();
            String docInfoEntry = key + " " + entry.getValue() + "\n";
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
    protected void readTokenIndex() throws IOException {
        File file = new File(INDEXDIR + "/tokenIndex");
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(" ");
                tokenIndex.put(data[0], Integer.parseInt(data[1]));
            }
        }
        freader.close();
    }

    // ==================================================================

    /**
     * Returns the postings for a specific term, or null if the term is not in the
     * index.
     */
    public PostingsList getPostings(String token) {
        if (index.containsKey(token))
            return index.get(token);

        int hash = Utils.hash(token);
        int shash = Utils.reverseHash(token);

        Entry entry;

        for (;;) {
            try {
                entry = readEntry(dictionaryFile, hash++);
            } catch (DataFormatException e) {
                return null;
            }

            if (entry.shash == shash) {
                break;
            }
        }

        try {
            String postingsList = readData(dataFile, entry.start, entry.size);

            /** Parse string */
            // String[] postingsEntries = postingsList.split(":");
            ArrayList<String> postingsEntries = Utils.splitByDelim(postingsList, PostingsList.ENTRY_DELIM);

            PostingsList pl = new PostingsList();
            pl.ensureCapacity(postingsEntries.size());

            for (String e : postingsEntries) {
                ArrayList<String> entryData = Utils.splitByDelim(e, PostingsEntry.OFFSET_DELIM);

                PostingsEntry postingsEntry = new PostingsEntry(Integer.parseInt(entryData.get(0)),
                        Integer.parseInt(entryData.get(1)));
                postingsEntry.reserveOffsetCapacity(entryData.size());

                entryData.stream().skip(2).forEachOrdered(i -> {
                    postingsEntry.addPosition(Integer.parseInt(i));
                });
                pl.add(postingsEntry);
            }

            index.put(token, pl);

            return pl;
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Inserts this token in the main-memory hashtable.
     */
    public void insert(String token, int docID, int offset) {
        // A PostingsList does not exist
        if (!index.containsKey(token)) {
            // Add to general purpose index
            PostingsList list = new PostingsList();
            list.add(new PostingsEntry(docID, offset));
            index.put(token, list);

        } else {
            PostingsList list = index.get(token);

            /** Add to general index only if not does not exist already */
            if (list.get(list.size() - 1).docID != docID)
                list.add(new PostingsEntry(docID, offset));
            else if (list.get(list.size() - 1).docID == docID)
                list.get(list.size() - 1).addPosition(offset);
        }

        if (!tokenIndex.containsKey(token))
            tokenIndex.put(token, noUniqueTokens++);
    }

    /**
     * Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println("[INFO]" + index.keySet().size() + " unique words");
        System.err.print("[INDEX] Writing index to disk...");
        writeIndex();
        try {
            writeTokenIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println("[SUCCESS] Done!");
    }
}
