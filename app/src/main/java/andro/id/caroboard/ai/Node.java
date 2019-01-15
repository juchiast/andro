package andro.id.caroboard.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qcuong98 on 1/11/19.
 */
public class Node {
    public State state;
    public Node parent;
    public List<Node> childArray;

    public Node() {
        this.state = new State();
        childArray = new ArrayList<>();
    }

    public Node(State state) {
        this.state = state;
        childArray = new ArrayList<>();
    }

    public Node(State state, Node parent, List<Node> childArray) {
        this.state = state;
        this.parent = parent;
        this.childArray = childArray;
    }

    public Node(Node node) {
        this.childArray = new ArrayList<>();
        this.state = new State(node.state);
        this.parent = node.parent;

        List<Node> childArray = node.childArray;
        for (Node child : childArray) {
            this.childArray.add(new Node(child));
        }
    }
    public Node getRandomChildNode() {
        int noOfPossibleMoves = this.childArray.size();
        int selectRandom = (int) (Math.random() * noOfPossibleMoves);
        return this.childArray.get(selectRandom);
    }

    public Node getChildWithMaxScore() {
        int maxv = 0;
        Node ans = null;
        for (int i = 0; i < childArray.size(); ++i) {
            Node node = childArray.get(i);
            if (node.state.visitCount > maxv) {
                maxv = node.state.visitCount;
                ans = node;
            }
        }
        return ans;
    }
}
