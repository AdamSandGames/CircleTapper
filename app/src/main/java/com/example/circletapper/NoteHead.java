package com.example.circletapper;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class NoteHead {
    private static final String TAG = "Note Head";
    public enum Head_Type {
        CIRCLE, SLIDER
    }


    // Meta
    protected Head_Type type;
    protected boolean hasCreated;
    protected boolean hasPlayed;
    protected boolean flagRemoveActive = false;

    // POSITION
    protected final float circleRatio = 0.3f;
    protected int xStart; // centered x coordinate
    protected int yStart; // centered y coordinate

    // TIMING
    protected final int anticipation = 1000; // difference between note hit and note first displayed
    protected final int lateTime = 300;
    protected int hitTime; // centered time for perfect hit

    // SCORING
    protected final int pointValue = 100;
    protected int awardedPoints = 0;

    NoteHead(int timing, int xPos, int yPos){
        this.xStart = xPos;
        this.yStart = yPos;
        this.hitTime = timing;
        this.hasCreated = false;
        this.hasPlayed = false;


    }

    public boolean isType(Head_Type t){
        return type == t;
    }

    public void draw(Canvas canvas, Drawable back, Drawable front, Drawable approach, int songTime, int radius){
        if(hasCreated && !hasPlayed){
            back.draw(canvas);
            front.draw(canvas);
            approach.draw(canvas);
        }
    }

    public boolean doesStartYet(int songTime){
        return songTime >= hitTime - anticipation;
    }

    public boolean isLate(int songTime){
        return songTime >= hitTime + lateTime;
    }

    public void create(){
        hasCreated = true;
    }

    public void reset(){
        hasCreated = false;
        hasPlayed = false;
        flagRemoveActive = false;
        awardedPoints = 0;
    }

    protected void kill(){
        hasPlayed = true;
        flagRemoveActive = true;
    }

    protected void hit(int points){
        awardedPoints += points;
    }

    protected void miss(){
        awardedPoints = 0;
        kill();
    }

    protected int hypotenuseOfIntegers(int x1, int y1, int x2, int y2) {
        return (int)Math.hypot(x2 - x1, y2 - y1);
    }

    public boolean inCircle( int x1, int y1, int x2, int y2, int radius){
        return hypotenuseOfIntegers(x1, y1, x2, y2) < radius;
    }

    public boolean checkWhiff(int tapX, int tapY, int radius){
        final float whiffRadiusMultiplier = 1.2f;
        return inCircle(xStart, yStart, tapX, tapY, (int) (radius * whiffRadiusMultiplier));
    }

    public boolean checkHit(int tapX, int tapY, int radius) {
        return inCircle(xStart, yStart, tapX, tapY, radius);
    }

    public boolean checkTiming(int songTime){
        final int perfectPointMultiplier = 3;

        int timeOff = Math.abs(hitTime - songTime);

        if (timeOff < lateTime) { // close time

            if(timeOff < lateTime / perfectPointMultiplier){ // perfect time
                hit(pointValue * perfectPointMultiplier);
            }
            else{
                hit(pointValue);
            }
            return true;

        }
        else { // miss time
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return "NoteHead with time: " + hitTime;
    }
}
