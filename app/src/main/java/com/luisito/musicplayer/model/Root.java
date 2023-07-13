package com.luisito.musicplayer.model;

import java.util.ArrayList;

public class Root {
    private int resultCount = 0;
    private ArrayList<Result> results = null;


    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public ArrayList<Result> getResults() {
        return results;
    }

    public void setResults(ArrayList<Result> results) {
        this.results = results;
    }
}
