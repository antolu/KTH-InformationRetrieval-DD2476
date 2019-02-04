package ir;

import java.util.LinkedHashMap;

public class PhraseToken implements Comparable<PhraseToken>{
    public String token;
    public int order;
    public PostingsList postingsList;
    public LinkedHashMap<Integer, Integer> indexes;

    public PhraseToken(String token, int order, PostingsList postingsList) {
        this.token = token;
        this.order = order;
        this.postingsList = postingsList;
    }

    @Override
    public int compareTo(PhraseToken o) {
        return postingsList.compareTo(o.postingsList);
    }
}