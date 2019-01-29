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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class PersistentScalableHashedIndex extends PersistentHashedIndex {

    private static final int INDEX_THRESHOLD = 1 << 12;

    private int noDataFiles = 0;
    private int processedFiles = 1;

    private ArrayList<RandomAccessFile> dataFiles = new ArrayList<>();
    private ArrayList<RandomAccessFile> dictionaryFiles = new ArrayList<>();
    private ArrayList<String> indexKeyNames = new ArrayList<>();

    private RandomAccessFile currentDataFile;
    private RandomAccessFile currentDictionaryFile;

    private String currentMergedFile = "0";
    private int lastSavedID = 0;

    private Thread t = new Thread();

    public PersistentScalableHashedIndex() {
        super();
    }

    /**
     * Writes data to the data file at a specified place.
     *
     * @return The number of bytes written.
     */
    int writeData(RandomAccessFile file, String dataString, long ptr) {
        try {
            file.seek(ptr);
            byte[] data = dataString.getBytes();
            file.write(data);
            return data.length;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Reads data from the data file
     * 
     * DONE
     */
    String readData(RandomAccessFile file, long ptr, int size) {
        try {
            file.seek(ptr);
            byte[] data = new byte[size];
            file.readFully(data);
            return new String(data);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Writes an entry to the dictionary hash table file.
     * 
     * DONE
     *
     * @param entry The key of this entry is assumed to have a fixed length
     * 
     * @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry(RandomAccessFile file, Entry entry, long ptr) {
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
     * DONE
     *
     * @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry(RandomAccessFile file, long ptr) throws DataFormatException {
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

    private void writeIndexKeys(HashMap<Integer, Integer> indexKeys, String idx) {
        try {
            FileOutputStream fout = new FileOutputStream(INDEXDIR + "/indexKeys" + idx);

            ByteBuffer buffer = ByteBuffer.allocate(indexKeys.size() * 4 * 2);

            for (Map.Entry<Integer, Integer> entry : indexKeys.entrySet()) { // Invert
                buffer.putInt(entry.getValue());
                buffer.putInt(entry.getKey());
            }
            buffer.flip();
            fout.write(buffer.array());
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<Integer, Integer> readIndexKeys(String idx) {
        try {
            RandomAccessFile file = new RandomAccessFile(INDEXDIR + "/indexKeys" + idx, "rw");

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

    private void writePartialDocInfo(String app, HashMap<Integer, String> docNames,
            HashMap<Integer, Integer> docLengths) {
        try {
            FileOutputStream fout = new FileOutputStream(INDEXDIR + "/docInfo" + app);

            for (Map.Entry<Integer, String> entry : docNames.entrySet()) {
                Integer key = entry.getKey();
                String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
                fout.write(docInfoEntry.getBytes());
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

    private void writePartialIndex() {
        System.err.println("Writing partial index " + noDataFiles + " to disk...");

        // Write the 'docNames' and 'docLengths' hash maps to a file
        writePartialDocInfo(Integer.toString(noDataFiles), docNames, docLengths);

        try {
            currentDataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + noDataFiles, "rw");
            currentDictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME + noDataFiles, "rw");
            dataFiles.add(currentDataFile);
            dictionaryFiles.add(currentDictionaryFile);
            noDataFiles++;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        HashMap<Integer, Integer> dictionary = new HashMap<>();

        // Write the dictionary and the postings list
        for (Map.Entry<String, PostingsList> entry : index.entrySet()) {

            int hash = Utils.hash(entry.getKey());
            int shash = Utils.reverseHash(entry.getKey());

            int size = writeData(currentDataFile, entry.getValue().toString(), free);
            for (;;) {
                if (dictionary.containsKey(hash)) {
                    hash++;
                    collisions++;
                    continue;
                }
                dictionary.put(hash, entry.getKey().hashCode());
                break;
            }
            writeEntry(currentDictionaryFile, new Entry(free, size, shash), hash);
            free += size;
            // System.err.println(entry.getKey());
        }

        writeIndexKeys(dictionary, Integer.toString(noDataFiles - 1));

        System.err.println("Written partial index " + (noDataFiles-1) + " to file");

        if (dataFiles.size() >= 2 && !t.isAlive()) {
            RandomAccessFile index1 = dictionaryFiles.get(0);
            RandomAccessFile index2 = dictionaryFiles.get(1);
            dictionaryFiles.remove(0);
            dictionaryFiles.remove(0);

            RandomAccessFile data1 = dataFiles.get(0);
            RandomAccessFile data2 = dataFiles.get(1);
            dataFiles.remove(0);
            dataFiles.remove(0);

            System.err.println("Starting merge of " + currentMergedFile + " and " + processedFiles);
            // t = new Thread() {
            //     public void run() {
            //         mergeIndexes(processedFiles++, index1, index2, data1, data2);
            //     }
            // };
            // t.start();
            mergeIndexes(processedFiles++, index1, index2, data1, data2);
        }

        /** Reset for next write */
        index.clear();
        free = 0L;
    }

    private void mergeIndexes(int idx2, RandomAccessFile index1, RandomAccessFile index2,
            RandomAccessFile data1, RandomAccessFile data2) {
        String idx2String = Integer.toString(idx2);
        String mergedName = currentMergedFile + idx2String;

        /** First merge docInfo */
        HashMap<Integer, String> names1 = new HashMap<>();
        HashMap<Integer, Integer> lengths1 = new HashMap<>();

        HashMap<Integer, String> names2 = new HashMap<>();
        HashMap<Integer, Integer> lengths2 = new HashMap<>();

        try {
            readDocInfo(names1, lengths1, currentMergedFile);
            readDocInfo(names2, lengths2, idx2String);

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
        HashMap<Integer, Integer> indexKeys1 = readIndexKeys(currentMergedFile);
        HashMap<Integer, Integer> indexKeys2 = readIndexKeys(idx2String);
        HashMap<Integer, Integer> dict = new HashMap<>();
        HashMap<Integer, Integer> reverseDict = new HashMap<>();

        try {
            RandomAccessFile mergedFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + mergedName, "rw");
            RandomAccessFile mergedDict = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME + mergedName, "rw");
            long ptr = 0L;

            for (int origHash1 : indexKeys1.keySet()) {
                if (indexKeys2.containsKey(origHash1)) { // Both files have the same token, merge lists
                    /** Get their respective entries in each dict */
                    int hash1 = indexKeys1.get(origHash1);
                    int hash2 = indexKeys2.get(origHash1);

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
                        dict.put(hash1, origHash1);
                        reverseDict.put(origHash1, hash1);
                        break;
                    }

                    writeEntry(mergedDict, new Entry(ptr, size, entry1.shash), hash1);
                    ptr += size;
                } else { // Token from file1 nexiste in file2, only write data from file2
                    int hash1 = indexKeys1.get(origHash1);
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
                        dict.put(hash1, origHash1);
                        reverseDict.put(origHash1, hash1);
                        break;
                    }


                    writeEntry(mergedDict, new Entry(ptr, size, entry1.shash), hash1);
                    ptr += size;
                }
            }

            /** Write the rest of entries from file2 to merged */
            for (int origHash2 : indexKeys2.keySet()) {
                if (!reverseDict.containsKey(origHash2)) {
                    int hash2 = indexKeys2.get(origHash2);
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
                        dict.put(hash2, origHash2);
                        reverseDict.put(origHash2, hash2);
                        break;
                    }

                    writeEntry(mergedDict, new Entry(ptr, size, entry2.shash), hash2);
                    ptr += size;
                }
            }

            /** Save the new index */
            writeIndexKeys(dict, mergedName);

            /** Prepare for next sorting */
            dataFiles.add(0, mergedFile);
            dictionaryFiles.add(0, mergedDict);

            /** Delete the read files */
            data1.close();
            data2.close();
            index1.close();
            index2.close();

            String[] toDelete = { DATA_FNAME + currentMergedFile, DATA_FNAME + idx2String,
                    DICTIONARY_FNAME + currentMergedFile, DICTIONARY_FNAME + idx2String, "docInfo" + currentMergedFile,
                    "docInfo" + idx2String, "indexKeys" + currentMergedFile, "indexKeys" + idx2String };
            for (String s : toDelete) {
                File file = new File(INDEXDIR + "/" + s);
                file.delete();
            }

            System.err.println("Finished merge of " + currentMergedFile + " and " + idx2String);
            currentMergedFile = mergedName;
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
    }

    @Override
    public void cleanup() {
        /** Write last index */
        System.err.println("Writing last partial index to disk...");
        writePartialIndex();

        // System.err.println("Waiting for current disk merge to complete...");
        // try {
        //     t.join();
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }

        System.err.println("Running final disk merges...");
        while (dataFiles.size() > 1) {
            RandomAccessFile index1 = dictionaryFiles.get(0);
            RandomAccessFile index2 = dictionaryFiles.get(1);
            dictionaryFiles.remove(0);
            dictionaryFiles.remove(0);

            RandomAccessFile data1 = dataFiles.get(0);
            RandomAccessFile data2 = dataFiles.get(1);
            dataFiles.remove(0);
            dataFiles.remove(0);

            mergeIndexes(processedFiles++, index1, index2, data1, data2);
        }

        System.err.println(collisions + " collisions.");

        System.err.println("Moving files into place...");

        try {

            /** re-point the index files */
            dataFiles.get(0).close();
            dictionaryFiles.get(0).close();

            dataFile.close();
            dictionaryFile.close();

            /** Delete last index file */
            File file = new File(INDEXDIR + "/indexKeys" + currentMergedFile);
            file.delete();

            /** Rename files */
            File dataFrom = new File(INDEXDIR + "/" + DATA_FNAME + currentMergedFile);
            File dataTo = new File(INDEXDIR + "/" + DATA_FNAME);
            dataTo.delete();
            dataFrom.renameTo(dataTo);
            dataFrom.delete();

            File dictionaryFrom = new File(INDEXDIR + "/" + DICTIONARY_FNAME + currentMergedFile);
            File dictionaryTo = new File(INDEXDIR + "/" + DICTIONARY_FNAME);
            dictionaryTo.delete();
            dictionaryFrom.renameTo(dictionaryTo);
            dictionaryFrom.delete();

            File docInfoFrom = new File(INDEXDIR + "/" + DOCINFO_FNAME + currentMergedFile);
            File docInfoTo = new File(INDEXDIR + "/" + DOCINFO_FNAME);
            docInfoFrom.renameTo(docInfoTo);
            docInfoFrom.delete();
            readDocInfo();

            /** Reopen files */
            dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME, "rw");
            dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME, "rw");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.err.println("Done!");
    }
}