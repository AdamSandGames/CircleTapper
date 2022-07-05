package com.example.circletapper;


/*TODO
 * Add words saying Perfect/Good on note hit
 * ? special sound for perfect hits
 * OPTIMIZE could probably optimize how radius is stored/handled
 *
 * */


import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public class NoteCircle extends NoteHead {
    private static final String TAG = "Note Circle";

    NoteCircle(int timing, int xPos, int yPos){
        super(timing, xPos, yPos);
        type = Head_Type.CIRCLE;
    }

    @Override
    public void draw(Canvas canvas, Drawable back, Drawable front, Drawable approach, int songTime, int radius){

        double ratio = 1 + Math.max( circleRatio * (((double)hitTime - songTime) / anticipation), 0 );

        int draw_x = xStart;
        int draw_y = yStart;

        back.setBounds(draw_x - radius, draw_y - radius, draw_x + radius, draw_y + radius);
        front.setBounds(draw_x - radius, draw_y - radius, draw_x + radius, draw_y + radius);
        approach.setBounds((int)(draw_x - radius * ratio),
                (int)(draw_y - radius * ratio),
                (int)(draw_x + radius * ratio),
                (int)(draw_y + radius * ratio));

        super.draw(canvas, back, front, approach, songTime, radius);
    }

    @Override
    public boolean checkHit(int tapX, int tapY, int radius) {
        return super.checkHit(tapX, tapY, radius);
    }

    @Override
    public boolean checkTiming(int songTime){
        if(super.checkTiming(songTime)){
            kill();
            return true;
        }
        kill();
        return false;
    }

}

