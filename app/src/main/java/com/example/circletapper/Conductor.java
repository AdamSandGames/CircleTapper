package com.example.circletapper;

import android.media.MediaPlayer;
import android.widget.MediaController;

public class Conductor {
    public static final String TAG = "Conductor";

    // RULERS
    float bpm;
    float crotchet;

    // SONGHEAD
    float songPosition;
    float deltaSongPosition;

    // HIT TRACKER
    float lastHit;
    float actualLastHit;

    // MEASURES
    float nextBeatTime;
    float nextBarTime;
    int beatNumber;
    int barNumber;

    // SOUND OFFSET
    float offset;
    float addOffset;
    static float offsetStatic;
    static boolean hasOffsetBeenAdjusted;

    // SCORING
    static int points_perfect = 15;
    static int points_close = 10;
    static int points_miss = 0;
    // multiplier?

    // Map
    // NoteMap
    //MediaPlayer mediaPlayer;
    //MediaController.MediaPlayerControl mediaControl;

    public Conductor(){
        // TODO
    }

    public void CreateNoteHead(){
        //TODO
    }



    // https://www.reddit.com/r/gamedev/comments/2fxvk4/heres_a_quick_and_dirty_guide_i_just_wrote_how_to/
    // https://www.reddit.com/r/gamedev/comments/13y26t/how_do_rhythm_games_stay_in_sync_with_the_music/c78aawd/
}
