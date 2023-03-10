/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query implements Cloneable {

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    static class QueryTerm {
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
    static final double ALPHA = 1.0;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    static final double BETA = 0.75;
    
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

        /* Isolate relevant indices */
        ArrayList<Integer> relevantIndices = new ArrayList<>();
        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) relevantIndices.add(i);
        }

        /* If no documents to get relevance feedback from, do nothing */
        if (relevantIndices.isEmpty()) return;

        HashMap<String, Integer> tknToIdx = new HashMap<>();

        /* Multiply q0 by alpha and its idf */
        for (int i = 0; i < size(); i++) {
            queryterm.get(i).weight *= ALPHA;
            queryterm.get(i).weight *= Math.log( engine.index.docLengths.size() * 1.0 / engine.index.getPostings(queryterm.get(i).term).size());
            tknToIdx.put(queryterm.get(i).term, i);
        }

        try {
            for (int i: relevantIndices) {

                ArrayList<QueryTerm> di = new ArrayList<>();

                /* Read each file and get each token */
                int docID = results.get(i).docID;
                String fileName = engine.index.docNames.get(docID);

                Reader reader = new InputStreamReader( new FileInputStream(new File(fileName)), StandardCharsets.UTF_8 );
                Tokenizer tok = new Tokenizer( reader, true, false, true, engine.patterns_file );

                while ( tok.hasMoreTokens() ) {
                    String token = tok.nextToken();
                    di.add(new QueryTerm(token, 1.0));
                }

                /* Calculate score foreach token */
                for (int j = 0; j < di.size(); j++) {
                    // di.get(j).weight *= ( (BETA / relevantIndices.size()) * (1.0 / (engine.index.docLengths.get(docID))) * Math.log(Index.docNames.size() * 1.0 / engine.index.getPostings(di.get(j).term).size()) );
                    // di.get(j).weight *= ( (BETA / relevantIndices.size()) * Math.log(Index.docNames.size() * 1.0 / engine.index.getPostings(di.get(j).term).size()) * 1.0 / Math.sqrt(engine.index.getPostings(di.get(j).term).size()) );
                    di.get(j).weight *= ( (BETA / relevantIndices.size()) * Math.log(Index.docNames.size() * 1.0 / engine.index.getPostings(di.get(j).term).size()) );
                    // di.get(j).weight *= ( (BETA / relevantIndices.size()) * (1.0 / (engine.index.docLengths.get(docID))));
                }

                /* Filter tokens and merge scores for same tokens */
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
//            for (QueryTerm qt: queryterm) {
//                System.err.println("Term: " + qt.term + ", weight: " + qt.weight);
//            }
            // normalize();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Determines whether the query contains wildcards
     * @return a boolean on if the query contains wildcards or not
     */
    boolean containsWildcards() {
        for (QueryTerm qt: queryterm) {
            if (qt.term.contains("*")) return true;
        }
        return false;
    }

    /**
     * Gets a list of queries that this wildcard query consists of
     * @param kgIndex The kgram index
     * @return a list of queries based on the wildcards
     */
    List<Query> getWildcardQueries(KGramIndex kgIndex) {
        ArrayList<QueryTerm> placeholder = new ArrayList<>();

        ArrayList<KGramIndex.Pair<Integer, String>> wildcardIndexes = new ArrayList<KGramIndex.Pair<Integer, String>>();
        ArrayList<KGramIndex.Pair<Integer, List<String>>> analysedWildcards = new ArrayList<>();

        /* Comb through original query */
        int i = 0;
        for (QueryTerm qt: queryterm) {
            if (qt.term.contains("*")) {
                placeholder.add(new QueryTerm("", 1.0));
                wildcardIndexes.add(new KGramIndex.Pair<Integer, String>(i, qt.term));
            }
            else
                placeholder.add(new QueryTerm(qt.term, qt.weight));
            i++;
        }

        /* Find all wildcards */
        for (i = 0; i < wildcardIndexes.size(); i++) {
            analysedWildcards.add(new KGramIndex.Pair<>(wildcardIndexes.get(i).first, kgIndex.getWildcards(wildcardIndexes.get(i).second)));
        }

        List<Query> queries = new ArrayList<>();

        Query q = new Query();
        q.queryterm = placeholder;
        queries.add(q);

        /* Actually construct all wildcard queries */
        for (i = 0; i < analysedWildcards.size(); i++) {
            int idx = analysedWildcards.get(i).first;
            List<String> wildcards = analysedWildcards.get(i).second;
            List<Query> newQueries = new ArrayList<>();
            ((ArrayList<Query>) newQueries).ensureCapacity(queries.size() * wildcards.size());

            for (Query query: queries) {
                for (String s: wildcards) {
                    ArrayList<QueryTerm> qt = new ArrayList<QueryTerm>(query.queryterm);
                    qt.set(idx, new QueryTerm(s, 1.0));

                    Query newQuery = new Query();
                    newQuery.queryterm = qt;
                    newQueries.add(newQuery);
                }
            }
            queries = newQueries;
        }

        return queries;
    }

    /**
     * Calculates a single query containing all wildcards for this query
     * @param kgIndex The kgram index
     * @return A single query whose queryterm contains all wildcard tokens
     */
    Query getWildcards(KGramIndex kgIndex) {

        Query q = new Query();

        ArrayList<String> wildcardsToSearch = new ArrayList<>();

        /* Comb through original query */
        for (QueryTerm qt: queryterm) {
            if (qt.term.contains("*")) {
                wildcardsToSearch.add(qt.term);
            }
            else
                q.queryterm.add(new QueryTerm(qt.term, 1.0));
        }

        /* Find all wildcards and append them to the query */
        for (int i = 0; i < wildcardsToSearch.size(); i++) {
            List<String> wildcards = kgIndex.getWildcards(wildcardsToSearch.get(i));

            for (String s: wildcards) {
                q.queryterm.add(new QueryTerm(s, 1.0));
            }
        }

        return q;
    }
}


