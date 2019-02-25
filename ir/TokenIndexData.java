package ir;

import java.util.LinkedHashMap;

public class TokenIndexData implements Comparable<TokenIndexData>{
    public String token;
    public int order;
    public PostingsList postingsList;
    public LinkedHashMap<Integer, Integer> indexes;

    public TokenIndexData(String token, int order, PostingsList postingsList) {
        this.token = token;
        this.order = order;
        this.postingsList = postingsList;
    }

    @Override
    public int compareTo(TokenIndexData o) {
        return postingsList.compareTo(o.postingsList);
    }
}