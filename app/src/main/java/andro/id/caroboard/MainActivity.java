package andro.id.caroboard;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import andro.id.caroboard.ai.Board;
import andro.id.caroboard.ai.MCTS;
import andro.id.caroboard.ai.Position;

public class MainActivity extends AppCompatActivity {
    public static Driver callback = new EmptyDriver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new MenuFragment()).commit();
    }

    public void onClickPvP(View view) {
        callback = new PvPDriver();

        Fragment newFragment = new CaroBoardFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void onClickVCPU(View view) {
        callback = new VCPUDriver();

        Fragment newFragment = new CaroBoardFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void onClickCvC(View view) {
        callback = new CvCDriver();

        Fragment newFragment = new CaroBoardFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
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


class VCPUDriver implements Driver {
    private final int CPU = -1;
    private final int HUMAN = 1;

    private final Board board;
    private int currentPlayer;

    VCPUDriver() {
        currentPlayer = HUMAN;
        board = new Board();
        ai = new MCTS(1);
    }

    @Override
    public void call(int x, int y, MyGLSurfaceView s) {
        if (board.result() != Board.IN_PROGRESS) {
            return;
        }
        if (0 == board.adj[x][y] && HUMAN == currentPlayer) {
            board.adj[x][y] = HUMAN;
            s.setBoard(x, y, HUMAN == -1 ? 2 : HUMAN);
            if (!PvPDriver.setAndCheckWin(board, HUMAN, s)) {
                currentPlayer = CPU;
                new Think(s).execute();
            }
        }
    }

    private final MCTS ai;

    private class Think extends AsyncTask<Void, Void, Position> {
        private final MyGLSurfaceView s;

        Think(MyGLSurfaceView s) {
            this.s = s;
        }

        @Override
        protected Position doInBackground(Void... voids) {
            return ai.findNextMove(board, CPU);
        }

        @Override
        protected void onPostExecute(Position p) {
            if (0 == board.adj[p.x][p.y] && CPU == currentPlayer) {
                board.adj[p.x][p.y] = CPU;
                s.setBoard(p.x, p.y, CPU == -1 ? 2 : CPU);
                if (!PvPDriver.setAndCheckWin(board, CPU, s)) {
                    currentPlayer = HUMAN;
                }
            }
        }
    }
}

class PvPDriver implements Driver {
    private final Board board;
    private int currentPlayer;

    PvPDriver() {
        currentPlayer = 1;
        board = new Board();
    }

    @Override
    public void call(int x, int y, MyGLSurfaceView s) {
        if (board.result() != Board.IN_PROGRESS) {
            return;
        }
        if (0 == board.adj[x][y]) {
            board.adj[x][y] = currentPlayer;
            s.setBoard(x, y, currentPlayer == -1 ? 2 : currentPlayer);
            setAndCheckWin(board, currentPlayer, s);
            currentPlayer *= -1;
        }
    }

    static boolean setAndCheckWin(Board board, int lastPlayer, MyGLSurfaceView s) {
        if (board.result() != Board.IN_PROGRESS && board.result() != Board.DRAW) {
            int winX = (board.p1.x + board.p2.x) / 2;
            int winY = (board.p1.y + board.p2.y) / 2;

            int yy = board.p1.x < board.p2.x ? board.p1.y : board.p2.y;
            int direction;
            if (winY == board.p1.y) {
                direction = 2;
            } else if (winX == board.p1.x) {
                direction = 0;
            } else if (yy < winY) {
                direction = 3;
            } else {
                direction = 1;
            }
            int texture = 3 + direction + 4 * (lastPlayer == -1 ? 1 : 0);
            s.setWin(winX, winY, texture);
            return true;
        }
        return false;
    }
}

class CvCDriver implements Driver {
    private final Board board;
    private int currentPlayer;
    private final MCTS ai;

    CvCDriver() {
        board = new Board();
        currentPlayer = 1;
        ai = new MCTS(1);
    }

    @Override
    public void call(int x, int y, MyGLSurfaceView s) {
        if (board.result() != Board.IN_PROGRESS) {
            return;
        }
        new Think(s).execute();
    }

    private class Think extends AsyncTask<Void, Void, Position> {
        private final MyGLSurfaceView s;

        Think(MyGLSurfaceView s) {
            this.s = s;
        }

        @Override
        protected Position doInBackground(Void... voids) {
            return ai.findNextMove(board, currentPlayer);
        }

        @Override
        protected void onPostExecute(Position p) {
            if (0 == board.adj[p.x][p.y]) {
                board.adj[p.x][p.y] = currentPlayer;
                s.setBoard(p.x, p.y, currentPlayer == -1 ? 2 : currentPlayer);
                if (!PvPDriver.setAndCheckWin(board, currentPlayer, s)) {
                    currentPlayer *= -1;
                }
            }
        }
    }
}