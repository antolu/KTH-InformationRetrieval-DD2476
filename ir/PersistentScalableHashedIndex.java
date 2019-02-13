package ir;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

public class PersistentScalableHashedIndex extends PersistentHashedIndex {

    /** How many documents should be processed to invoke a merge. Default 8192 */
    private static final int INDEX_THRESHOLD = 1 << 13;

    /** Number of exported partial data files */
    private int noDataFiles = 0;

    /** Files to be berged into the main one */
    private final ArrayList<String> toMerge = new ArrayList<>();

    /** Keeps track of the last merged file */
    private String currentMergedFileID = "0";

    /** Keeps track of last document a partial index was written to */
    private int lastSavedID = 0;

    /** The threaded merge thread */
    private Thread t = new Thread();

    public PersistentScalableHashedIndex() {
        super();
    }

    /**
     * Writes a hashmap of token IDs and their address in dictionary to file
     * 
     * @param indexKeys The HashMap to be written to file
     * @param idx       The identifier of the file written to disk
     */
    private void writePartialTokens(HashMap<Integer, Integer> indexKeys, String idx) {
        try {
            FileOutputStream fout = new FileOutputStream(INDEXDIR + "/partialTokens" + idx);

            ByteBuffer buffer = ByteBuffer.allocate(indexKeys.size() * 4 * 2);

            for (Map.Entry<Integer, Integer> entry : indexKeys.entrySet()) {
                buffer.putInt(entry.getKey());
                buffer.putInt(entry.getValue());
            }
            buffer.flip();
            fout.write(buffer.array());
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads token IDs and their corresponding hash address in dictionary file idx
     * 
     * @param idx The index of the file to be read
     * 
     * @return A HashMap of tokenIDs and dictionary addresses
     */
    private HashMap<Integer, Integer> readPartialTokens(String idx) {
        try {
            RandomAccessFile file = new RandomAccessFile(INDEXDIR + "/partialTokens" + idx, "rw");

            byte[] bytes = new byte[(int) file.length()];
            file.readFully(bytes);

            ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            HashMap<Integer, Integer> indexKeys = new HashMap<>();

            for (int i = 0; i < bytes.length; i += 8)
                indexKeys.put(buffer.getInt(i), buffer.getInt(i + 4));

            file.close();
            return indexKeys;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Writes partial docInfo to file
     * 
     * @param app        The identifier of the file (appendix)
     * @param docNames   HashMap of document names
     * @param docLengths HashMap of document lengths
     */
    private void writePartialDocInfo(String app, HashMap<Integer, String> docNames,
            HashMap<Integer, Integer> docLengths) {
        try {
            FileOutputStream fout = new FileOutputStream(INDEXDIR + "/docInfo" + app);

            for (Map.Entry<Integer, String> entry : docNames.entrySet()) {
                Integer key = entry.getKey();
                String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
                fout.write(docInfoEntry.getBytes("UTF-8"));
            }
            docNames.clear();
            docLengths.clear();
            fout.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads docInfo from file
     * 
     * @param names   An empty HashMap for document names
     * @param lengths An empty HashMap for document lengths
     * @param idx     The index of the docInfo file to be read
     */
    protected void readDocInfo(HashMap<Integer, String> names, HashMap<Integer, Integer> lengths, String idx)
            throws IOException {
        File file = new File(INDEXDIR + "/docInfo" + idx);
        FileReader freader = new FileReader(file);

        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                names.put(new Integer(data[0]), data[1]);
                lengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }

    /**
     * Writes a partial index (dictionary + data) to disk, clears all indexes.
     */
    private void writePartialIndex() {
        System.err.println("[INDEX] Writing partial index " + noDataFiles + " to disk...");

        /** Write the 'docNames' and 'docLengths' hash maps to a file */
        writePartialDocInfo(Integer.toString(noDataFiles), docNames, docLengths);

        try {
            RandomAccessFile currentDataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + noDataFiles, "rw");
            RandomAccessFile currentDictionaryFile = new RandomAccessFile(
                    INDEXDIR + "/" + DICTIONARY_FNAME + noDataFiles, "rw");
            noDataFiles++;

            HashMap<Integer, Integer> dictionary = new HashMap<>();
            HashMap<Integer, Integer> writtenTokens = new HashMap<>();

            /** Write the dictionary and the postings list */
            for (Map.Entry<String, PostingsList> entry : index.entrySet()) {

                /** Get primary and secondary hash */
                int hash = Utils.hash(entry.getKey());
                int shash = Utils.reverseHash(entry.getKey());

                /** Write data to file */
                int size = writeData(currentDataFile, entry.getValue().toString(), free);

                /** Find a non-occupied address in dictionary */
                for (;;) {
                    if (dictionary.containsKey(hash)) {
                        hash++;
                        collisions++;
                        continue;
                    }
                    break;
                }

                dictionary.put(hash, Utils.improvedHash(entry.getKey()));
                writtenTokens.put(tokenIndex.get(entry.getKey()), hash);

                writeEntry(currentDictionaryFile, new Entry(free, size, shash), hash);
                free += size;
                // System.err.println(entry.getKey());
            }

            /** Save which tokens this datafile contains */
            writePartialTokens(writtenTokens, Integer.toString(noDataFiles - 1));

            /** Add data file to merge queue if it's not the first one */
            if (noDataFiles - 1 != 0)
                toMerge.add(Integer.toString(noDataFiles - 1));

            System.err.println("[INDEX] Written partial index " + (noDataFiles - 1) + " to file");

            /** Start merge if more than two partial datafiles available */
            if (!toMerge.isEmpty() && !t.isAlive()) {
                String idx = toMerge.get(0);
                toMerge.remove(0);

                t = new Thread() {
                    public void run() {
                        mergeIndexes(idx);
                    }
                };
                t.start();
                // mergeIndexes(processedFiles++, index1, index2, data1, data2);
            }

            /** Reset for next write */
            index.clear();
            free = 0L;

            currentDataFile.close();
            currentDictionaryFile.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Merges the main datafile with next partial datafile
     * 
     * @param idx2 The file to be merged into the main datafile
     */
    private synchronized void mergeIndexes(String idx2) {
        System.err.println("[MERGE] Starting merge of " + currentMergedFileID + " and " + idx2);

        String mergedName = currentMergedFileID + idx2;

        /** First merge docInfo */
        HashMap<Integer, String> names1 = new HashMap<>();
        HashMap<Integer, Integer> lengths1 = new HashMap<>();

        HashMap<Integer, String> names2 = new HashMap<>();
        HashMap<Integer, Integer> lengths2 = new HashMap<>();

        try {
            readDocInfo(names1, lengths1, currentMergedFileID);
            readDocInfo(names2, lengths2, idx2);

            HashMap<Integer, String> mergedNames = new HashMap<>();
            HashMap<Integer, Integer> mergedLengths = new HashMap<>();

            mergedNames.putAll(names1);
            mergedNames.putAll(names2);
            mergedLengths.putAll(lengths1);
            mergedLengths.putAll(lengths2);

            writePartialDocInfo(mergedName, mergedNames, mergedLengths);

        } catch (IOException e) {
            e.printStackTrace();
        }

        /** Read index and data files */
        HashMap<Integer, Integer> indexKeys1 = readPartialTokens(currentMergedFileID);
        HashMap<Integer, Integer> indexKeys2 = readPartialTokens(idx2);
        HashMap<Integer, Integer> dict = new HashMap<>();

        HashMap<Integer, Integer> mergedTokenIndex = new HashMap<>();

        try {
            /** Open all files to read/write */
            RandomAccessFile index1 = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME + currentMergedFileID,
                    "rw");
            RandomAccessFile index2 = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME + idx2, "rw");
            RandomAccessFile data1 = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + currentMergedFileID, "rw");
            RandomAccessFile data2 = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + idx2, "rw");

            RandomAccessFile mergedFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + mergedName, "rw");
            RandomAccessFile mergedDict = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME + mergedName, "rw");

            long ptr = 0L;

            /** Compare tokens in main data file and second (smaller) one */
            for (int id1 : indexKeys1.keySet()) {
                if (indexKeys2.containsKey(id1)) { // Both files have the same token, merge lists
                    /** Get their respective entries in each dict */
                    int hash1 = indexKeys1.get(id1);
                    int hash2 = indexKeys2.get(id1);

                    Entry entry1 = readEntry(index1, hash1);
                    Entry entry2 = readEntry(index2, hash2);

                    String d1 = readData(data1, entry1.start, entry1.size);
                    String d2 = readData(data2, entry2.start, entry2.size);

                    String mergedData = d1 + PostingsList.ENTRY_DELIM + d2;

                    int size = writeData(mergedFile, mergedData, ptr);

                    /** Write to new file with hash of first one */
                    for (;;) {
                        if (dict.containsKey(hash1)) {
                            hash1++;
                            collisions++;
                            continue;
                        }
                        dict.put(hash1, id1);
                        mergedTokenIndex.put(id1, hash1);
                        break;
                    }

                    writeEntry(mergedDict, new Entry(ptr, size, entry1.shash), hash1);
                    ptr += size;
                } else { // Token from file1 nexiste in file2, only write data from file2
                    int hash1 = indexKeys1.get(id1);
                    Entry entry1 = readEntry(index1, hash1);
                    String d1 = readData(data1, entry1.start, entry1.size);

                    int size = writeData(mergedFile, d1, ptr);

                    /** Write to new file with hash of first one */
                    for (;;) {
                        if (dict.containsKey(hash1)) {
                            hash1++;
                            collisions++;
                            continue;
                        }
                        dict.put(hash1, id1);
                        mergedTokenIndex.put(id1, hash1);
                        break;
                    }

                    writeEntry(mergedDict, new Entry(ptr, size, entry1.shash), hash1);
                    ptr += size;
                }
            }

            /** Write the rest of entries from file2 to merged */
            for (int id2 : indexKeys2.keySet()) {
                if (!mergedTokenIndex.containsKey(id2)) {
                    int hash2 = indexKeys2.get(id2);
                    Entry entry2 = readEntry(index2, hash2);
                    String d2 = readData(data2, entry2.start, entry2.size);

                    int size = writeData(mergedFile, d2, ptr);

                    /** Write to new file with hash of first one */
                    for (;;) {
                        if (dict.containsKey(hash2)) {
                            hash2++;
                            collisions++;
                            continue;
                        }
                        dict.put(hash2, id2);
                        mergedTokenIndex.put(id2, hash2);
                        break;
                    }

                    writeEntry(mergedDict, new Entry(ptr, size, entry2.shash), hash2);
                    ptr += size;
                }
            }

            /** Save the new index */
            writePartialTokens(mergedTokenIndex, mergedName);

            /** Delete the read files */
            data1.close();
            data2.close();
            index1.close();
            index2.close();
            mergedFile.close();
            mergedDict.close();

            String[] toDelete = { DATA_FNAME + currentMergedFileID, DATA_FNAME + idx2,
                    DICTIONARY_FNAME + currentMergedFileID, DICTIONARY_FNAME + idx2, "docInfo" + currentMergedFileID,
                    "docInfo" + idx2, "partialTokens" + currentMergedFileID, "partialTokens" + idx2 };
            for (String s : toDelete) {
                File file = new File(INDEXDIR + "/" + s);
                file.delete();
            }

            System.err.println("[MERGE] Finished merge of " + currentMergedFileID + " and " + idx2);
            currentMergedFileID = mergedName;
            return;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (DataFormatException ex) {
            ex.printStackTrace();
            System.exit(1);
        } catch (IOException ioex) {
            ioex.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void insert(String token, int docID, int offset) {
        if (docID % INDEX_THRESHOLD == 0 && docID != lastSavedID) {
            writePartialIndex();
            lastSavedID = docID;
        }

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

    @Override
    public void cleanup() {
        
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.err.println("[INFO] Writing last partial index to disk...");
        writePartialIndex();

        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.err.println("[INFO] Running final disk merges...");
        while (!toMerge.isEmpty()) {

            String idx2 = toMerge.get(0);
            toMerge.remove(0);

            mergeIndexes(idx2);
        }

        System.err.println("[INFO] Number of unique tokens: " + noUniqueTokens);
        System.err.println("[INFO]" + collisions + " collisions.");

        System.err.println("[INFO] Moving files into place...");

        try {

            /** re-point the index files */
            dataFile.close();
            dictionaryFile.close();

            /** Delete last index file */
            File file = new File(INDEXDIR + "/partialTokens" + currentMergedFileID);
            file.delete();

            /** Rename files */
            File dataFrom = new File(INDEXDIR + "/" + DATA_FNAME + currentMergedFileID);
            File dataTo = new File(INDEXDIR + "/" + DATA_FNAME);
            dataTo.delete();
            dataFrom.renameTo(dataTo);
            dataFrom.delete();

            File dictionaryFrom = new File(INDEXDIR + "/" + DICTIONARY_FNAME + currentMergedFileID);
            File dictionaryTo = new File(INDEXDIR + "/" + DICTIONARY_FNAME);
            dictionaryTo.delete();
            dictionaryFrom.renameTo(dictionaryTo);
            dictionaryFrom.delete();

            File docInfoFrom = new File(INDEXDIR + "/" + DOCINFO_FNAME + currentMergedFileID);
            File docInfoTo = new File(INDEXDIR + "/" + DOCINFO_FNAME);
            docInfoFrom.renameTo(docInfoTo);
            readDocInfo();

            /** Reopen files */
            dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME, "rw");
            dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME, "rw");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.err.println("[SUCCESS] Done!");
    }
}