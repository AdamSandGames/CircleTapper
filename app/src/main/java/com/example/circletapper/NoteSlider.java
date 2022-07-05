package com.example.circletapper;

/*TODO *** ALL OF THIS ***
*  from osu: Curvepoints = x/yEnd, length = ?
* */


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class NoteSlider extends NoteHead {
    private static final String TAG = "Note Slider";

    int xEnd;
    int yEnd;
    int duration;
    float progress;
    boolean slideStarted;
    Paint linePaint;

    private int active_x;
    private int active_y;


    NoteSlider(int timing, int xStart, int yStart, int duration, int xEnd, int yEnd) {
        super(timing, xStart, yStart);
        type = Head_Type.SLIDER;

        this.xEnd = xEnd;
        this.yEnd = yEnd;
        this.duration = duration;
        this.progress = 0;
        this.slideStarted = false;

    }

    @Override
    public void draw(Canvas canvas, Drawable back, Drawable front, Drawable approach, int songTime, int radius) {
        // Log.d(TAG, "Draw Slider");
        canvas.drawLine(xStart, yStart, xEnd, yEnd, linePaint);

        double ratio = 1 + Math.max(circleRatio * (((double) hitTime - songTime) / anticipation), 0);

        active_x = (int) scale(xStart, xEnd, getProgress_updated(songTime));
        active_y = (int) scale(yStart, yEnd, getProgress_updated(songTime));

        back.setBounds(active_x - radius, active_y - radius, active_x + radius, active_y + radius);
        front.setBounds(active_x - radius, active_y - radius, active_x + radius, active_y + radius);
        approach.setBounds((int) (active_x - radius * ratio),
                (int) (active_y - radius * ratio),
                (int) (active_x + radius * ratio),
                (int) (active_y + radius * ratio));
        super.draw(canvas, back, front, approach, songTime, radius);
    }

    @Override
    public boolean isLate(int songTime) {
        return songTime >= hitTime + lateTime + duration;
    }

    @Override
    public void reset() {
        super.reset();
        this.progress = 0;
        this.slideStarted = false;
    }

    @Override
    protected void hit(int points) {
        super.hit(points);
    }

    public void setPaint(Paint paint) {
        linePaint = paint;
    }

    protected float getProgress_updated(int songTime) {
        float a = 0f;
        float b = duration;
        float t = (float) songTime - hitTime;
        progress = cappedLerp(a, b, t);
        return progress;
    }

    private float cappedLerp(float a, float b, float f) {
        final float cap_low = 0f;
        final float cap_high = 1f;
        return Math.max(cap_low, Math.min(interpolate(a, b, f), cap_high));
    }

    private float interpolate(float a, float b, float f) {
        return ((f - a) / (b - a));
    }

    private float scale(float a, float b, float s) {
        return a * (1 - s) + b * s;
    }

    @Override
    public boolean checkHit(int tapX, int tapY, int radius) {
        return inCircle(active_x, active_y, tapX, tapY, radius);
    }

    public boolean checkReleaseTime(int songTime) {
        if (checkTiming(songTime - duration)) {
            return true;
        }
        return false;
    }

    public boolean checkHold(int tapX, int tapY, int radius) {
        if (inCircle(active_x, active_y, tapX, tapY, radius)) {
            return true;
        }
        kill();
        return false;
    }
}
