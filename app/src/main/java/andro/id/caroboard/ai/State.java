package andro.id.caroboard.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qcuong98 on 1/11/19.
 */
public class State {
    Board board;
    int playerNo; //1, -1
    int visitCount;
    int winScore;

    public State() {
        board = new Board();
    }

    public State(Board board) {
        this.board = new Board(board);
    }

    public State(State state) {
        this.board = new Board(state.board);
        this.playerNo = state.playerNo;
        this.visitCount = state.visitCount;
        this.winScore = state.winScore;
    }

    public List<State> getNextStates() {
        List<State> possibleStates = new ArrayList<>();
        List<Position> availablePositions = board.nextPositions();

        for (int i = 0; i < availablePositions.size(); ++i) {
            Position p = availablePositions.get(i);
            State newState = new State(this.board);
            newState.playerNo = -this.playerNo;
            newState.board.performMove(newState.playerNo, p);
            possibleStates.add(newState);
        }
        return possibleStates;
    }

    void incVisitCnt() {
        ++visitCount;
    }

    void addScore(int result) {
        winScore += result;
    }

    public void randomPlay() {
        List<Position> availablePositions = this.board.getEmptyPositions();
        int totalPossibilities = availablePositions.size();
        int selectRandom = (int) (Math.random() * totalPossibilities);
        this.board.performMove(this.playerNo, availablePositions.get(selectRandom));
    }

    public int getOpponent() {
        return -this.playerNo;
    }

    public void togglePlayer() {
        this.playerNo = getOpponent();
    }
}
