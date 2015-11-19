package com.emehiser.bruce.bemehiserprojectashman;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Random;

/**
 * Created by Bruce Emehiser on 11/12/2015.
 *
 * Defines a Mover
 */
public class Ghost extends Mover {

    // random for random direction
    private final Random random;

    public Ghost() {
        super();

        color = Color.RED;

        // set the paint
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);

        // set the speed
        speed = 1;

        // get random for random ghost direction
        random = new Random();
    }

    @Override
    public void drawMover(Canvas canvas) {

        // draw circle that defines the current position of the ghost
        canvas.drawCircle(x, y, radius, paint);
    }

    @Override
    public void move(Maze maze) {

        if(direction != Mover.STOPPED) {
            // if we have a direction
            super.move(maze);
        }
        else {
            // get random next direction
            int direction = random.nextInt(4) + 1;
            super.move(maze, direction);
        }
    }
}
