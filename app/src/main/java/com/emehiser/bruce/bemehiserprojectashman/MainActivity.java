package com.emehiser.bruce.bemehiserprojectashman;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Maze maze;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get maze
        maze = (Maze) findViewById(R.id.maze);
        // start a new game
        maze.newGame();

        // set this as button listener on controls
        findViewById(R.id.up_button).setOnClickListener(this);
        findViewById(R.id.down_button).setOnClickListener(this);
        findViewById(R.id.left_button).setOnClickListener(this);
        findViewById(R.id.right_button).setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {

            Toast.makeText(this, "Bruce Emehiser\nFinal Project, Ashman\nCSCD 372 Android Mobile Development\nFall 2015", Toast.LENGTH_LONG).show();
            return true;
        }
        if(id == R.id.action_new_game) {
            maze.newGame();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.up_button:
                maze.moveAshman(Mover.UP);
                break;
            case R.id.down_button:
                maze.moveAshman(Mover.DOWN);
                break;
            case R.id.left_button:
                maze.moveAshman(Mover.LEFT);
                break;
            case R.id.right_button:
                maze.moveAshman(Mover.RIGHT);
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        maze.stopAnimateMaze();
    }
}
