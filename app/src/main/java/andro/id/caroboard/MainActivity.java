package andro.id.caroboard;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

import andro.id.caroboard.ai.Board;
import andro.id.caroboard.ai.MCTS;
import andro.id.caroboard.ai.Position;

public class MainActivity extends AppCompatActivity {
    public static Driver callback = new EmptyDriver();
    MyVideoView background;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_main);
        background = findViewById(R.id.background_video);
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.stars);
        background.setVideoURI(uri);
        background.setTag(19981007);
        background.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.start();
            }
        });
        background.start();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new MenuFragment()).commit();
    }

    void addFragment() {
        Fragment newFragment = new CaroBoardFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void onClickPvP(View view) {
        addFragment();
        callback = new PvPDriver(this);
    }

    public void onClickVCPU(View view) {
        addFragment();
        callback = new VCPUDriver();
    }

    public void onClickCvC(View view) {
        addFragment();
        callback = new CvCDriver();
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                          View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    public void boardReady(View view) {
        callback.boardReady(view);
    }
}

interface Driver {
    void call(int x, int y, MyGLSurfaceView s);

    void boardReady(View view);
}

class EmptyDriver implements Driver {

    @Override
    public void call(int x, int y, MyGLSurfaceView s) {
    }

    @Override
    public void boardReady(View view) {
    }
}


class VCPUDriver implements Driver {
    private final int CPU = -1;
    private final int HUMAN = 1;

    private final Board board;
    private int currentPlayer;
    private TextView text;

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
            text.setText(getText());
        }
    }

    @Override
    public void boardReady(View view) {
        text = view.findViewById(R.id.game_text);
        text.setText(getText());
    }

    private String getText() {
        int res = board.result();
        if (res == Board.IN_PROGRESS) {
            return currentPlayer == HUMAN ? "Human Turn" : "Thinking...";
        } else if (res == Board.DRAW) {
            return "Draw!";
        } else {
            return currentPlayer == HUMAN ? "Human wins" : "CPU Wins";
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
                text.setText(getText());
            }
        }
    }
}

class PvPDriver implements Driver {
    private final Board board;
    private final MainActivity main;
    private int currentPlayer;
    private TextView text;

    PvPDriver(MainActivity main) {
        currentPlayer = 1;
        this.main = main;
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
            if (!setAndCheckWin(board, currentPlayer, s)) {
                currentPlayer *= -1;
            }
            text.setText(getText());
        }
    }

    String getText() {
        int res = board.result();
        if (board.result() == Board.IN_PROGRESS) {
            return currentPlayer == -1 ? "X Turn" : "O Turn";
        } else if (res == Board.DRAW) {
            return "Draw!";
        } else {
            return res == -1 ? "X Wins" : "O Win";
        }
    }

    @Override
    public void boardReady(View view) {
        text = view.findViewById(R.id.game_text);
        text.setText(getText());
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
    private TextView text;
    private boolean thinking;

    CvCDriver() {
        board = new Board();
        currentPlayer = 1;
        ai = new MCTS(1);
        thinking = false;
    }

    @Override
    public void call(int x, int y, MyGLSurfaceView s) {
        if (board.result() != Board.IN_PROGRESS) {
            return;
        }
        if (thinking) {
            return;
        }
        thinking = true;
        text.setText(getText());
        new Think(s).execute();
    }

    @Override
    public void boardReady(View view) {
        text = view.findViewById(R.id.game_text);
        text.setText(getText());
    }

    private String getText() {
        int res = board.result();
        if (res == Board.IN_PROGRESS) {
            if (thinking) {
                return currentPlayer == -1 ? "X is thinking" : "O is thinking";
            } else {
                return "Click to continue";
            }
        } else if (res == Board.DRAW) {
            return "Draw!";
        } else {
            return currentPlayer == -1? "X wins" : "O wins";
        }
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
            thinking = false;
            if (0 == board.adj[p.x][p.y]) {
                board.adj[p.x][p.y] = currentPlayer;
                s.setBoard(p.x, p.y, currentPlayer == -1 ? 2 : currentPlayer);
                if (!PvPDriver.setAndCheckWin(board, currentPlayer, s)) {
                    currentPlayer *= -1;
                }
                text.setText(getText());
            }
        }
    }
}