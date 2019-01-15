package andro.id.caroboard.ai;


import android.util.Log;

import java.util.List;

/**
 * Created by qcuong98 on 1/11/19.
 */
public class MCTS {
    public int level;
    private final int[] thinkTime = new int[] {0, 10000, 150000, 20000};

    public MCTS(int level) {
        this.level = level;
    }

    private int getNoOfSimulatePlayout() {
        return 8000 * this.level;
    }

    public Position findNextMove(Board board, int playerNo) {
        long end = System.currentTimeMillis() + thinkTime[level];

        Node rootNode = new Node();
        rootNode.state.board = board;
        rootNode.state.playerNo = -playerNo;

        int cnt = getNoOfSimulatePlayout();
        while (cnt >= 0 && System.currentTimeMillis() < end) {
            --cnt;
            // Phase 1 - Selection
            Node promisingNode = selectPromisingNode(rootNode);
            // Phase 2 - Expansion
            if (promisingNode.state.board.result() == Board.IN_PROGRESS)
                expandNode(promisingNode);
            // Phase 3 - Simulation
            Node nodeToExplore = promisingNode;
            if (promisingNode.childArray.size() > 0) {
                nodeToExplore = promisingNode.getRandomChildNode();
            }
            int playoutResult = simulateRandomPlayout(nodeToExplore);
            // Phase 4 - Update
            backPropogation(nodeToExplore, playoutResult);
        }
        Log.i("Number of playout", String.valueOf(getNoOfSimulatePlayout() - cnt));

        Node winnerNode = rootNode.getChildWithMaxScore();
        return rootNode.state.board.diff(winnerNode.state.board);
    }

    private Node selectPromisingNode(Node rootNode) {
        Node node = rootNode;
        while (node.childArray.size() != 0) {
            node = findBestNodeWithUCT(node);
        }
        return node;
    }

    private double uctValue(int totalVisit, double nodeWinScore, int nodeVisit) {
        if (nodeVisit == 0)
            return Double.MAX_VALUE;
        return (nodeWinScore / (double) nodeVisit)
                + Math.sqrt(2) * Math.sqrt(Math.log(totalVisit) / (double) nodeVisit);
    }

    private Node findBestNodeWithUCT(Node node) {
        double maxv = -1;
        Node ans = null;

        int parentVisit = node.state.visitCount;
        List<Node> childArray = node.childArray;
        for (int i = 0; i < childArray.size(); ++i) {
            Node c = childArray.get(i);
            double val = uctValue(parentVisit, c.state.winScore, c.state.visitCount);
            if (val > maxv) {
                maxv = val;
                ans = c;
            }
        }

        return ans;
    }

    private void expandNode(Node node) {
        List<State> possibleStates = node.state.getNextStates();
        for (int i = 0; i < possibleStates.size(); ++i) {
            State state = possibleStates.get(i);
            Node newNode = new Node(state);
            newNode.parent = node;
            newNode.state.playerNo = node.state.getOpponent();
            node.childArray.add(newNode);
        }
    }

    private int simulateRandomPlayout(Node node) {
        Node tempNode = new Node(node);
        State tempState = tempNode.state;
        int boardStatus = tempState.board.result();

        while (boardStatus == Board.IN_PROGRESS) {
            tempState.togglePlayer();
            tempState.randomPlay();
            boardStatus = tempState.board.result();
        }
        return boardStatus;
    }

    private void backPropogation(Node nodeToExplore, int playoutResult) {
        Node tempNode = nodeToExplore;
        while (tempNode != null) {
            tempNode.state.incVisitCnt();
            if (tempNode.state.playerNo == playoutResult) {
                tempNode.state.addScore(1);
            }
            tempNode = tempNode.parent;
        }
    }
}
