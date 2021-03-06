package org.dyc.algo.maxsum.acceleration;

import java.util.Arrays;

public class Node {
    int val;
    long est;
    Node[] children;
    private int pointer;
    private int limit;

    public Node(int val, long est, int limit) {
        this.val = val;
        this.est = est;
        this.limit = limit;
        if (limit > 0)
            this.children = new Node[1];
    }

    public void addChild(Node child){
        if (this.limit <= 0){
            throw new RuntimeException("error");
        }
        if (this.children.length == this.pointer){
            Node[] newArr = new Node[Integer.min(2 * this.children.length, this.limit)];
            System.arraycopy(this.children, 0, newArr, 0, this.children.length);
            this.children = newArr;
        }
        this.children[this.pointer] = child;
        this.pointer++;
    }

    public Node find(int val){
        if (this.pointer == 0)
            return null;
        // we only need to check the last position because we sequentially explore the space
        return this.children[this.pointer - 1].val == val ? this.children[this.pointer - 1] : null;
    }

}
