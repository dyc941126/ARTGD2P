package org.dyc.algo.maxsum.acceleration;

import org.dyc.algo.maxsum.AbstractFunctionNode;
import org.dyc.core.Constraint;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FDSP extends AbstractFunctionNode {
    private Map<Integer, Constraint> uninformedEst;
    private Map<Integer, Map<Integer, Constraint[]>> informedEst;
    private long maxUtil;
    private Map<Integer, Long> msgEst = new HashMap<>();

    public FDSP(Constraint function) {
        super(function);
        this.uninformedEst = new HashMap<>();
        this.informedEst = new HashMap<>();
    }

    @Override
    public void init() {
        super.init();
        Constraint data = this.function;
        for (int i = this.function.dimDomains.size() - 2; i >= 0; i--){
            int curId = this.function.dimOrdering[i];
            int succId = this.function.dimOrdering[i + 1];
            data = data.max(succId);
            this.uninformedEst.put(curId, data);
            this.informedEst.put(curId, new HashMap<>());
        }

        for (int i = this.function.dimDomains.size() - 2; i >= 0; i--){
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
    }

    @Override
    public long[] max(int id) {
        super.max(id);
        msgEst.clear();
        int[] curAssign = new int[this.function.dimDomains.size()];
        int[] domainLength = new int[this.function.dimDomains.size()];
        int skipIdx = 0;
        for (int i = 0; i < this.function.dimDomains.size(); i++){
            int curId = this.function.dimOrdering[i];
            curAssign[i] = 0;
            domainLength[i] = this.function.dimDomains.get(curId);
            if (curId == id){
                skipIdx = i;
            }
        }
        long acc = 0;
        for (int i = this.function.dimDomains.size() - 1; i > 0; i--){
            int prevId = this.function.dimOrdering[i - 1];
            int curId = this.function.dimOrdering[i];
            if (curId != id){
                long maxUtil = Long.MIN_VALUE;
                for (long val : this.incomeMsg.get(curId)){
                    maxUtil = Long.max(maxUtil, val);
                }
                acc += maxUtil;
            }
            if (prevId != id)
                this.msgEst.put(prevId, acc);
        }
        long[] result = new long[this.function.dimDomains.get(id)];
        long norm = 0;
        for (int val = 0; val < result.length; val++){
            this.maxUtil = Long.MIN_VALUE;
            curAssign = new int[this.function.dimDomains.size()];
            Arrays.fill(curAssign, 0);
            curAssign[skipIdx] = val;
            rec(curAssign, domainLength, 0, skipIdx, 0);
            result[val] = this.maxUtil;
            norm += this.maxUtil;
        }
        norm = norm / result.length;
        for (int i = 0; i < result.length; i++){
            result[i] -= norm;
        }
        return result;
    }

    private void rec(int[] curAssign, int[] domainLength, int curIdx, int skipIdx, long partialUtil){
        boolean last = curIdx == domainLength.length - 1;
        int curId = this.function.dimOrdering[curIdx];
        if (last){
            if (curIdx == skipIdx){
                long util = partialUtil + this.function.eval(curAssign);
                this.currentCC++;
                maxUtil = Long.max(maxUtil, util);
                return;
            }
            for (int val = 0; val < this.function.dimDomains.get(curId); val++){
                this.currentBasicOp++;
                this.currentCC++;
                curAssign[curIdx] = val;
                long util = partialUtil + this.function.eval(curAssign);
                util += this.incomeMsg.get(curId)[val];
                maxUtil = Long.max(maxUtil, util);
            }
        }
        else {
            if (curIdx == skipIdx){
                rec(curAssign, domainLength, curIdx + 1, skipIdx, partialUtil);
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
                    ub += this.uninformedEst.get(curId).eval(curAssign);
                }
                if (ub > maxUtil){
                    rec(curAssign, domainLength, curIdx + 1, skipIdx, util);
                }
            }
        }
    }

}
