package com.emehiser.bruce.bemehiserprojectashman;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Created by Bruce Emehiser on 11/12/2015.
 *
 * Defines an Ashman
 */
public class Ashman extends Mover {

    // paint object for the ashman mouth
    private final Paint mouth;

    // counts animations for mouth, so mouth opens and closes
    private int mouthCountdown;

    public Ashman() {
        super();

        // set the ashman color to be yellow
        color = Color.YELLOW;

        // set the mouth paint
        mouth = new Paint();
        mouth.setStyle(Paint.Style.FILL);
        mouth.setColor(Color.BLACK);

        // set the ashman paint
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);

        // set the speed
        speed = 1;

        // mouth countdown is ratio of animations per second
        mouthCountdown = 0;
    }

    @Override
    public void move(Maze maze) {
        super.move(maze);
        // check for cake to eat
        maze.chompCake(x, y);

        // see if ashman collides with other movers in the maze
        boolean collision = maze.collision(this);
        if (collision) {
            // report to the game that ashman lost (was eaten)
            maze.endGame(Maze.GAME_LOSS);
        }
    }

    @Override
    public void drawMover(Canvas canvas) {

        // draw ashman
        canvas.drawCircle(x, y, radius, paint);

        // the offset from ashman center, and the radius of the mouth
        float offsetRadius = radius / 1.5f;

        // calculate current mouth state
        mouthCountdown = (mouthCountdown + 1) % Maze.ANIMATIONS_PER_SECOND;

        // draw mouth half of the time
        if(mouthCountdown < Maze.ANIMATIONS_PER_SECOND / 2) {
            // draw mouth based on ashman direction
            switch (direction) {
                case Mover.UP:
                    canvas.drawCircle(x, y - offsetRadius, offsetRadius, mouth);
                    break;
                case Mover.DOWN:
                    canvas.drawCircle(x, y + offsetRadius, offsetRadius, mouth);
                    break;
                case Mover.LEFT:
                    canvas.drawCircle(x - offsetRadius, y, offsetRadius, mouth);
                    break;
                case Mover.RIGHT:
                    canvas.drawCircle(x + offsetRadius, y, offsetRadius, mouth);
            }
        }
    }
}
