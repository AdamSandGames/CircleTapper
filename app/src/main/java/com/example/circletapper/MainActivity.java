package com.example.circletapper;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedHashMap;


public class MainActivity extends AppCompatActivity implements OnClickListener {
    private static final String TAG = "CircleTapper";

    private static final LinkedHashMap<String, Integer> song_ids = new LinkedHashMap<>();
    private static boolean hasStartupMapsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, ".onCreate()");
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onCreate(savedInstanceState);



        if(!hasStartupMapsLoaded){
            try {
                Log.d(TAG, "load maps");
                updateSongsHash();
                hasStartupMapsLoaded = true;
            } catch (IOException e) {
                e.printStackTrace();
                throw (new RuntimeException(e));
            }
        }

        setContentView(R.layout.activity_main);

        // listeners for buttons
        View newButton = findViewById(R.id.play_menu_button);
        newButton.setOnClickListener(this);

        View aboutButton = findViewById(R.id.options_button);
        aboutButton.setOnClickListener(this);

        View exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener(this);
    }


    public void onClick(View v) {
        Intent g;
        int id = v.getId(); // Buttons go to other activities
        if (id == R.id.play_menu_button) { // song select
            g = new Intent(this, SongSelectActivity.class);
            startActivity(g);
        } else if (id == R.id.options_button) { // options
            g = new Intent(this, OptionsActivity.class); // TODO implement options page
            startActivity(g);
        } else if (id == R.id.exit_button) { // exit
            finish();
        }
    }

    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d(TAG, "onConfigurationChanged " + newConfig.orientation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    public void updateSongsHash() throws IOException {
        boolean plogs = false;
        Log.d(TAG, "Updating Song Maps...");
        if (plogs) {
            SongMap.printSongsMap();
        }

        Hashtable<Integer, Integer> updatedMap = new Hashtable<>();

        Field[] fields = R.raw.class.getDeclaredFields();
        String[] names = new String[fields.length];
        int[] ids = new int[fields.length];

        try {

            for (int i = 0; i < fields.length; i++) {
                names[i] = fields[i].getName();
                int rawId = getResources().getIdentifier("raw/"+names[i], null, getPackageName());
                ids[i] = fields[i].getInt(rawId); // IllegalAccessException
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        Log.i(TAG, ".updateSongsHash()" +
                "\nNames: " + Arrays.toString(names) +
                "\nids: " + Arrays.toString(ids)
        );

        Log.d(TAG, "*** IDs filenames lookup ***");
        for (int id : ids){
            TypedValue val = new TypedValue();
            getResources().getValue(id, val, true);
            String filename = val.string.toString();

            if(filename.endsWith(".mp3")){
                Log.d(TAG, "~~~ MP3 Found : id = " + id);
                song_ids.put(filename, id);
            }

        }

        for (int id : ids){
            TypedValue val = new TypedValue();
            getResources().getValue(id, val, true);
            String filename = val.string.toString();
            Log.d(TAG, "IDs filenames lookup: " + filename);
            if (filename.endsWith(".txt")){
                Log.d(TAG, "~~~ TXT Found : id = " + id);
                SongMap s = SongMap.loadFromFile(this, id);

                updatedMap.put(id, s.songResourceID);
                addMap(s);
            }
        }




        SongMap.setSongsHash(updatedMap);
        Log.d(TAG, "Song Maps Updated: ");
        if (plogs) {
            SongMap.printSongsMap();
        }
    }

    public ArrayList<SongMap> getLoadedMaps(){
        return SongMap.applicationLoadedSongMaps;
    }

    public void addMap(SongMap myMap){
        if(!SongMap.applicationLoadedSongMaps.contains(myMap)){
            SongMap.applicationLoadedSongMaps.add(myMap);
        }
    }

    public int getSongIDbyName(String name){
        Log.d(TAG, "song_ids = " + song_ids.toString() + ", name = " + name + ", value = " + song_ids.get("res/raw/" + name));
        Integer id = song_ids.get("res/raw/" + name);
        if (id == null){
            throw new NullPointerException("Song ID for " + name + "not found.");
        }
        return id;
    }

    public int getImageIDbyName(String name){
        Field[] fields = R.drawable.class.getDeclaredFields();
        for (Field fi : fields){
            String fiName = fi.getName();
            if(name.substring(0, name.length()-4).equals(fiName)){
                Log.d(TAG, fiName + " == " + name);
                return getResources().getIdentifier("drawable/"+fiName, "drawable", getPackageName());
            }
        }
        throw new RuntimeException("IMAGE ID NOT FOUND");
    }

    public static SongMap getSongById(int id){
        for (SongMap song : SongMap.applicationLoadedSongMaps){
            if (song.mapID == id){
                return song;
            }
        }
        return null;
    }

    private ArrayList<String> getMapNames(){
        ArrayList<String> names = new ArrayList<>();
        for(SongMap sm : SongMap.applicationLoadedSongMaps){
            names.add(sm.mapName);
        }
        return names;
    }
}


/*
DOCS *** Grading Criteria ***
 FOR A C:
    Hit Circles
    Scoring (Perfect, Close, Miss)
    Sound effects (hit, miss, slider roll)
    Mp3/song backdrop
 For A B:
    Sliders
    Image Backgrounds
 FOR An A:
    Map/song library with multiple songs
    Leaderboard/Score Tracking
 FOR Extra Credit:
    Live Map recording on new songs
    Difficulty approximation on recorded maps (notes/minute and distance travelled/minute)
 ***     ***     ***     ***     ***     ***     ***     ***     ***     ***     ***     ***     ***
 Description:
    Rhythm game based on tapping circles at the correct time set to music.
    The player must tap circles that appear on the screen at a time indicated
    by a shrinking timer circle that will line up with the circumference of the
    hit circles at the indicated time. Some hit circles will be sliders that the
    player must follow with their finger as the hit-area moves from one location
    to another. Players will be graded based on if they tap inside the circle and
    on how close to the correct time they hit it. Players will also be able to
    select songs from their device to record their own maps on, and the game will
    record their inputs while the track plays as a new map for the game.
*/