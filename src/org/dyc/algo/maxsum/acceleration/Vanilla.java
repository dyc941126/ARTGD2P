package org.dyc.algo.maxsum.acceleration;

import org.dyc.algo.maxsum.AbstractFunctionNode;
import org.dyc.core.Constraint;
import org.dyc.utilities.MultiDimDataUtils;

import java.util.Arrays;

public class Vanilla extends AbstractFunctionNode {
    public Vanilla(Constraint function) {
        super(function);
    }

    @Override
    public long[] max(int id) {
        super.max(id);
        long[] res = new long[this.function.dimDomains.get(id)];
        int[] domainLength = new int[this.function.dimDomains.size()];
        int[] curAssign = new int[domainLength.length];
        int skip = 0;
        for (int i = 0; i < this.function.dimDomains.size(); i++){
            int curId = this.function.dimOrdering[i];
            domainLength[i] = this.function.dimDomains.get(curId);
            curAssign[i] = 0;
            if (curId == id){
                skip = i;
            }
        }

        long norm = 0;
        for (int val = 0; val < domainLength[skip]; val++){
            Arrays.fill(curAssign, 0);
            curAssign[skip] = val;
            long maxValue = Long.MIN_VALUE;
            while (true){
                long value = this.function.eval(curAssign);
                for (int i = 0; i < curAssign.length; i++){
                    if (i == skip)
                        continue;
                    this.currentBasicOp++;
                    int curId = this.function.dimOrdering[i];
                    value += this.incomeMsg.get(curId)[curAssign[i]];
                }
                maxValue = Long.max(maxValue, value);
                if (!MultiDimDataUtils.next(curAssign, domainLength, skip)){
                    break;
                }
            }
            norm += maxValue;
            res[val] = maxValue;
        }
        norm = norm / res.length;
        for (int i = 0; i < res.length; i++){
            res[i] -= norm;
        }
        return res;
    }
}
