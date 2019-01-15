package andro.id.caroboard.ai;

/**
 * Created by qcuong98 on 1/10/19.
 */

import java.util.ArrayList;
import java.util.List;

/**
 * +1: X
 * -1: O
 */

public class Board {
    public static final int IN_PROGRESS = -2;
    public static final int DRAW = 0;

    private static final int LEN_FOR_WIN = 5;
    private static final int BOARD_SIZE = 15;

    public Position p1, p2;
    public int[][] adj;

    public Board() {
        adj = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; ++i)
            for (int j = 0; j < BOARD_SIZE; ++j)
                adj[i][j] = 0;
    }

    public Board(Board board) {
        adj = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++)
            for (int j = 0; j < BOARD_SIZE; j++)
                this.adj[i][j] = board.adj[i][j];
    }

    public int result() {
        for (int i = 0; i < BOARD_SIZE; ++i)
            for (int j = 0; j < BOARD_SIZE; ++j) {
                if (adj[i][j] != 0 && (check_line(i, j, 0, 1) || check_line(i, j, 1, 0) ||
                        check_line(i, j, 1, -1) || check_line(i, j, 1, 1)))
                    return adj[i][j];
            }

        if (getEmptyPositions().size() != 0)
            return IN_PROGRESS;
        else
            return DRAW;
    }

    private boolean inside(int x, int y) {
        return (x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE);
    }

    private boolean check_line(int i, int j, int di, int dj) {
        for (int k = 1; k < LEN_FOR_WIN; ++k) {
            int x = i + k * di, y = j + k * dj;
            if (!inside(x, y) || adj[x][y] != adj[i][j])
                return false;
        }

        /* return false if be blocked by two opponent cells */
        if (inside(i - di, j - dj) && adj[i - di][j - dj] + adj[i][j] == 0 &&
                inside(i + LEN_FOR_WIN * di, j + LEN_FOR_WIN * dj) &&
                adj[i + LEN_FOR_WIN * di][j + LEN_FOR_WIN * dj] + adj[i][j] == 0) {
            return false;
        } else {
            p1 = new Position(i, j);
            p2 = new Position(i + (LEN_FOR_WIN - 1) * di, j + (LEN_FOR_WIN - 1) * dj);
            return true;
        }
    }

    public List<Position> getEmptyPositions() {
        List<Position> emptyPositions = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (adj[i][j] == 0)
                    emptyPositions.add(new Position(i, j));
            }
        }
        return emptyPositions;
    }

    boolean checkedNear(int x, int y) {
        int minx = Math.max(0, x - 1), maxx = Math.min(x + 1, BOARD_SIZE - 1);
        int miny = Math.max(0, y - 1), maxy = Math.min(y + 1, BOARD_SIZE - 1);
        for (int i = minx; i <= maxx; ++i)
            for (int j = miny; j <= maxy; ++j)
                if (adj[i][j] != 0)
                    return true;
        return false;
    }

    public List<Position> nextPositions() {
        List<Position> nextPositions = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (adj[i][j] == 0 && checkedNear(i, j))
                    nextPositions.add(new Position(i, j));
            }
        }
        if (nextPositions.size() == 0)
            nextPositions.add(new Position((BOARD_SIZE - 1) / 2, (BOARD_SIZE - 1) / 2));
        return nextPositions;
    }

    public void performMove(int playerNo, Position p) {
        adj[p.x][p.y] = playerNo;
    }

    public Position diff(Board board) {
        for (int i = 0; i < BOARD_SIZE; ++i)
            for (int j = 0; j < BOARD_SIZE; ++j)
                if (this.adj[i][j] != board.adj[i][j])
                    return new Position(i, j);
        return new Position(-1, -1);
    }
}
