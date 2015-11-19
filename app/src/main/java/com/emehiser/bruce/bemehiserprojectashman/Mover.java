package com.emehiser.bruce.bemehiserprojectashman;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.security.InvalidParameterException;

/**
 * Created by Bruce Emehiser on 11/10/2015.
 *
 * This class contains the characteristics of Ashman and the Ghosts
 */
abstract class Mover {

    // current x and y location on game map
    float x;
    float y;

    // radius of the mover
    final float radius;

    // definition of keys for current direction
    static final int STOPPED = 0;
    public static final int UP = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;
    public static final int RIGHT = 4;

    // the current direction Mover is moving
    int direction;

    // the speed of the mover in blocks per second
    float speed;

    // paint object used when drawing mover
    Paint paint;
    int color;

    Mover() {
        // se the radius
        radius = .4f;

        direction = Mover.STOPPED;
    }

    public abstract void drawMover(Canvas canvas);

    public void setPosition(float curX, float curY) {
        this.x = curX;
        this.y = curY;
    }


    public void setSpeed(float speed) {
        if(speed < 0) {
            throw new InvalidParameterException("Speed cannot be negative");
        }
        this.speed = speed;
    }

    public void move(Maze maze) {

        // make call to moveMover
        move(maze, direction);
    }

    public void move(Maze maze, int direction) {

        // if we are not moving, return
        if(direction == Mover.STOPPED) {
            this.direction = Mover.STOPPED;
            return;
        }
        // else
        this.direction = direction;

        // distance mover will moveMover in this animation
        float distance = speed / ((float) Maze.ANIMATIONS_PER_SECOND);

        // calculate the final location based on which direction mover is moving
        float finalX = x;
        float finalY = y;
        switch (direction) {
            case Mover.UP:
                finalY -= distance;
                break;
            case Mover.DOWN:
                finalY += distance;
                break;
            case Mover.LEFT:
                finalX -= distance;
                break;
            case Mover.RIGHT:
                finalX += distance;
                break;
            // default: mover is stopped. Already handled.
        }

        // check to see if we can moveMover
        boolean canMove = maze.canMove(finalX, finalY, radius, direction);
        // if we can moveMover to the new location
        if(canMove) {
            // invalidate current location
            invalidateMoverPosition(maze);
            // moveMover to new location
             setPosition(finalX, finalY);
            // invalidate new mover location
            invalidateMoverPosition(maze);
        }
        // if we cannot moveMover, stop
        else {
            this.direction = Mover.STOPPED;
        }
    }

    private void invalidateMoverPosition(Maze maze) {

        // check for null view
        if(maze == null)
            throw new NullPointerException("Maze is null");

        // get rectangle
        int top = (int) (y - radius - 1);
        int bottom = (int) (y + radius + 1);
        int left = (int) (x - radius - 1);
        int right = (int) (x + radius + 1);

        // call to invalidate
        maze.invalidate(left, top, right, bottom);
    }
}
