package org.dyc.algo.maxsum.acceleration.utility_digesters;

import org.dyc.core.Constraint;

import java.util.Set;
import java.util.TreeMap;

public class HIndexUtilityDigest extends AbstractUtilityDigest{
    private TreeMap<Long, Integer> histogram;

    public HIndexUtilityDigest(Constraint data, Set<Integer> fixedIndexes, int[] curAssign) {
        super(data, fixedIndexes, curAssign);
        this.histogram = new TreeMap<>();
    }

    @Override
    protected void process(int[] curAssign, long util, int idx) {
        int cnt = this.histogram.getOrDefault(util, 0);
        this.maxUtil = Long.max(this.maxUtil, util);
        this.histogram.put(util, cnt + 1);
    }

    @Override
    protected void end() {
        int cnt = 0;
        for (long util : this.histogram.navigableKeySet()){
            cnt +=  this.histogram.get(util);
            double cntPercentage = 1 - cnt * 1.0 / this.combinationNum;
            double utilPercentage = (util - this.data.minValue) * 1.0 / (this.data.maxValue - this.data.minValue);
            if (utilPercentage <= cntPercentage){
                this.weight = util;
            }
            else
                break;
        }
    }
}
