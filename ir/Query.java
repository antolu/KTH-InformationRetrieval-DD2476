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
    static final double ALPHA = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    static final double BETA = 1 - ALPHA;
    
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

    boolean containsWildcards() {
        for (QueryTerm qt: queryterm) {
            if (qt.term.contains("*")) return true;
        }
        return false;
    }

    List<Query> getWildcardQueries(KGramIndex kgIndex) {

        List<Query> queries = new ArrayList<>();

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

        /* Actually construct all wildcard queries */
        for (i = 0; i < analysedWildcards.size(); i++) {
            int idx = analysedWildcards.get(i).first;
            List<String> wildcards = analysedWildcards.get(i).second;

            for (String s: wildcards) {
                ArrayList<QueryTerm> qt = new ArrayList<QueryTerm>(placeholder);
                qt.set(idx, new QueryTerm(s, 1.0));

                Query newQuery = new Query();
                newQuery.queryterm = qt;
                queries.add(newQuery);
            }
        }

        return queries;
    }

    Query getWildcards(KGramIndex kgIndex) {

        Query q = new Query();

        ArrayList<String> wildcardsToSearch = new ArrayList<>();

        /* Comb through original query */
        int i = 0;
        for (QueryTerm qt: queryterm) {
            if (qt.term.contains("*")) {
                wildcardsToSearch.add(qt.term);
            }
            else
                q.queryterm.add(new QueryTerm(qt.term, 1.0));
            i++;
        }

        /* Find all wildcards and append them to the query */
        for (i = 0; i < wildcardsToSearch.size(); i++) {
            List<String> wildcards = kgIndex.getWildcards(wildcardsToSearch.get(i));

            for (String s: wildcards) {
                q.queryterm.add(new QueryTerm(s, 1.0));
            }
        }

        return q;
    }
}


