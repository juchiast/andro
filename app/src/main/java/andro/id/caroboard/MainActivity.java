package andro.id.caroboard;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import andro.id.caroboard.ai.Board;

public class MainActivity extends AppCompatActivity {
    public static Driver callback = new EmptyDriver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new MenuFragment()).commit();
    }

    public void onClickPvP(View view) {
        Fragment newFragment = new CaroBoardFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        callback = new PvPDriver();
    }
}

interface Driver {
    void call(int x, int y, MyGLSurfaceView s);
}

class EmptyDriver implements Driver {

    @Override
    public void call(int x, int y, MyGLSurfaceView s) {
    }
}

class PvPDriver implements Driver {
    private Board board;
    private int currentPlayer;

    PvPDriver() {
        currentPlayer = 1;
        board = new Board();
    }

    @Override
    public void call(int x, int y, MyGLSurfaceView s) {
        if (0 == board.adj[y][x]) {
            board.adj[y][x] = currentPlayer;
            s.setBoard(x, y, currentPlayer == -1 ? 2 : currentPlayer);
            currentPlayer *= -1;
        }
    }
}