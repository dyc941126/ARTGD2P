package org.dyc.algo.maxsum.acceleration;

import org.dyc.algo.maxsum.AbstractFunctionNode;
import org.dyc.core.Constraint;
import org.dyc.utilities.AlgoUtils;
import org.dyc.utilities.MultiDimDataUtils;

import java.util.*;

public class ART_GD2P extends AbstractFunctionNode {
    private long stepSize;
    public Map<Integer, Node[][]> trees;
    private Map<Integer, Long> msgEst;
    private long lb;
    private int cnt;

    public ART_GD2P(Constraint function, long stepSize) {
        super(function);
        this.stepSize = stepSize;
        this.cnt = function.dimDomains.size();
        this.trees = new HashMap<>();
        this.msgEst = new HashMap<>();
    }

    @Override
    public void init() {
        super.init();
        for (int id : this.function.dimOrdering) {
            int[] domainLength = new int[this.function.dimDomains.size()];
            int skipIdx = 0;
            for (int i = 0; i < this.function.dimDomains.size(); i++) {
                int curId = this.function.dimOrdering[i];
                domainLength[i] = this.function.dimDomains.get(curId);
                if (curId == id) {
                    skipIdx = i;
                }
            }
            Node[][] andOrTreesList = new Node[domainLength[skipIdx]][];
            int firstIdx = skipIdx == 0 ? 1 : 0;
            for (int val = 0; val < domainLength[skipIdx]; val++) {
                List<Long> allUtils = new ArrayList<>();
                Map<Long, Node> allRoot = new HashMap<>();
                int[] curAssign = new int[this.function.dimDomains.size()];
                curAssign[skipIdx] = val;
                while (true) {
                    long util = this.function.eval(curAssign);

                    long roundUtil = this.stepSize == 0? util : ((util / stepSize) + 1) * stepSize;
                    assert roundUtil >= util;
                    Node root = allRoot.get(roundUtil);
                    if (root == null) {
                        root = new Node(-1, util, domainLength[firstIdx]);
                        allRoot.put(roundUtil, root);
                        allUtils.add(roundUtil);
                    } else {
                        root.est = Long.max(root.est, util);
                    }
                    Node curNode = root;
                    for (int i = firstIdx; i < curAssign.length; i++) {
                        if (i == skipIdx)
                            continue;
                        Node foundNode = curNode.find(curAssign[i]);
                        int next = i + 1;
                        if (next == skipIdx){
                            next++;
                        }
                        if (foundNode == null) {
                            foundNode = new Node(curAssign[i], util, next < domainLength.length ? domainLength[next] : -1);
                            curNode.addChild(foundNode);
                        } else {
                            foundNode.est = Long.max(foundNode.est, util);
                        }
                        curNode = foundNode;
                    }
                    if (!MultiDimDataUtils.next(curAssign, domainLength, skipIdx))
                        break;
                }
                Collections.sort(allUtils);

                Node[] andOrTrees = new Node[allUtils.size()];
                for (int i = allUtils.size() - 1; i >= 0; i--) {
                    andOrTrees[andOrTrees.length - 1 - i] = allRoot.get(allUtils.get(i));
                }
                andOrTreesList[val] = andOrTrees;
            }
            this.trees.put(id, andOrTreesList);
        }
    }

    @Override
    public long[] max(int id) {
        super.max(id);
        long[] results = new long[this.function.dimDomains.get(id)];
        long acc = 0;
        this.msgEst.clear();
        for (int i = this.function.dimDomains.size() - 1; i > 0; i--){
            int prevId = this.function.dimOrdering[i - 1];
            int curId = this.function.dimOrdering[i];
            if (curId != id){
                acc += AlgoUtils.max(this.incomeMsg.get(curId));
            }
            if (prevId != id)
                this.msgEst.put(prevId, acc);
        }
        int skipIdx = 0;
        for (int i = 0; i < this.function.dimDomains.size(); i++){
            int curId = this.function.dimOrdering[i];
            if (curId == id){
                skipIdx = i;
            }
        }
        int firstIdx = 0;
        if (skipIdx != 0){
            acc += AlgoUtils.max(this.incomeMsg.get(this.function.dimOrdering[0]));
        }
        else {
            firstIdx = 1;
        }
        long norm = 0;
        int domainLength = this.function.dimDomains.get(id);
        for (int val = 0; val < domainLength; val++){
            Node[] andOrTrees = this.trees.get(id)[val];
            lb = Long.MIN_VALUE;
            for (Node root : andOrTrees){
                if (lb != Long.MIN_VALUE && root.est < lb - acc)
                    break;
                for (Node node : root.children) {
                    if (node == null)
                        break;
                    rec(node, firstIdx, skipIdx, 0);
                }
            }
            norm += lb;
            results[val] = lb;
        }
        norm /= results.length;
        for (int i = 0; i < results.length; i++){
            results[i] -= norm;
        }

        return results;
    }

    private void rec(Node curNode, int idx, int skipIdx, long partialUtil){
        boolean last = (idx == cnt - 1 && idx != skipIdx) || (idx == cnt - 2 && idx + 1 == skipIdx);
        int curId = this.function.dimOrdering[idx];
        if (last){
            this.currentBasicOp ++;
            long util = partialUtil + curNode.est + this.incomeMsg.get(curId)[curNode.val];
            this.currentCC++;
            this.lb = Long.max(this.lb, util);
        }
        else {
            if (idx == skipIdx){
                rec(curNode, idx + 1, skipIdx, partialUtil);
            }
            else {
                this.currentBasicOp ++;
                long pu = partialUtil + this.incomeMsg.get(curId)[curNode.val];
                long ub = pu + this.msgEst.get(curId) + curNode.est;
                for (Node node : curNode.children){
                    if (node == null)
                        break;
                    if (ub <= this.lb)
                        return;
                    rec(node, idx + 1, skipIdx, pu);
                }
            }
        }
    }

}
