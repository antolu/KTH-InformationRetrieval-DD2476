/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.nio.charset.*;
import java.io.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query implements Cloneable {

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
        String term;
        double weight;
        QueryTerm( String t, double w ) {
            term = t;
            weight = w;
        }
    }

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    static final double ALPHA = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    static double BETA = 1 - ALPHA;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
        StringTokenizer tok = new StringTokenizer( queryString );
        while ( tok.hasMoreTokens() ) {
            queryterm.add( new QueryTerm(tok.nextToken(), 1.0) );
        }    
    }
    
    
    /**
     *  Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for ( QueryTerm t : queryterm ) {
            len += t.weight; 
        }
        return len;
    }

    public void normalize() {

        for (int i = 0; i < size(); i++) {
            queryterm.get(i).weight /= size();
        }
    }
    
    /**
     *  Returns a copy of the Query
     */
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        Query queryCopy = new Query();
        for ( QueryTerm t : queryterm ) {
            queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
        }

        return queryCopy;
    }
    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {
        normalize();

        HashMap<String, Integer> tknToIdx = new HashMap<>();

        /* Multiply q0 by alpha */
        for (int i = 0; i < size(); i++) {
            queryterm.get(i).weight *= ALPHA;
            tknToIdx.put(queryterm.get(i).term, i);
        }

        ArrayList<Integer> relevantIndices = new ArrayList<>();
        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) relevantIndices.add(i);
        }

        try {
            for (int i: relevantIndices) {

                ArrayList<QueryTerm> di = new ArrayList<>();

                int docID = results.get(i).docID;
                String fileName = engine.index.docNames.get(docID);

                Reader reader = new InputStreamReader( new FileInputStream(new File(fileName)), StandardCharsets.UTF_8 );
                Tokenizer tok = new Tokenizer( reader, true, false, true, engine.patterns_file );
                int offset = 0;
                while ( tok.hasMoreTokens() ) {
                    String token = tok.nextToken();
                    di.add(new QueryTerm(token, 1.0));
                }

                /* Normalize */
                for (int j = 0; j < di.size(); j++) {
                    di.get(j).weight *= ( (BETA / relevantIndices.size()) * (1.0 / (engine.index.docLengths.get(docID))) );
                }

                for (int j = 0; j < di.size(); j++) {
                    QueryTerm qt = di.get(j);
                    if (!tknToIdx.containsKey(qt.term)) {

                        queryterm.add(qt);
                        tknToIdx.put(qt.term, size() - 1);
                    } else {
                        queryterm.get(tknToIdx.get(qt.term)).weight += qt.weight;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


