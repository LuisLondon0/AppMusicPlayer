package com.luisito.musicplayer.services;

import android.util.Log;

import java.net.URL;

import cafsoft.foundation.HTTPURLResponse;
import cafsoft.foundation.URLComponents;
import cafsoft.foundation.URLQueryItem;
import cafsoft.foundation.URLSession;

public class MusicService {
    private URLComponents components = null;

    public MusicService(){
        components = new URLComponents();
        components.setScheme("https");
        components.setHost("itunes.apple.com");
        components.setPath("/search");
    }

    public void searchSongsByTerm(String searchTerm, int limit, OnDataResponse delegate){
        components.setQueryItems(new URLQueryItem[]{
                new URLQueryItem("media", "music"),
                new URLQueryItem("entity", "song"),
                new URLQueryItem("limit", String.valueOf(limit)),
                new URLQueryItem("term", searchTerm)
        });

        URL url = components.getURL();
        Log.d("MusicService", url.toString());

        URLSession.getShared().dataTask(url, (data, response, error) -> {
            HTTPURLResponse resp = (HTTPURLResponse) response;
            int statusCode = -1;
            String text = null;

            if (error == null && resp.getStatusCode() == 200){
                text = data.toText();
                //Log.d("Petition", text);
                statusCode = resp.getStatusCode();
            }

            if (delegate != null){
                delegate.onChange(error != null, statusCode, text);
            }
        }).resume();
    }

    public interface OnDataResponse {
        void onChange(boolean isNetworkError, int statusCode, String response);
    }
}
