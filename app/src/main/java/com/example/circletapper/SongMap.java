package com.example.circletapper;

import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashSet;

/*DOCS
*   SongMap files are stored in .txt format
*   Line types are denoted by their first characters [#, H, N, S, etc.]
*   Information within a line is separated by a [semicolon delimiter] character [;]
*       **  *  **
*   '#' *Comment*
*   'H' *Header*
*       Headers denote map info
*       They take the format:
*       H;mapID;songName;bpm;delay;noteRadius;songFile
*           mapID       = id code for the file (NOT resource address)
*           songName    = Displayed String Name of Map
*           bpm         = number of beats per 60000 milliseconds
*           delay       = time before first note starts                       // TD MAY CHANGE TO ms DELAY BEFORE PLAYING SONG FILE
*           noteRadius  = how big notes are
*           songFile    = name of song file for lookup
*   'N' *NoteHead Object*
*       NoteHeads denote default type NoteHead info
*       They take the format:
*       N;timing;x;y
*           timing      = position of song playhead in ms for perfect note hit
*           x/y         = coordinate positions of note on 1000x1000 grid (later scaled to screen)
*   'S' *Slider Object*
*       Sliders denote Slider type NoteHead info
*       They take the format:
*       S;timeStart;xStart;yStart;duration;xEnd;yEnd
*           timeStart   = position of song playhead in ms for perfect note hit
*           x/yStart    = coordinate positions of slider start on 1000x1000 grid (later scaled to screen)
*           duration    = time difference between [timeStart] and slider end position
*           x/yEnd      = coordinate positions of slider end on 1000x1000 grid (later scaled to screen)
*   * Example *
*   # Title 01: song.mp3
*   H;1;Title 01;67;4;50;song.mp3;background.jpg
*   N;0;719;779
*   S;----------
* */

public class SongMap {
    private static final String TAG = "Song Map";

    public static ArrayList<SongMap> applicationLoadedSongMaps = new ArrayList<>();
    protected static int counter = 0;
    // <mapID, musicID> Resource ID pairs
    protected static Hashtable<Integer, Integer> songmapsDictionary;


    static {
        songmapsDictionary = new Hashtable<>();
//        songmapsDictionary.put(R.raw.random_song_01, R.raw.love_dramatic);
//        songmapsDictionary.put(R.raw.random_song_02, R.raw.love_dramatic);
//        Log.d(TAG, "Test Dictionary Creation");
    }

    protected static final int MillisecondMinute = 60000; // Milliseconds in a minute
    protected static final int CoordinateScale = 1000;
    protected static int ScreenLongDimension;
    protected static int ScreenShortDimension;
    static {
        int a = Resources.getSystem().getDisplayMetrics().widthPixels;
        int b = Resources.getSystem().getDisplayMetrics().heightPixels;
        ScreenLongDimension = Math.max(a, b);
        ScreenShortDimension = Math.min(a, b);
    }


    protected int mapID;              // Map ID
    protected String mapName;         // Song Name
    protected int songResourceID;     // Associated Song File
    protected double beatsPerMinute;  // BPM
    protected double startDelay;      // Start Delay
    protected int radius;        // radius of noteheads
    protected final LinkedHashSet<NoteHead> noteHeadsData;  // Linked Hash Set of Notes
    protected int backgroundImageID;


    SongMap(int id, String name, double bpm, double startDelay, int rad, int songResId) {
        this.mapID = id;
        this.mapName = name;
        this.beatsPerMinute = bpm;
        this.startDelay = startDelay;
        this.radius = rad;
        this.songResourceID = songResId;
        this.noteHeadsData = new LinkedHashSet<>();

        counter++;
        //if(counter == 4) throw new RuntimeException("WTF counter 4");
    }

    protected void addNote(int timing, int xPos, int yPos) {
        NoteCircle n = new NoteCircle(
                timing,
                coordTransform(xPos, ScreenLongDimension, radius * 2),
                coordTransform(yPos, ScreenShortDimension, radius * 2)
        );

        this.noteHeadsData.add(n);
    }

    protected void addSlider(int timing, int xPos, int yPos, int dur, int xEnd, int yEnd) {
        NoteSlider n = new NoteSlider(
                timing,
                coordTransform(xPos, ScreenLongDimension, radius * 2),
                coordTransform(yPos, ScreenShortDimension, radius * 2),
                dur,
                coordTransform(xEnd, ScreenLongDimension, radius * 2),
                coordTransform(yEnd, ScreenShortDimension, radius * 2)
        );

        this.noteHeadsData.add(n);
    }

    public LinkedHashSet<NoteHead> getNoteHeadsData() {
        return noteHeadsData;
    }

    // OPTIMIZE
    public static SongMap loadFromFile(MainActivity context, int mapid) throws IOException {
        Log.d(TAG + ".loadFromFile()", "Load Start.");

        SongMap songMap = null;
        int hcount = 0;
        int ncount = 0;
        int scount = 0;
        int defcount = 0;

        InputStream finStream;
        BufferedReader buffRead;
        String readLine;

        finStream = context.getResources().openRawResource(mapid);
        if (finStream != null) {
            buffRead = new BufferedReader(new InputStreamReader(finStream));

            while ((readLine = buffRead.readLine()) != null) {

                if (!readLine.startsWith("#") && !readLine.isEmpty()) {

                    String[] fields = readLine.split(";", -1);
                    switch (readLine.charAt(0)) {
                        case 'H': // Header Line
                            songMap = new SongMap(  /* int id, String name, double bpm, double startDelay, int rad, int songResId */
                                    Integer.parseInt(fields[1]),
                                    fields[2],
                                    Double.parseDouble(fields[3]),
                                    Double.parseDouble(fields[4]),
                                    Integer.parseInt(fields[5]),
                                    context.getSongIDbyName(fields[6]));
                            songMap.radiusMapToScreen();
                            songMap.backgroundImageID = context.getImageIDbyName(fields[7]);
                            Log.d(TAG, "background assignment: id = " + songMap.backgroundImageID + ", name = " + fields[7]);
                            hcount++;
                            break;

                        case 'N': // Notehead Line  ***  N;209000;840;361
                            if (songMap == null) {
                                throw (new RuntimeException("Notes have no Header: " + mapid));
                            }
                            songMap.addNote( /*(int timing, int xPos, int yPos)*/
                                    Integer.parseInt(fields[1]),
                                    Integer.parseInt(fields[2]),
                                    Integer.parseInt(fields[3])
                            );
                            ncount++;
                            break;

                        case 'S': // Slider Line *** S;210000;708;470;1000;706;568
                            if (songMap == null) {
                                throw (new RuntimeException("Notes have no Header: " + mapid));
                            }
                            songMap.addSlider( /*(int timing, int xStart, int yStart, int duration, int xEnd, int yEnd )*/
                                    Integer.parseInt(fields[1]),
                                    Integer.parseInt(fields[2]),
                                    Integer.parseInt(fields[3]),
                                    Integer.parseInt(fields[4]),
                                    Integer.parseInt(fields[5]),
                                    Integer.parseInt(fields[6])
                            );
                            scount++;
                            break;

                        default:
                            defcount++;
                            break;
                    }
                    //lines.add(readLine);
                }
            }
            finStream.close();
            buffRead.close();

            Log.d(TAG, "H: " + (hcount) +
                    "\nN: " + (ncount) +
                    "\nS: " + (scount) +
                    "\nDefault: " + (defcount));

        }
        /*} catch (Exception e) {
            Log.e(TAG + ".SongLoader()", "Loading Fail.");
            Log.e(TAG, e.getStackTrace().toString());
            throw new RuntimeException(e);
        }*/
        if (songMap == null) {
            throw (new RuntimeException("Couldn't Load Song: " + mapid));
        }
        return songMap;
    }

    public static void resetNotes(LinkedHashSet<NoteHead> noteList){
        for(NoteHead note : noteList){
            note.reset();
        }
    }

    protected int coordTransform(int xyz, int screenDim, int borders) {
        return (borders) + (screenDim - (borders * 2)) * xyz / CoordinateScale;
    }

    protected void radiusMapToScreen(){
        radius = ScreenLongDimension * radius / CoordinateScale;
    }

    protected float totalBeatCount(int millisecDuration) {
        return (int) (beatsPerMinute * (millisecDuration / MillisecondMinute));
    }

    protected int timingCalculation(float timing) {
        float noteDur = singleBeatDur((float) beatsPerMinute);
        int del = (int) (noteDur * startDelay);

        //Log.d(TAG, String.format("timingCalculation(%0.2f, %0.2f, %0.2f)): %d", beatsPerMinute, startDelay, timing, noteDur));
        return (int) (timing * noteDur) + del; // returns in milliseconds
    }


    public static Hashtable<Integer, Integer> getSongmapsDictionary() {
        return songmapsDictionary;
    }

    public static void setSongsHash(Hashtable<Integer, Integer> inMap) {
        songmapsDictionary = inMap;
    }

    public static void printSongsMap() {
        Log.d(TAG, "printSongsMap() <mapfile, musicfile>");
        int counter = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //Log.d(TAG, "forEach version:");
            songmapsDictionary.entrySet().forEach( entry -> {
                Log.d(TAG, String.format("Key: %d ++ Value: %d", entry.getKey(), entry.getValue()));
            });
        } else {
            //Log.d(TAG, "for int : version:");
            for (int i : songmapsDictionary.keySet()) {
                try {
                    int value = songmapsDictionary.get(i);
                    Log.d(TAG, String.format("Key: %d ++ Value: %d", i, value));
                } catch (NullPointerException e) {
                    Log.e(TAG, "SongmapsDictionary has unassigned value for key = " + i);
                    e.printStackTrace();
                }
            }
        }
        Log.d(TAG, "printSongsMap() Count: " + counter);
    }

    protected static float singleBeatDur(float bpm) {
        return (float) MillisecondMinute / bpm; //returns in milliseconds
    }
}