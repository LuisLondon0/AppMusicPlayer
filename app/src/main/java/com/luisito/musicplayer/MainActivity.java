package com.luisito.musicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.luisito.musicplayer.model.Result;
import com.luisito.musicplayer.model.Root;
import com.luisito.musicplayer.services.MusicService;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import cafsoft.foundation.HTTPURLResponse;
import cafsoft.foundation.URLSession;

public class MainActivity extends AppCompatActivity {
    private MusicService musicService = null;
    private TextInputEditText term = null;
    private LinearLayout songsLayout = null;
    private MediaPlayer mediaPlayer = null;
    private Root root = null;
    private String currentFullFilename = "";
    private URLSession downloadImagesSession = null;
    private Gson gson = null;
    private Button btnSearch = null;
    private boolean isPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        musicService = new MusicService();
        downloadImagesSession = new URLSession();
        gson = new Gson();

        initViews();
        initEvents();
    }

    public void initViews() {
        term = findViewById(R.id.termInput);
        btnSearch = findViewById(R.id.searchButton);
        songsLayout = findViewById(R.id.songsLayout);
    }

    public void initEvents() {
        btnSearch.setOnClickListener(view -> searchSongs());
    }

    public void searchSongs() {
        songsLayout.removeAllViews();
        musicService.searchSongsByTerm(term.getText().toString(), 50, ((isNetworkError, statusCode, response) -> {
            switch (statusCode) {
                case -1:
                    Log.d("BUSQUEDA", "Error -1, " + isNetworkError);
                    Log.d("BUSQUEDA", "Error -1, " + term.getText().toString());
                    break;

                case 404:
                    Log.d("BUSQUEDA", "Termino -" + term.getText().toString() + "- no encontrado");
                    break;

                case 200:
                    root = gson.fromJson(response, Root.class);

                    Log.d("CREADO", "Número de resultados: " + root.getResultCount());

                    for (Result result : root.getResults()) {
                        Log.d("CREADO", "CANCIÓN: " + result.getTrackName() + " - CANTANTE: " + result.getArtistName());

                        runOnUiThread(() -> {
                            View songRow = LayoutInflater.from(this).inflate(R.layout.song_row, null, false);

                            ImageView artwork = songRow.findViewById(R.id.artwork);
                            TextView trackName = songRow.findViewById(R.id.trackName);
                            TextView artistName = songRow.findViewById(R.id.artistName);
                            ImageButton mediaButton = songRow.findViewById(R.id.mediaButton);

                            String artworkUrl = result.getArtworkUrl100();
                            String artworkfilename = result.getLocalArtworkFilename();
                            String fullArtworkFilename = getApplicationContext().getCacheDir() + "/" + artworkfilename;

                            currentFullFilename = fullArtworkFilename;

                            runOnUiThread(() -> {
                                if (new File(fullArtworkFilename).exists()) {
                                    Bitmap bitmap = BitmapFactory.decodeFile(fullArtworkFilename);
                                    artwork.setImageBitmap(bitmap);
                                } else {
                                    URL url = null;
                                    try {
                                        url = new URL(artworkUrl);
                                    } catch (MalformedURLException e) {
                                        e.printStackTrace();
                                    }

                                    URLSession session = new URLSession();
                                    session.downloadTask(url, (localUrl, response5, error) -> {
                                        HTTPURLResponse resp = (HTTPURLResponse) response5;

                                        if (error == null && resp.getStatusCode() == 200) {
                                            File file = new File(localUrl.getFile());
                                            if (file.renameTo(new File(fullArtworkFilename))) {
                                                if (currentFullFilename.equals(fullArtworkFilename)) {
                                                    Bitmap bitmap = BitmapFactory.decodeFile(fullArtworkFilename);
                                                    artwork.setImageBitmap(bitmap);
                                                }
                                            }
                                        }
                                    }).resume();
                                }
                            });


                            trackName.setText(result.getTrackName());
                            artistName.setText(result.getArtistName());

                            String aux1 = result.getLocalPreviewFilename();
                            String aux2 = getApplicationContext().getCacheDir() + "/" + aux1;

                            if (new File(aux2).exists()) {
                                mediaButton.setImageDrawable(getResources().getDrawable(R.drawable.play_img));
                            }

                            mediaButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    String previewUrl = result.getPreviewUrl();
                                    String filename = result.getLocalPreviewFilename();
                                    String fullFilename = getApplicationContext().getCacheDir() + "/" + filename;

                                    currentFullFilename = fullFilename;
                                    stopTrack();

                                    if (new File(fullFilename).exists()) {
                                        if (!isPlaying) {
                                            isPlaying = true;
                                            mediaButton.setImageDrawable(getResources().getDrawable(R.drawable.pause_img));
                                            playTrack(fullFilename);
                                        } else {
                                            isPlaying = false;
                                            mediaButton.setImageDrawable(getResources().getDrawable(R.drawable.play_img));
                                        }
                                    } else {
                                        URL url = null;
                                        try {
                                            url = new URL(previewUrl);
                                        } catch (MalformedURLException e) {
                                            e.printStackTrace();
                                        }

                                        URLSession session = new URLSession();
                                        session.downloadTask(url, (localUrl, response, error) -> {
                                            HTTPURLResponse resp = (HTTPURLResponse) response;

                                            if (error == null && resp.getStatusCode() == 200) {
                                                File file = new File(localUrl.getFile());
                                                if (file.renameTo(new File(fullFilename))) {
                                                    if (currentFullFilename.equals(fullFilename)) {
                                                        mediaButton.setImageDrawable(getResources().getDrawable(R.drawable.play_img));
                                                    }
                                                }
                                            }
                                        }).resume();
                                    }
                                }
                            });

                            songsLayout.addView(songRow);
                        });
                    }
                    break;
            }
        }));
    }

    public void stopTrack() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void playTrack(String fullFileName) {
        stopTrack();
        mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(fullFileName));
        mediaPlayer.start();
    }
}