package com.example.circletapper;

import static android.content.Context.MODE_PRIVATE;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;


/*TODO
 * Instructions detailing the mechanics
 * Score Tracking
 *
 *
 * */



public class PlayMapView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "Map View";

    // Program stuff
    private final PlayMapActivity mapActivity;
    private final Context _context;
    private Thread _thread;
    private final SurfaceHolder _surfaceHolder;
    private boolean _run = false;

    // Game stuff
    private int screenWidth;    // screen width
    private int screenHeight;   // screen height
    private final static int MAX_FPS = 60; //desired fps
    private final static int FRAME_PERIOD = 1000 / MAX_FPS; // the frame period
    private float songVolume = 1f;
    private float noteVolume = 1f;
    private int gameStartDelay = 1000;
    private static final String GAMESONGPOS = "CircleTapperGameSongPosition";

    // Music/Map Handling
    private SongMap activeSongMap;
    public MediaPlayer songPlayer;
    public int saveSongPosition;
    private int songPosition;
    private int songDuration;
    // TODO private Conductor conductor; // good games like this have conductor classes. Moves a lot of stuff out of the view, and tracks time more accurately.

    // Note Tracking
    private LinkedHashSet<NoteHead> activeMapNoteList = new LinkedHashSet<>();
    private LinkedHashSet<NoteHead> activeNotes = new LinkedHashSet<>();
    private NoteHead nextTimedNote;
    private Iterator<NoteHead> noteCreationIterator;
    //private SongMap activeSongMapData;
    //private

    // Note Drawing
    private Drawable noteBack;
    private Drawable noteFront;
    private Drawable approachCircle;
    private Drawable screenBackground;
    private Paint sliderLinePaint;

    // Note Sounds
    private SoundPool soundPool;
    private final int MAX_STREAMS = 30;
    private int hitSound;
    private int missSound;
    private int sliderollSound;
    private boolean soundpoolLoaded = false;

    // Point display
    private int playPoints;
    private String currentDisplayPoints = "00000";
    private Paint scorePaint;
    private Drawable perfectBadge;
    private Drawable closeBadge;
    private Drawable missBadge;
    private LinkedList<ScoreBadge> badgeList = new LinkedList<>();

    // Input tracking
    private int lastTouchX = 0;
    private int lastTouchY = 0;
    private boolean isTouchDown = false;

    boolean endgame = false;

    public PlayMapView(Context context){
        super(context);
        _surfaceHolder = getHolder();
        getHolder().addCallback(this);
        mapActivity = (PlayMapActivity) context;
        _context = context;

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void initialize(int w, int h) {
        Log.d(TAG, "initialize");
        screenWidth = w;
        screenHeight = h;

        init_PlayMap();
        init_Drawables();
        init_Sounds();
        init_SongPlayer(activeSongMap.songResourceID);

        activeMapNoteList = activeSongMap.getNoteHeadsData();
        noteCreationIterator = activeMapNoteList.iterator();
        nextTimedNote = noteCreationIterator.next();

        try {
            for(NoteHead note : activeMapNoteList){
                if(note.isType(NoteHead.Head_Type.SLIDER)){
                    NoteSlider slide = (NoteSlider) note;
                    slide.setPaint(sliderLinePaint);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Slider Paint Fuckup");
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> songPlayer.start(), gameStartDelay);
        // songPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
    }

    protected void init_SongPlayer(int songID){
        try {
            if (songPlayer != null){
                songPlayer.stop();
                songPlayer.release();
            }

            songPlayer = MediaPlayer.create(_context, songID);
            songPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            songPlayer.setVolume(songVolume,songVolume); //systemVolume * songVolume
            songPlayer.setLooping(false);

            songPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d(TAG, "On Media Completion: pass internal method");
                    endgame = true;
                }
            });

            Log.d(TAG, "Track Info: " + Arrays.toString(songPlayer.getTrackInfo()) +
                    "\nDuration: " + songPlayer.getDuration()); // "Title" + songPlayer.getTrackInfo(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_METADATA)+
        } catch (IllegalStateException ise) {
            ise.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void endOfMap(){

        Log.d(TAG, "On Song Completion: endOfMap() method");
        mapActivity.finish();
        //Toast.makeText(_context, "End of Song", Toast.LENGTH_LONG).show();
    }

    private void init_PlayMap(){
        updatePoints(); // Set Starting score value

        //Log.d(TAG, "activeMap Reset");
        SongMap.resetNotes(activeMapNoteList);

    }

    //@SuppressLint("deprecation")
    //@SuppressWarnings("deprecation")
    private void init_Drawables(){
        // background

        do {
            screenBackground = ResourcesCompat.getDrawable(getResources(), activeSongMap.backgroundImageID, null);
            if (screenBackground != null) {
                screenBackground.setBounds(0,0,screenWidth, screenHeight);
                screenBackground.setAlpha(50);
            }

            // Score
            scorePaint = new Paint();
            // scorePaint.setTypeface(ResourcesCompat.getFont(getResources(), R.font.hel));
            scorePaint.setColor(getResources().getColor(R.color.white));
            scorePaint.setTextAlign(Paint.Align.LEFT);
            scorePaint.setTextSize(30);

            // Sliders
            sliderLinePaint = new Paint();
            sliderLinePaint.setColor(getResources().getColor(R.color.slider_color));
            sliderLinePaint.setStrokeCap(Paint.Cap.ROUND);
            sliderLinePaint.setStrokeWidth(40);
            sliderLinePaint.setAlpha(100);

            // Circles
            noteBack = ResourcesCompat.getDrawable(getResources(), R.drawable.hitcircle, null);
            noteFront = ResourcesCompat.getDrawable(getResources(), R.drawable.hitcircleoverlay, null);
            approachCircle = ResourcesCompat.getDrawable(getResources(), R.drawable.approachcircle, null);
            perfectBadge = ResourcesCompat.getDrawable(getResources(), R.drawable.badge_excellent, null);
            closeBadge = ResourcesCompat.getDrawable(getResources(), R.drawable.badge_good, null);
            missBadge = ResourcesCompat.getDrawable(getResources(), R.drawable.badge_miss, null);
        } while (scorePaint == null || sliderLinePaint == null ||
                noteBack == null || noteFront == null || approachCircle == null);


    }

    private void init_Sounds(){
        //AudioManager audioManager = (AudioManager) mapActivity.getSystemService(Context.AUDIO_SERVICE);
        //String sampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);


        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(attrs)
                .build();
        soundPool.setOnLoadCompleteListener((soundPool, sampleID, status) -> {
            soundpoolLoaded = true;
        });

        hitSound = soundPool.load(_context, R.raw.hitsound_normal, 1);
        missSound = soundPool.load(_context, R.raw.combobreak, 2);
        sliderollSound = soundPool.load(_context, R.raw.soft_sliderslide, 3);
        // do {} while (!soundpoolLoaded);
    }

    @Override
    public void run(){
        //float avg_sleep = 0.0f;
        //float fcount = 0.0f;
        //long fps = System.currentTimeMillis();

        Canvas c;
        while (_run){
            if(endgame){
                endgame = false;
                endOfMap();
            }
            c = null;
            long started = System.currentTimeMillis();
            try{
                c = _surfaceHolder.lockCanvas(null);

                synchronized (_surfaceHolder){
                    update();
                }

                draw(c);
            } finally{
                if (c != null){
                    _surfaceHolder.unlockCanvasAndPost(c);
                }
            }

            float deltaTime = (System.currentTimeMillis() - started);
            int sleepTime = (int) (FRAME_PERIOD - deltaTime);



            if(sleepTime > 0){
                try{
                    sleep(sleepTime);
                } catch (InterruptedException e){
                    // try again shutting down the thread
                    _thread.interrupt();
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void pause() {
        // Wrap up song, pause, save position
        songPlayer.pause();
        saveSongPosition = songPlayer.getCurrentPosition();
        mapActivity.getPreferences(MODE_PRIVATE).edit().putInt(GAMESONGPOS, saveSongPosition).apply();

        _run = false;
        boolean retry = true;
        while (retry) {
            try {
                _thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // try again shutting down the thread
                //_thread.interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    protected void update() {
        songPosition = songPlayer.getCurrentPosition(); // += updateTime;

        // Kill old notes
        for(NoteHead note : activeNotes){
            if(note.isLate(songPosition)){
                playHitSound(missSound);
                note.kill();
                //Log.d(TAG, "Note Removed Because Late: songPos = " + songPosition + ", NotePos = " + note.hitTime);
            }

            if(note.flagRemoveActive){
                createScoreBadge(note);
            }
        }


        // Remove dead notes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activeNotes.removeIf(note -> {
                return note.flagRemoveActive;
            });

        }
        else{
            for (Iterator<NoteHead> it = activeNotes.iterator(); it.hasNext();) {
                //NoteHead n = it.next();
                if (it.next().flagRemoveActive){
                    it.remove();
                }
            }
        }
        // Create new notes
        if(nextTimedNote.doesStartYet(songPosition)){ // && not created
            //Log.d(TAG, "iter: " + nextTimedNote.toString());
            activeNotes.add(nextTimedNote);
            nextTimedNote.create();
            if(noteCreationIterator.hasNext()){
                nextTimedNote = noteCreationIterator.next();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            badgeList.removeIf( badge-> !badge.checkLife(songPosition));
        }

    }

    private void createScoreBadge(NoteHead note){
        if(note.awardedPoints == 0){
            badgeList.add(new ScoreBadge(note.xStart, note.yStart, songPosition, 0));
        }
        else if (note.awardedPoints == 600 || note.awardedPoints == 300){
            badgeList.add(new ScoreBadge(note.xStart, note.yStart, songPosition, 2));
        }
        else{
            badgeList.add(new ScoreBadge(note.xStart, note.yStart, songPosition, 1));
        }
    }

    int pointSum(LinkedHashSet<NoteHead> myMap){
        int result = 0;
        for (NoteHead note : myMap){
            result += note.awardedPoints;
        }

        return result;
    }

    public void updatePoints(){
        playPoints = pointSum(activeMapNoteList);
        currentDisplayPoints = String.valueOf(playPoints);
    }


    @Override
    public void draw(Canvas canvas) {
        try {
            super.draw(canvas);
            screenBackground.draw(canvas);

            for (NoteHead note : activeNotes) {
                if(!note.flagRemoveActive) {
                    note.draw(canvas, noteBack, noteFront, approachCircle, songPosition, activeSongMap.radius);
                }
            }
            for (ScoreBadge badge : badgeList){
                if (badge.checkLife(songPosition)){
                    float badgeScale = 0.5f;
                    if(badge.type == 0){
                        missBadge.setBounds(badge.x, badge.y, badge.x + (int)(120*badgeScale), badge.y + (int)(50*badgeScale));
                        missBadge.draw(canvas);
                    }
                    else if (badge.type == 2){
                        perfectBadge.setBounds(badge.x, badge.y, badge.x + (int)(245*badgeScale), badge.y + (int)(40*badgeScale));
                        perfectBadge.draw(canvas);
                    }
                    else {
                        closeBadge.setBounds(badge.x, badge.y, badge.x + (int)(120*badgeScale), badge.y + (int)(40*badgeScale));
                        closeBadge.draw(canvas);
                    }
                }
            }

            if (scorePaint != null) {
                canvas.drawText(currentDisplayPoints, (float)screenWidth / 20, (float)screenHeight / 20, scorePaint);
                canvas.drawText(activeSongMap.mapName, (float)screenWidth / 2, (float)screenHeight / 20, scorePaint);
            }
        } catch (Exception e) {
            //Log.e(TAG, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();

        switch(action) {
            case (MotionEvent.ACTION_DOWN) :
                lastTouchX = (int) event.getX();
                lastTouchY = (int) event.getY();
                isTouchDown = true;
                return noteHitChecking(event);

            case (MotionEvent.ACTION_MOVE) :
                lastTouchX = (int) event.getX();
                lastTouchY = (int) event.getY();
                return noteHitChecking(event);

            case (MotionEvent.ACTION_UP) :
                lastTouchX = (int) event.getX();
                lastTouchY = (int) event.getY();
                isTouchDown = false;
                return noteHitChecking(event);

            case (MotionEvent.ACTION_CANCEL) :
                return true;
            case (MotionEvent.ACTION_OUTSIDE) :
                return true;
            default :
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick(){
        super.performClick();
        // TODO use this for accessibility reasons
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean noteHitChecking(MotionEvent event){
        int action = event.getAction();
        boolean hitFlag = false;
        boolean whiffFlag = false;
        NoteHead whiff = null;
        for (NoteHead note : activeNotes){
            if (!note.flagRemoveActive) {
                if(note.isType(NoteHead.Head_Type.CIRCLE)){
                    if(action == MotionEvent.ACTION_DOWN){
                        if(note.checkWhiff(lastTouchX, lastTouchY, activeSongMap.radius)){

                            if (note.checkHit(lastTouchX, lastTouchY, activeSongMap.radius)) {

                                if(note.checkTiming(songPosition)){
                                    playHitSound(hitSound);
                                    updatePoints();
                                }
                                else{
                                    playHitSound(missSound);
                                }
                                hitFlag = true;
                                break;
                            }
                            else{
                                // miss unless something hits
                                whiffFlag = true;
                                whiff = note;
                            }
                        }
                    }
                }
                else{ // if (note.isType(NoteHead.Head_Type.SLIDER) {
                    NoteSlider sNote = (NoteSlider) note;
                    if(!sNote.slideStarted && isTouchDown){ // check for initial hit
                        if (action == MotionEvent.ACTION_DOWN) {
                            if(sNote.checkWhiff(lastTouchX, lastTouchY, activeSongMap.radius)) {

                                if (sNote.checkHit(lastTouchX, lastTouchY, activeSongMap.radius)) {
                                    if(sNote.checkTiming(songPosition)){
                                        playHitSound(hitSound);
                                        updatePoints();
                                        sNote.slideStarted = true;
                                        if(soundpoolLoaded) {
                                            soundPool.play(sliderollSound, noteVolume, noteVolume, 1, 1, 1);
                                        }
                                    }
                                    else{
                                        playHitSound(missSound);
                                        note.miss();
                                    }
                                    break;
                                }
                                else{
                                    whiffFlag = true;
                                    whiff = note;
                                }
                            }
                        }
                    }
                    if(sNote.slideStarted && isTouchDown) { // check for slide
                        if (sNote.checkHold(lastTouchX, lastTouchY, activeSongMap.radius)) {

                        } else { // on release
                            playHitSound(missSound);
                            note.kill();

                        }
                        break;
                    }
                    if(sNote.slideStarted && !isTouchDown){
                        if(action == MotionEvent.ACTION_UP){ // check for release
                            if (sNote.checkHit(lastTouchX, lastTouchY, activeSongMap.radius)) {
                                if(sNote.checkReleaseTime(songPosition)){
                                    playHitSound(hitSound);
                                    updatePoints();
                                }
                                else{
                                    playHitSound(missSound);
                                }
                                note.kill();
                                break;
                            }
                        }
                    }
                }
            }
        }
        if(whiffFlag && !hitFlag){
            whiff.miss();
            playHitSound(missSound);
        }
        // activeNotes.removeIf(note -> note.flagRemoveActive);
        return true;
    }


    public void playHitSound(int soundID){
        soundPool.stop(sliderollSound);
        if(soundpoolLoaded) {
            soundPool.play(soundID, noteVolume, noteVolume, 1, 0, 1); // systemVolume * noteVolume
            //Log.d(TAG, "Playing Sound: " + soundID);
        }
        else{
            Log.e(TAG, "Sound Not Loaded Error");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        // ?initialize(width, height);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        _run = true;
        _thread = new Thread(this);
        _thread.start();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // simply copied from sample application LunarLander:
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        _run = false;
        while (retry) {
            try {
                _thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // we will try it again and again...
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initialize(w, h);
    }


    public void togglePauseMySong(){
        if(songPlayer.isPlaying()){
            songPlayer.pause();
        }
        else{
            new Handler(Looper.getMainLooper()).postDelayed(() -> songPlayer.start(), gameStartDelay);
        }
    }

    public void cleanUpResources(){
        if (songPlayer != null){
            songPlayer.stop();
            songPlayer.release();
        }
        songPlayer = null;
        if (soundPool != null){
            soundPool.autoPause();
            soundPool.release();
        }
        soundPool = null;
    }

    public void pushSong(SongMap songMap){
        activeSongMap = songMap;
    }

    public void saveScore(){
        updatePoints();
        String refID = String.valueOf(activeSongMap.mapID);
        String points = String.valueOf(playPoints);
        ArrayList<String> scoresList;

        SharedPreferences sharedPreferences = mapActivity.getPreferences(MODE_PRIVATE);
        scoresList = new ArrayList<>(sharedPreferences.getStringSet(refID, null));

        scoresList.add(points);
        sharedPreferences.edit().putStringSet(refID, (Set<String>) scoresList).apply();

    }


}
