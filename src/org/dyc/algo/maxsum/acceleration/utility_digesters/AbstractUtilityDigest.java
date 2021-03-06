package org.dyc.algo.maxsum.acceleration.utility_digesters;

import org.dyc.core.Constraint;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractUtilityDigest {
    protected Constraint data;
    private Set<Integer> fixedIndexes;
    private int[] curAssign;
    protected int combinationNum = 1;
    public long maxUtil;
    public long weight;

    public AbstractUtilityDigest(Constraint data, Set<Integer> fixedIndexes, int[] curAssign) {
        this.data = data;
        this.fixedIndexes = fixedIndexes;
        this.curAssign = curAssign;
    }

    public void digest(){
        int idx = 0;
        for (int i = 0; i < curAssign.length; i++){
            int curId = this.data.dimOrdering[i];
            if (this.fixedIndexes.contains(i)){
                idx += curAssign[i] * this.data.weight.get(curId);
            }
            else {
                this.combinationNum *= this.data.dimDomains.get(curId);
                this.curAssign[i] = 0;
            }
        }
        while (idx != -1){
            process(curAssign, this.data.data[idx], idx);
            idx = next(curAssign, idx);
        }
        end();
    }

    protected abstract void end();

    protected abstract void process(int[] curAssign, long util, int idx);

    private int next(int[] curAssign, int idx){
        boolean carry = true;
        for (int i = curAssign.length - 1; i >= 0; i--){
            int curId = this.data.dimOrdering[i];
            if (this.fixedIndexes.contains(i)){
                continue;
            }
            int weight = this.data.weight.get(curId);
            idx -= curAssign[i] * weight;
            curAssign[i] ++;
            if (curAssign[i] == this.data.dimDomains.get(curId)){
                curAssign[i] = 0;
            }
            else {
                idx += curAssign[i] * weight;
                carry = false;
                break;
            }
        }
        return carry ? -1 : idx;
    }
}
