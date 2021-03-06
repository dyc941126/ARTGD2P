package org.dyc.algo.maxsum.acceleration.utility_digesters;

import org.dyc.core.Constraint;

import java.util.Set;
import java.util.TreeMap;

public class QuantileUtilityDigest  extends AbstractUtilityDigest{
    private TreeMap<Long, Integer> histogram;

    public QuantileUtilityDigest(Constraint data, Set<Integer> fixedIndexes, int[] curAssign) {
        super(data, fixedIndexes, curAssign);
        this.histogram = new TreeMap<>();
    }

    @Override
    protected void process(int[] curAssign, long util, int idx) {
        this.maxUtil = Long.max(this.maxUtil, util);
        int cnt = this.histogram.getOrDefault(util, 0);
        this.histogram.put(util, cnt + 1);
    }

    @Override
    protected void end() {
        int target = (int) (this.combinationNum * 0.25);
        int cnt = 0;
        for (long util : this.histogram.descendingKeySet()){
            cnt += this.histogram.get(util);
            if (cnt >= target){
                this.weight = util;
                break;
            }
        }
    }
}
