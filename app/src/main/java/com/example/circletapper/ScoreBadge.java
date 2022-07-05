package com.example.circletapper;

public class ScoreBadge {

    int x;
    int y;
    int birthTime;
    int duration = 300;
    int type; // 0 miss, 1 close, 2 perfect

    ScoreBadge(int xPos, int yPos, int time, int pts){
        this.x = xPos;
        this.y = yPos;
        this.birthTime = time;
        this.type = pts;
    }

    public boolean checkLife(int songTime){
        return songTime < birthTime + duration;
    }
}
