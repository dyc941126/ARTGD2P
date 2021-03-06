package org.dyc.algo.maxsum.acceleration;

import org.dyc.algo.maxsum.AbstractFunctionNode;
import org.dyc.algo.maxsum.acceleration.utility_digesters.*;
import org.dyc.core.Constraint;
import org.dyc.utilities.AlgoUtils;

import java.util.*;

public class PTS extends AbstractFunctionNode {
    private Map<Integer, Constraint> uninformedEst;
    private int k;
    private Map<Integer, Map<Integer, Constraint[]>> informedEst;
    public Map<Integer, Node[][]> trees;
    private Map<Integer, Long> msgEst;
    private long lb;
    private Map<Integer, List<Integer>> andOrIndexes;
    private int[] domainLength;
    public Map<Integer, long[][]> sortedUtils;
    public Map<Integer, Map<Node, Integer>[]> sortedTrees;
    private Map<Integer, int[][]> flags;
    private long stepSize;
    private String weightedCriterion;
    private List<Integer> curRemainingIndexes;
    private int curTimestep = 0;

    public PTS(Constraint function, int k, int stepSize, String weightedCriterion) {
        super(function);
        this.k = k;
        this.uninformedEst = new HashMap<>();
        this.informedEst = new HashMap<>();
        this.msgEst = new HashMap<>();
        this.andOrIndexes = new HashMap<>() ;
        this.domainLength = new int[this.function.dimDomains.size()];
        this.trees = new HashMap<>();
        this.sortedTrees = new HashMap<>();
        this.sortedUtils = new HashMap<>();
        for (int i = 0; i < this.function.dimDomains.size(); i++){
            domainLength[i] = this.function.dimDomains.get(this.function.dimOrdering[i]);
        }
        this.stepSize = stepSize;
        this.weightedCriterion = weightedCriterion;
    }

    @Override
    public void init() {
        for (int i = 0; i < this.function.dimDomains.size(); i++){
            List<Integer> andOrIndexes = new ArrayList<>();
            for (int j = 0; j < this.function.dimDomains.size(); j++){
                if (i != j){
                    andOrIndexes.add(j);
                    if (andOrIndexes.size() == this.k)
                        break;
                }
            }
            this.andOrIndexes.put(i, andOrIndexes);
        }

        Constraint data = this.function;
        for (int i = this.function.dimDomains.size() - 2; i >= this.k; i--){
            int curId = this.function.dimOrdering[i];
            int succId = this.function.dimOrdering[i + 1];
            data = data.max(succId);
            this.uninformedEst.put(curId, data);
            this.informedEst.put(curId, new HashMap<>());
        }

        for (int i = this.function.dimDomains.size() - 2; i >= this.k; i--){
            int curId = this.function.dimOrdering[i];
            for (int j = this.function.dimDomains.size() - 1; j > i; j--){
                int informerId = this.function.dimOrdering[j];
                Constraint[] allData = new Constraint[this.function.dimDomains.get(informerId)];
                for (int informVal = 0; informVal < this.function.dimDomains.get(informerId); informVal++){
                    data = this.uninformedEst.getOrDefault(informerId, this.function);
                    data = data.conditionOn(informerId, informVal);
                    for (int k = j - 1; k > i; k--){
                        data = data.max(this.function.dimOrdering[k]);
                    }
                    allData[informVal] = data;
                }
                this.informedEst.get(curId).put(informerId, allData);
            }
        }

        for (int id : this.function.dimOrdering){
            int[] domainLength = new int[this.function.dimDomains.size()];
            int skipIdx = 0;
            Set<Integer> fixedIndexes = new HashSet<>();
            List<Integer> andOrIndexes = new ArrayList<>();
            Map<Integer, Integer> limits = new HashMap<>();
            for (int i = 0; i < this.function.dimDomains.size(); i++){
                int curId = this.function.dimOrdering[i];
                domainLength[i] = this.function.dimDomains.get(curId);
                if (curId == id){
                    skipIdx = i;
                    fixedIndexes.add(i);
                }
                else if (andOrIndexes.size() < k){
                    if (andOrIndexes.size() > 0)
                        limits.put(andOrIndexes.get(andOrIndexes.size() - 1), domainLength[i]);
                    andOrIndexes.add(i);
                    fixedIndexes.add(i);
                }
            }
            limits.put(andOrIndexes.get(andOrIndexes.size() - 1), -1);
            Node[][] andOrTreesList = new Node[domainLength[skipIdx]][];
            long[][] sortedUtilList = new long[domainLength[skipIdx]][];
            Map<Node, Integer>[] sortedTreeList = new Map[domainLength[skipIdx]];
            for (int val = 0; val < domainLength[skipIdx]; val++) {
                List<Long> allWeight = new ArrayList<>();
                Map<Long, Node> allRoot = new HashMap<>();
                Map<Long, List<Node>> utilRoot = new HashMap<>();
                int[] curAssign = new int[this.function.dimDomains.size()];
                curAssign[skipIdx] = val;
                while (true) {
                    AbstractUtilityDigest utilityDigest = createUtilityDigest(fixedIndexes, curAssign);
                    utilityDigest.digest();
                    long weight = utilityDigest.weight;
                    weight = ((weight / stepSize) + 1) * stepSize;
                    long maxUtil = utilityDigest.maxUtil;
                    Node root = allRoot.get(weight);

                    if (root == null) {
                        root = new Node(-1, maxUtil, domainLength[andOrIndexes.get(0)]);
                        allRoot.put(weight, root);
                        allWeight.add(weight);
                    } else {
                        root.est = Long.max(root.est, maxUtil);
                    }
                    Node curNode = root;
                    for (int i : andOrIndexes) {
                        Node foundNode = curNode.find(curAssign[i]);
                        if (foundNode == null) {
                            foundNode = new Node(curAssign[i], maxUtil, limits.get(i));
                            curNode.addChild(foundNode);
                        } else {
                            foundNode.est = Long.max(foundNode.est, maxUtil);
                        }
                        curNode = foundNode;
                    }
                    if (!next(curAssign, domainLength, andOrIndexes))
                        break;
                }
                Collections.sort(allWeight);
                Node[] andOrTrees = new Node[allWeight.size()];
                for (int i = allWeight.size() - 1; i >= 0; i--) {
                    andOrTrees[andOrTrees.length - 1 - i] = allRoot.get(allWeight.get(i));
                }
                List<Long> allUtilList = new ArrayList<>();
                for (Node node : allRoot.values()) {
                    long util = node.est;
                    allUtilList.add(util);
                    if (!utilRoot.containsKey(util)) {
                        utilRoot.put(util, new ArrayList<>());
                    }
                    utilRoot.get(util).add(node);
                }
                allUtilList.sort(Comparator.reverseOrder());
                long[] utils = new long[allUtilList.size()];
                Map<Node, Integer> sortedTrees = new HashMap<>();
                for (int i = 0; i < utils.length; i++) {
                    utils[i] = allUtilList.get(i);
                    Node node = utilRoot.get(utils[i]).remove(0);
                    sortedTrees.put(node, i);
                }
                sortedUtilList[val] = utils;
                sortedTreeList[val] = sortedTrees;
                andOrTreesList[val] = andOrTrees;
            }
            this.trees.put(id, andOrTreesList);
            this.sortedUtils.put(id, sortedUtilList);
            this.sortedTrees.put(id, sortedTreeList);
        }
        this.flags = new HashMap<>();
        for (int id : this.sortedUtils.keySet()){
            int[][] flags = new int[this.function.dimDomains.get(id)][];
            long[][] utils = this.sortedUtils.get(id);
            for (int i = 0; i < flags.length; i++){
                flags[i] = new int[utils[i].length];
            }
            this.flags.put(id, flags);
        }
    }

    @Override
    public long[] max(int id) {
        super.max(id);
        this.curRemainingIndexes = new ArrayList<>();
        this.curTimestep ++;
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
        for (int i = 0; i < this.function.dimDomains.size(); i++){
            if (i == skipIdx || this.andOrIndexes.get(skipIdx).contains(i))
                continue;
            this.curRemainingIndexes.add(i);
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
            int[] curAssign = new int[this.function.dimDomains.size()];
            curAssign[skipIdx] = val;
            Node[] andOrTrees = this.trees.get(id)[val];
            lb = Long.MIN_VALUE;
            long prevLb = lb;
            long[] sortedUtils = this.sortedUtils.get(id)[val];
            Map<Node, Integer> sortedTrees = this.sortedTrees.get(id)[val];
            int[] fulfilled = this.flags.get(id)[val];
            int pointer = sortedUtils.length - 1;
            int lo = 0;
            for (Node root : andOrTrees){
                if (lo > pointer)
                    break;
                if (lb != Long.MIN_VALUE && root.est < lb - acc)
                    continue;
                for (Node node : root.children) {
                    if (node == null)
                        break;
                    rec(node, firstIdx, skipIdx, 0, curAssign);
                }
                int index = sortedTrees.get(root);
                if (fulfilled[index] == this.curTimestep){
                    throw new RuntimeException();
                }
                fulfilled[index] = this.curTimestep;
                for (int i = lo; i <= pointer; i++){
                    if (this.curTimestep != fulfilled[i])
                        break;
                    lo++;
                }
                if (prevLb != lb){
                    prevLb = lb;
                    if (lo > pointer)
                        break;
                    int newPointer = binarySearch(sortedUtils, lb - acc, lo, pointer);
                    if (newPointer < 0)
                        break;
                    pointer = newPointer;
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

    private void rec(Node curNode, int idx, int skipIdx, long partialUtil, int[] curAssign){
        List<Integer> andOrIndexes = this.andOrIndexes.get(skipIdx);
        boolean last = idx == andOrIndexes.get(andOrIndexes.size() - 1);
        int curId = this.function.dimOrdering[idx];
        if (last){
            curAssign[idx] = curNode.val;
            this.currentBasicOp ++;
            int lastIdx = this.function.dimDomains.size() - 1;
            if (idx == lastIdx || idx == lastIdx - 1 && lastIdx == skipIdx) {
                long util = partialUtil + curNode.est + this.incomeMsg.get(curId)[curNode.val];
                this.currentCC++;
                this.lb = Long.max(this.lb, util);
            }
            else {
                long pu = partialUtil + this.incomeMsg.get(curId)[curNode.val];
                long ub = pu + this.msgEst.get(curId) + curNode.est;

                if (ub > this.lb){
                    fdspRec(curAssign, 0, skipIdx, pu);
                }
            }
        }
        else {
            if (idx == skipIdx){
                rec(curNode, idx + 1, skipIdx, partialUtil, curAssign);
            }
            else {
                this.currentBasicOp ++;
                long pu = partialUtil + this.incomeMsg.get(curId)[curNode.val];
                long ub = pu + this.msgEst.get(curId) + curNode.est;
                curAssign[idx] = curNode.val;
                for (Node node : curNode.children){
                    if (node == null)
                        break;
                    if (ub <= this.lb)
                        return;

                    rec(node, idx + 1, skipIdx, pu, curAssign);
                }
            }
        }
    }

    private void fdspRec(int[] curAssign, int curRIdx, int skipIdx, long partialUtil){
        int curIdx = this.curRemainingIndexes.get(curRIdx);
        boolean last = curRIdx == this.curRemainingIndexes.size() - 1;
        int curId = this.function.dimOrdering[curIdx];
        if (last){
            if (curIdx == skipIdx){
                this.currentCC++;
                long util = partialUtil + this.function.eval(curAssign);
                lb = Long.max(lb, util);
                return;
            }
            for (int val = 0; val < this.function.dimDomains.get(curId); val++){
                this.currentBasicOp++;
                this.currentCC++;
                curAssign[curIdx] = val;
                long util = partialUtil + this.function.eval(curAssign);
                util += this.incomeMsg.get(curId)[val];
                lb = Long.max(lb, util);
            }
        }
        else {
            if (curIdx == skipIdx){
                fdspRec(curAssign, curRIdx + 1, skipIdx, partialUtil);
                return;
            }
            int target = this.function.dimOrdering[skipIdx];
            for (int val = 0; val < this.function.dimDomains.get(curId); val++){
                curAssign[curIdx] = val;
                long util = partialUtil + this.incomeMsg.get(curId)[val];
                long ub = util + msgEst.get(curId);
                this.currentBasicOp++;
                if (this.informedEst.get(curId).containsKey(target)){
                    ub += this.informedEst.get(curId).get(target)[curAssign[skipIdx]].eval(curAssign);
                }
                else {
                    if (curIdx < skipIdx){
                        throw new RuntimeException();
                    }
                    ub += this.uninformedEst.get(curId).eval(curAssign);
                }
                if (ub > lb){
                    fdspRec(curAssign, curRIdx + 1, skipIdx, util);
                }
            }
        }
    }

    private static int binarySearch(long[] arr, long obj, int lo, int hi){
        if (lo == hi){
            return arr[lo] >= obj ? lo : -1;
        }
        if (lo > hi){
            throw new RuntimeException();
        }
        int mid = (lo + hi) / 2;
        if (arr[mid] > obj){
            return Integer.max(binarySearch(arr, obj, mid + 1, hi), mid);
        }
        else if (arr[mid] == obj){
            return mid;
        }
        else {
            int h = Integer.max(mid - 1, lo);
            return binarySearch(arr, obj, lo, h);
        }
    }

    private static boolean next(int[] curAssign, int[] domainLength, List<Integer> varyIndexes){
        boolean carry = true;
        for (int i = varyIndexes.size() - 1; i >= 0; i--){
            int idx = varyIndexes.get(i);
            curAssign[idx]++;
            if (curAssign[idx] == domainLength[idx]){
                curAssign[idx] = 0;
            }
            else {
                carry = false;
                break;
            }
        }
        return !carry;
    }

    private AbstractUtilityDigest createUtilityDigest(Set<Integer> fixedIndexes, int[] curAssign){
        if (this.weightedCriterion.equalsIgnoreCase("MAX")){
            return new MaxUtilityDigest(this.function, fixedIndexes, curAssign);
        }
        else if (this.weightedCriterion.equalsIgnoreCase("MEAN")){
            return new MeanUtilityDigest(this.function, fixedIndexes, curAssign);
        }
        else if (this.weightedCriterion.equalsIgnoreCase("QUANTILE")){
            return new QuantileUtilityDigest(this.function, fixedIndexes, curAssign);
        }
        else if (this.weightedCriterion.equalsIgnoreCase("HINDEX")){
            return new HIndexUtilityDigest(this.function, fixedIndexes, curAssign);
        }
        return null;
    }

}
