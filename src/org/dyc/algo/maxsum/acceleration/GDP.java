package org.dyc.algo.maxsum.acceleration;

import org.dyc.algo.maxsum.AbstractFunctionNode;
import org.dyc.core.Constraint;
import org.dyc.utilities.AlgoUtils;
import org.dyc.utilities.MultiDimDataUtils;

import java.util.*;

public class GDP extends AbstractFunctionNode {
    private int[][] allAssign;
    public Map<Integer, SortedUtilityList> sortedUtils;
    private boolean dynamic;

    public GDP(Constraint function, boolean dynamic) {
        super(function);
        this.allAssign = new int[function.data.length][];
        this.dynamic = dynamic;
        this.sortedUtils = new HashMap<>();
    }

    @Override
    public void init() {
        for (int id : this.function.dimOrdering) {
            SortedUtilityList sortedUtilityList = new SortedUtilityList();
            int[] domainLength = new int[this.function.dimDomains.size()];
            int skipIdx = 0;
            for (int i = 0; i < this.function.dimOrdering.length; i++) {
                int curId = this.function.dimOrdering[i];
                domainLength[i] = this.function.dimDomains.get(curId);
                if (curId == id) {
                    skipIdx = i;
                }
            }

            for (int val = 0; val < domainLength[skipIdx]; val++) {
                int[] curAssign = new int[this.function.dimDomains.size()];
                Arrays.fill(curAssign, 0);
                curAssign[skipIdx] = val;
                while (true) {
                    int idx = this.function.getIndex(curAssign);
                    long util = this.function.data[idx];
                    if (this.allAssign[idx] == null) {
                        int[] copy = new int[curAssign.length];
                        System.arraycopy(curAssign, 0, copy, 0, curAssign.length);
                        this.allAssign[idx] = copy;
                    }
                    sortedUtilityList.put(val, util, this.allAssign[idx]);
                    if (!MultiDimDataUtils.next(curAssign, domainLength, skipIdx)) {
                        break;
                    }
                }
            }
            this.sortedUtils.put(id, sortedUtilityList);
        }
    }

    @Override
    public long[] max(int id) {
        super.max(id);
        long[] results = new long[this.function.dimDomains.get(id)];
        SortedUtilityList sortedUtilityList = this.sortedUtils.get(id);
        long msgEst = 0;
        for (int var : this.incomeMsg.keySet()){
            if (var != id){
                msgEst += AlgoUtils.max(this.incomeMsg.get(var));
            }
        }
        long norm = 0;
        for (int val = 0; val < results.length; val++){
            long lb = Long.MIN_VALUE;
            long maxUtil = Long.MIN_VALUE;
            TreeMap<Long, List<int[]>> data = sortedUtilityList.storage.get(val);
            for (long localUtil : data.descendingKeySet()){
                if (localUtil < lb){
                    break;
                }
                for (int[] assign : data.get(localUtil)){
                    long util = localUtil;
                    this.currentCC++;
                    for (int i = 0; i < this.function.dimDomains.size(); i++){
                        int curId = this.function.dimOrdering[i];
                        if (curId == id)
                            continue;
                        this.currentBasicOp++;
                        util += this.incomeMsg.get(curId)[assign[i]];
                    }
                    maxUtil = Long.max(maxUtil, util);
                    if (lb == Long.MIN_VALUE || this.dynamic){
                        lb = maxUtil - msgEst;
                    }
                }
            }
            results[val] = maxUtil;
            norm += maxUtil;
        }
        norm /= results.length;
        for (int i = 0; i < results.length; i++){
            results[i] -= norm;
        }
        return results;
    }

    private static class SortedUtilityList{
        Map<Integer, TreeMap<Long, List<int[]>>> storage = new HashMap<>();
        public void put(int val, long util, int[] assign){
            if (!this.storage.containsKey(val)){
                this.storage.put(val, new TreeMap<>());
            }
            TreeMap<Long, List<int[]>> data = this.storage.get(val);
            if (!data.containsKey(util)){
                data.put(util, new ArrayList<>(1));
            }
            List<int[]> assigns = data.get(util);
            assigns.add(assign);
        }

    }
}
