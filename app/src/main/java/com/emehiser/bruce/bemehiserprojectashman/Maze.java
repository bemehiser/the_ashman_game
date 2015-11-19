package com.emehiser.bruce.bemehiserprojectashman;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/*
 * Created by Bruce on 10/26/2015.
 *
 * Ashman Game
 *
 * Contains the state of the maze.
 * Hold references to all movers.
 * Has ability to animate.
 * Has ability to pause.
 * Has ability to move Ashman based on user commands.
 * Has ability to randomly move Ghosts.
 *
 */
public class Maze extends View implements View.OnLongClickListener, View.OnClickListener {

    // calling context
    private final Context context;

    // the canvas and gui scale factor
    private float scale;
    // the width and height of maze which will be drawn on canvas
    private final static int DRAWING_WIDTH = 14;
    private final static int DRAWING_HEIGHT = 14;
    private final static int DRAWING_BORDER = 1;

    // paint objects used for drawing on canvas
    private Paint solid; // dark grey
    private Paint empty; // light blue
    private Paint cake; // white

    // each maze square will contain either a 0 solid, 1 empty, or 2 cake
    private int[][] maze;
    // Maze numbers
    private final static int SOLID_VAL = 0;
    private final static int EMPTY_VAL = 1;
    private final static int CAKE_VAL = 2;
    // count of current cakes in maze
    private int cakeCount;

    // list of movers
    private ArrayList<String> moverTags;
    private HashMap<String, Mover> movers;

    // ashman tag so ashman can be controlled from without
    private static final String ASHMAN_TAG = "ashman_tag";

    // handler and timer for animating maze
    private Handler clockHandler;
    private Runnable clockTimer;
    private ExecutorService threadExecutor;
    private Future future;

    // animate maze for play / pause
    private boolean animateMaze;
    // game running
    private boolean gameRunning;

    // number of animations/second
    public static final int ANIMATIONS_PER_SECOND = 15;
    //    private static final long DELAY = 1000 / ANIMATIONS_PER_SECOND;
    // note: 1000 / animationsPerSecond is technically correct, but the game ran slow because
    // of the time it takes to run the game, so I shortened the delay a bit
    // this will make it seem very fast on fast phones, and isn't technically kosher
    private static final long DELAY = 500 / ANIMATIONS_PER_SECOND;

    // win or loss
    public static final int GAME_WIN = 0;
    public static final int GAME_LOSS = 1;
    // current level
    private int currentLevel;

    // user interface fields
    private TextView currentLevelText;
    private TextView cakeCountText;

    // media players for audio clips
    private MediaPlayer mediaPlayerChomp;
    private MediaPlayer mediaPlayer;

    public Maze(Context context) {
        super(context);
        this.context = context;
        initialize();
    }

    public Maze(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initialize();
    }

    public Maze(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initialize();
    }

    private void initialize() {

        // create maze with empty borders
        maze = new int[DRAWING_HEIGHT + 2 * DRAWING_BORDER][DRAWING_WIDTH + 2 * DRAWING_BORDER];

        // create movers and mover tags
        moverTags = new ArrayList<>();
        movers = new HashMap<>();

        // register play pause click
        this.setOnClickListener(this);

        // register cheat long click
        this.setOnLongClickListener(this);
    }

    private void startGameRunning() {
        gameRunning = true;
        startAnimateMaze();
    }

    private void stopGameRunning() {
        gameRunning = false;
        stopAnimateMaze();
    }

    // load the appropriate ghosts and maze for the specified level
    private void prepareGame(int level) {

        // if the level is not valid for this game
        if(level < 1 || level > 2) {
            throw new InvalidParameterException("Not a valid level: " + level);
        }

        // reset all the variables
        stopGameRunning();

        // set the current level variable
        currentLevel = level;

        // clear the movers and ashman (if any)
        moverTags.clear();
        movers.clear();

        // clear the media players
        if(mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer = null;
        }
        if(mediaPlayerChomp != null) {
            mediaPlayerChomp.stop();
            mediaPlayerChomp = null;
        }

        // set up new game board
        Mover ashman;
        Mover ghost;

        int ghostCountThisLevel = 0;
        float ghostSpeedThisLevel = 0;

        switch (level) {
            case 1:
                ghostCountThisLevel = 3;
                ghostSpeedThisLevel = .6f;
                break;
            case 2:
                ghostCountThisLevel = 5;
                ghostSpeedThisLevel = 1f;
                break;
        }

        // load level maze
        try {
            prepareMaze("level.txt");
        } catch (IOException e) {
            Log.e("prepareGame()", "IOException: error loading level " + "level.txt");
        }

        // add an ashman to the maze
        ashman = new Ashman();
        ashman.setPosition(.5f, .5f);
        ashman.setSpeed(1f);
        addMover(ASHMAN_TAG, ashman);

        // add a ghosts to the maze
        for(int i = 0; i < ghostCountThisLevel; i ++) {
            ghost = new Ghost();
            ghost.setPosition(1.5f, 13.5f);
            ghost.setSpeed(ghostSpeedThisLevel);
            addMover("ghost" + i, ghost);
        }

        // call invalidate so that we can see changes before call to animate
        invalidate();
    }


    private void startAnimateMaze() {

        // set animate maze
        animateMaze = true;
        // create our runnable thread
        prepareAnimateMaze();
    }

    public void stopAnimateMaze() {
        // set animate maze false, which will it turn stop posting events on the event handler
        animateMaze = false;
        // dispose of thread pieces
        disposeAnimateMaze();
    }

    private void prepareAnimateMaze() {
        // check for pre-existing clock timer
        if(clockTimer == null) {
            // create new thread executor, used for stopping thread
            if (threadExecutor == null) {
                threadExecutor = Executors.newSingleThreadExecutor();
            }
            // future task handler for timing next clock cycle
            if (clockHandler == null) {
                clockHandler = new Handler();
            }
            // runnable thread
            clockTimer = new Runnable() {
                @Override
                public void run() {
                    // call to animate maze actions
                    animateMazeActions();

                    // post a delay until the next clock cycle
                    if (animateMaze) {
                        clockHandler.postDelayed(this, DELAY);
                    }
                }
            };
            // register runnable in the thread executor
            threadExecutor.submit(clockTimer);
            // future task scheduler
            future = threadExecutor.submit(clockTimer);
            // start the thread
            clockTimer.run();
        }
    }

    private void disposeAnimateMaze() {
        if(clockHandler != null) {
            clockHandler = null;
        }
        if(future != null) {
            future.cancel(true);
            future = null;
        }
        if(clockTimer != null) {
            clockTimer = null;
        }
    }

    private void animateMazeActions() {
        // call to movers to animate
        animateMovers();
        // call to update ui
        animateUI();
        // check for game win
        if(cakeCount == 0) {
            endGame(GAME_WIN);
        }
    }

    private void animateMovers() {
        // moveMover all the movers
        for(String moverTag : moverTags) {
            Mover mover = movers.get(moverTag);
            mover.move(this);
        }
    }

    private void animateUI() {

        // update the current level
        if(currentLevelText == null) {
            currentLevelText = (TextView) ((Activity) context).findViewById(R.id.level_text);
        }
        // update cake count
        if(cakeCountText == null) {
            cakeCountText = (TextView) ((Activity) context).findViewById(R.id.cakes_left_text);
        }

        cakeCountText.setText(String.format("%s: %d",context.getString(R.string.cakes_left), cakeCount));
        currentLevelText.setText(String.format("%s: %d",context.getString(R.string.level), currentLevel));
    }

    // checks if the maze restrains the mover from moving to this location
    public boolean canMove(float destinationX, float destinationY, float moverRadius, int moverDirection) {

        // make sure the destination center is within the bounds of the maze
        if (destinationY + moverRadius > DRAWING_WIDTH ||
                destinationX + moverRadius > DRAWING_WIDTH ||
                destinationY - moverRadius < 0 ||
                destinationX - moverRadius < 0) {
            return false;
        }

        // find if our mover center is inside a solid block
        // get the maze block type at the destination location
        int blockType = getMazePos((int) destinationX, (int) destinationY);
        // if it our end position is solid, we can't moveMover there
        if(blockType == SOLID_VAL) {
            return false;
        }

        // find if there is a solid block within the radius of the mover
        float finalX = destinationX;
        float finalY = destinationY;
        switch (moverDirection) {
            case Mover.UP:
                finalY -= moverRadius;
                break;
            case Mover.DOWN:
                finalY += moverRadius;
                break;
            case Mover.LEFT:
                finalX -= moverRadius;
                break;
            case Mover.RIGHT:
                finalX += moverRadius;
                break;
            // default, our mover is not moving, and we don't need to calculate because we haven't moved
            default:
                return true;
        }
        // get the block type at the final location
        int destinationBlockType = getMazePos((int) finalX,(int) finalY);
        if(destinationBlockType == SOLID_VAL) {
            return false;
        }

        // we are within the edges of the maze, and we are not touching a solid block
        return true;
    }

    private void moveMover(String tag, int direction) {

        Mover mover = movers.get(tag);
        // check for null
        if(mover == null) {
            throw new NullPointerException("Mover has not been added to map");
        }
        mover.move(this, direction);
    }

    public void moveAshman(int direction) {
        // move ashman if our maze is set to animate
        if(animateMaze) {
            moveMover(ASHMAN_TAG, direction);
        }
    }

    @Override
    public void invalidate(int l, int t, int r, int b) {

        // calculate location on canvas, based on the scaling factor
        l = (int) (l * scale);
        t = (int) (t * scale);
        r = (int) (r * scale);
        b = (int) (b * scale);

        // call super to invalidate canvas
        super.invalidate(l, t, r, b);
    }

    private void playStartSound(MediaPlayer.OnCompletionListener listener) {

        // play the startup sound
        playSound("pacman_beginning.wav", mediaPlayer).setOnCompletionListener(listener);
    }

    private void playPauseSound(MediaPlayer.OnCompletionListener listener) {

        // play the stop game sound
        playSound("pacman_intermission.wav", mediaPlayer).setOnCompletionListener(listener);
    }

    private void playDeathSound() {

        // play the death sound
        playSound("pacman_death.wav", mediaPlayer);
    }

    private void playChompSound() {

        // play the chomp sound
        playSound("pacman_chomp.wav", mediaPlayerChomp);
    }

    private MediaPlayer playSound(String assetName, MediaPlayer mediaPlayer) {
        // hook player to this chomp sound and start it
        if(mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        // already playing
        if(mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(0);
        }
        // new up player and start playback
        else {
            try {
                AssetFileDescriptor assetFileDescriptor = context.getAssets().openFd(assetName);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.reset();
                mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());

                mediaPlayer.prepare();
                mediaPlayer.start();

                // release media player after playback ends
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                    }
                });


            } catch (IOException e) {
                Log.e("Error", "IOException in playChompSound() mediaPlayer.prepare() " + e);
                return mediaPlayer;
            }
        }

        return mediaPlayer;
    }

    private void beginGame() {
        // call to start game running
        startGameRunning();
    }

    // ends the game with a win or a loss
    public void endGame(int outcome) {

        // call to stop the game
        stopGameRunning();

        // check game outcome
        if(outcome == GAME_WIN) {

            // load next level
            if(currentLevel == 1) {
                // pat the user on the back
                Toast.makeText(context, R.string.level_two_warning, Toast.LENGTH_SHORT).show();
                prepareGame(2);
                // play the intermission music
                // wait for the music to finish playing
                playPauseSound(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // call to start game
                        beginGame();
                    }
                });
            }
            else {

                // play end music
                playPauseSound(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // I don't actually want to do anything. What a waste
                        // now if I were actually using the credits sound... (dreamy sigh)
                    }
                });

                // the storyteller sat back and looked pleased with his work
                // the game was over
                // it had been a good game
                // farewell, the storyteller said

                Toast.makeText(context, R.string.end_story_01, Toast.LENGTH_LONG).show();
                Toast.makeText(context, R.string.end_story_02, Toast.LENGTH_LONG).show();
                Toast.makeText(context, R.string.end_story_03, Toast.LENGTH_LONG).show();
                Toast.makeText(context, R.string.end_story_04, Toast.LENGTH_LONG).show();
            }
        }
        else if(outcome == GAME_LOSS) {
            // play the "you are sooooo dead" sound
            playDeathSound();
            // make user feel even worse about it
            Toast.makeText(context, "You Lost!", Toast.LENGTH_SHORT).show();
            // let user try to redeem themselves (if they dare)
            Toast.makeText(context, R.string.new_game_prompt, Toast.LENGTH_SHORT).show();
        }
        // else magic numbers
        else {
            throw new InvalidParameterException("invalid end game code " + outcome);
        }
    }

    public void newGame() {
        // load level 1
        prepareGame(1);

        // play new game sound
        playStartSound(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // call to start game
                beginGame();
            }
        });
    }

    private void addMover(String tag, Mover mover) {
        // add mover and tag
        moverTags.add(tag);
        movers.put(tag, mover);
    }

    // tells you if the incoming mover collides with any of the movers in the list, except for itself
    public boolean collision(Mover mover) {

        // for each mover in movers
        for(String moverTag : moverTags) {
            Mover m = movers.get(moverTag);
            // if they are not the same objects, see if they bump into each other
            if(! mover.equals(m)) {
                // return if there is a collision. May need updated if I change visibility from package private
                if(collision(mover.x, mover.y, mover.radius, m.x, m.y, m.radius)) {
                    return true;
                }
            }
        }
        return false;
    }

    // finds whether or not two circles at given x and y coordinates, and with given radii, collide
    private boolean collision(float ax, float ay, float aRad, float bx, float by, float bRad) {

        // get x and y distance between two points
        double dx = ax - bx;
        double dy = ay - by;

        // pythagorean theorem
        double distance = Math.sqrt(dx * dx + dy * dy);

        // if the distance is smaller than our added radii, we have a collision
        if (distance < aRad + bRad) {
            // collision detected
            return true;
        }
        // collision not detected
        return false;
    }

    private void prepareMaze(String resourcefileName) throws IOException {

        Scanner scanner;

        // set total cakes to zero
        cakeCount = 0;

        // open scanner and read file into array
        InputStream inputStream = context.getAssets().open(resourcefileName);
        scanner = new Scanner(inputStream);
        // read a maze from the file
        for(int i = 0; i < DRAWING_HEIGHT; i ++) {
            // scan each line and split it into the maze
            char[] temp = scanner.nextLine().toCharArray();
            for(int j = 0; j < DRAWING_WIDTH; j ++) {
                int t = Character.getNumericValue(temp[j]);
                maze[i + DRAWING_BORDER][j + DRAWING_BORDER] = t;
                // add all the cakes to our current cake count
                if(t == CAKE_VAL) {
                    cakeCount ++;
                }
            }
        }
    }

    @Override
    public void onMeasure(int widthMeasurespec, int heightMeasurespec) {

        // get the width and height of the container
        int width = MeasureSpec.getSize(widthMeasurespec);
        int height = MeasureSpec.getSize(heightMeasurespec);

        // find the scaling factor
        float scaleWidth = width / DRAWING_WIDTH;
        float scaleHeight = height / DRAWING_HEIGHT;

        // get the maximum scaling factor
        scale = scaleWidth < scaleHeight ? scaleWidth : scaleHeight;

        // find the max height that our maze can be based on the allocated containter
        int maxWidth = (int) (scale * DRAWING_WIDTH);
        int maxHeight = (int) (scale * DRAWING_HEIGHT);

        // make a call to set the actual measured dimensions based on our calculations
        setMeasuredDimension(maxWidth, maxHeight);
    }

    @Override
    public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {

        super.onSizeChanged(width, height, oldWidth, oldHeight);

    }

    @Override
    public void onDraw(Canvas canvas) {

        // remember canvas

        // scale canvas
        canvas.scale(scale, scale);

        // populate canvas
        // populate canvas with squares
        drawMazeSquares(canvas);
        // draw cakes on the canvas
        drawCakes(canvas);
        // draw movers on canvas
        drawMovers(canvas);
    }

    private void drawMovers(Canvas canvas) {

        // draw all movers
        for(String moverTag : moverTags) {
            Mover mover = movers.get(moverTag);
            mover.drawMover(canvas);
        }
    }

    private void drawCakes(Canvas canvas) {

        for(int y = 0; y < DRAWING_HEIGHT; y ++) {
            for(int x = 0; x < DRAWING_WIDTH; x ++) {
                // draw all cakes in squares
                if(getMazePos(x, y) == CAKE_VAL) {
                    canvas.drawCircle(x + .5f, y + .5f, .22f, cake);
                }
            }
        }
    }

    private void drawMazeSquares(Canvas canvas) {

        // if paint objects are null, initialize them to the appropriate colors
        if(solid == null) {
            solid = new Paint();
            solid.setStyle(Paint.Style.FILL);
            int SOLID_COLOR = Color.BLUE;
            solid.setColor(SOLID_COLOR);
        }
        // empty square yor
        if(empty == null) {
            empty = new Paint();
            empty.setStyle(Paint.Style.FILL);
            int EMPTY_COLOR = Color.DKGRAY;
            empty.setColor(EMPTY_COLOR);
        }

        // cake yor
        if(cake == null) {
            cake = new Paint();
            cake.setStyle(Paint.Style.FILL);
            int CAKE_COLOR = Color.WHITE;
            cake.setColor(CAKE_COLOR);
        }

        // paint each square with correct yor
        for(int x = 0; x < DRAWING_HEIGHT; x ++) {
            for(int y = 0; y < DRAWING_WIDTH; y ++) {
                // draw squares
                canvas.drawRect((float) x, (float) y, (float) x + 1, (float) y + 1, getMazePos(x, y) == 0 ? solid : empty);
            }
        }
    }

    // removes cake in square
    public void chompCake(float x, float y) {

        // cast the values to ints
        int intX = (int) x;
        int intY = (int) y;

        // get the current cake type
        int squareType = getMazePos(intX, intY);
        // if the maze position is a cake, eat the cake
        if(squareType == Maze.CAKE_VAL) {
            setMazePos(intX, intY, Maze.EMPTY_VAL);
            // decrement cake count
            cakeCount --;
            // play chomp sound
            playChompSound();
        }
    }

    // get the value at maze[row][col] with accounting for border
    private int getMazePos(int x, int y) throws IndexOutOfBoundsException {
        if(x < 0 || x > DRAWING_WIDTH
                || y < 0 || y > DRAWING_HEIGHT)
            throw new IndexOutOfBoundsException("getMazePos(int, int) must have values within DIMENSION_WIDTH and DIMENSION_HEIGHT");
        return maze[y + 1][x + 1];
    }

    // set a maze position to either 0, 1, or 2
    private void setMazePos(int x, int y, int val) {

        // check value
        if(val < 0 || val > 2)
            throw new InvalidParameterException("Value must be either 0, 1, or 2");
        // make sure the value is within the border of the maze
        if(y < 0 || y > DRAWING_HEIGHT
                || x < 0 || x > DRAWING_WIDTH) {
            throw new IndexOutOfBoundsException("Value falls outside of the dimensions of the maze");
        }
        // set the maze value
        maze[y + 1][x + 1] = val;
    }

    @Override
    public boolean onLongClick(View v) {

        // remove all but one cake
        for(int i = 0; i < DRAWING_HEIGHT && cakeCount > 1; i ++) {
            for(int j = 0; j < DRAWING_WIDTH && cakeCount > 1; j ++) {
                if(getMazePos(j, i) == CAKE_VAL) {
                    setMazePos(j, i, EMPTY_VAL);
                    cakeCount --;
                }
            }
        }
        // say that we handled the event
        return true;
    }

    @Override
    public void onClick(View view) {

        // onClick pauses and resumes game
        if(gameRunning) {
            if (animateMaze) {
                stopAnimateMaze();
            } else {
                startAnimateMaze();
            }
        }
    }
}
// life is good