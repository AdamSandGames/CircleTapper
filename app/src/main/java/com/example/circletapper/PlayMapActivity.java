package com.example.circletapper;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

public class PlayMapActivity extends Activity {
    private static final String TAG = "Map Activity";

    private PlayMapView mapView;
    private SongMap currentSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        // Unbundling
        Bundle b;
        int songid;
        try{
            b = getIntent().getExtras();
            if (b != null) {
                songid = b.getInt(getString(R.string.song_id_handoff_mapLoader));
                currentSong = MainActivity.getSongById(songid);
                if (currentSong == null){
                    Toast.makeText(this, "Song Failed to Load properly.", Toast.LENGTH_LONG).show();
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, Toast.LENGTH_LONG);
                }
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage());
            throw new RuntimeException(e);
        }

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN );

        mapView = new PlayMapView(this);
        mapView.pushSong(currentSong);
        setContentView(mapView);
        mapView.requestFocus();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.pause();

        // Moved to PlayMapView.pause();
        /*mapView.songPlayer.pause();
        mapView.saveSongPosition = mapView.songPlayer.getCurrentPosition();
        getPreferences(MODE_PRIVATE).edit().putInt(GAMESONGPOS, mapView.saveSongPosition).commit();*/
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //mapView.saveScore();
        mapView.cleanUpResources();
    }

}