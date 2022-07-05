package com.example.circletapper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

public class SongSelectActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "Song Menu Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_menu);
        populateButtons();
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");
    }



    private void populateButtons(){
        LinearLayout scrollRow = findViewById(R.id.song_scrolling_list);
        scrollRow.removeAllViews();

        boolean noSongs = true;
        for (SongMap song : SongMap.applicationLoadedSongMaps){

            Button button = new Button(this);
            button.setText(song.mapName);
            button.setPadding(0, 0, 0, 0);
            button.setLayoutParams( new FrameLayout.LayoutParams( FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1 ));
            button.setOnClickListener(v -> menuButtonClicked(song.mapID));
            scrollRow.addView(button);
            noSongs = false;
        }
        if(noSongs){
            Log.e(TAG, "NO SONGS LOADED");
            Toast.makeText(this, "No songs could be found, returning to main menu.", Toast.LENGTH_LONG).show();
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, Toast.LENGTH_LONG);
        }
    }

    private void menuButtonClicked(int id) {
        Intent g = new Intent(this, PlayMapActivity.class);
        Bundle b = new Bundle();
        b.putInt(getString(R.string.song_id_handoff_mapLoader), id);
        g.putExtras(b);
        startActivity(g);
    }


    // TODO can associate song.mapName with high scores tables by storing them with the key name "CTHighScores_{mapName}", may need to convert to string
}
