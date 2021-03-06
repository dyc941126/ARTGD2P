package org.dyc.algo.hop;

import org.dyc.core.Agent;
import org.dyc.core.Constraint;
import org.dyc.core.Mailer;
import org.dyc.utilities.AlgoUtils;

import java.util.*;

// HOP algorithm (for channel allocation problem only)
public class HOPMaxsumAgent extends Agent {

    public static int CYCLE = 200;

    private static final int MSG_Q = 0;
    private static final int MSG_R = 1;

    private static final int PHASE_VARIABLE = 0;
    private static final int PHASE_FACTOR = 1;

    private int phase;

    private int inferRange = 3;
    private long[][] hop; // in a channel allocation problem, each agent holds a utility function, which is going to be converted to a cardinality factor hop


    private Map<Integer, long[]> variableIncomingMsg;
    private Map<Integer, long[]> factorIncomingMsg;
    private Constraint factor;

    private int cycle;

    public HOPMaxsumAgent(int id, int domain, int[] neighbors, List<Constraint> constraints, Mailer mailer) {
        super(id, domain, neighbors, constraints, mailer);
        this.variableIncomingMsg = new HashMap<>();
        this.factorIncomingMsg = new HashMap<>();
    }

    @Override
    protected void onStart() {
        this.cycle = CYCLE;
        factor = null;
        //find the utility function associated with the agent
        for (Constraint constraint : this.constraints){
            if (constraint.getHostId() == this.id){
                factor = constraint;
                for (int i : constraint.dimOrdering){
                    this.factorIncomingMsg.put(i, new long[constraint.dimDomains.get(i)]);
                }
                break;
            }
        }

        //build cardinality factor. The factor is related to how many neighboring APs are interfering with the current agent
        this.hop = new long[this.domain][factor.dimOrdering.length];
        long[] hop = new long[this.hop[0].length];

        int[] assign = new int[factor.dimOrdering.length];
        // we build the factor by iteratively reducing the number of interfering APs
        Arrays.fill(assign, 0);
        for (int i = 1; i <= assign.length; i++){
            hop[hop.length - i] = factor.eval(assign); // check out utility when there are hop.length - i + 1 APs interfer with current AP
            if (i == assign.length)
                break;
            assign[i] = this.inferRange + 1; // let i-th AP not interfer with current AP
        }

        for (int i = 0; i < this.hop.length; i++){
            for (int j = 0; j < hop.length; j++){
                long rndPref = (long) (Math.random() * 0.001 * hop[j]);
                this.hop[i][j] = hop[j] + rndPref;
            }
        }
        this.phase = PHASE_FACTOR;
    }


    private void computeFactorMsgs(){
        for (int target : this.factor.dimOrdering){
            long[] msg = new long[this.factor.dimDomains.get(target)];
            long total = 0;
            for (int val = 0; val < this.factor.dimDomains.get(target); val++){
                if (target == this.id){
                    msg[val] = max(val);
                }
                else {
                    msg[val] = max(target, val);
                }
                total += msg[val];
            }
            //normalize msg
            long alpha = total / msg.length;
            for (int i = 0; i < msg.length; i++){
                msg[i] -= alpha;
            }
            this.sendMessage(target, MSG_R, msg);
        }
    }


    // maximization for the current AP
    private long max(int targetVal){
        long[][] utils = new long[this.factor.dimOrdering.length - 1][2]; // msg utility table for all other APs.
                                                                         // 1st component is the maximum msg utility achieved if the AP is interfering with the current AP
                                                                        // 2nd component is the maximum msg utility achieved if the AP is not interfering with the current AP
        long[] diff = new long[this.factor.dimOrdering.length - 1];
        long accInterfering = 0;
        long accNoInterfering = 0;
        for (int i = 1; i < this.factor.dimOrdering.length; i++) { // the first element of dimOrdering is the current AP, so we skip it
            int curId = this.factor.dimOrdering[i];
            long maxInterferingUtil = Long.MIN_VALUE;
            long maxNoInterferingUtil = Long.MIN_VALUE;
            int domain = this.factor.dimDomains.get(curId);
            for (int j = 0; j < domain; j++){
                if (j >= targetVal - this.inferRange && j <= targetVal + this.inferRange){ // interfering
                    maxInterferingUtil = Long.max(maxInterferingUtil, this.factorIncomingMsg.get(curId)[j]);
                }
                else { // not interfering
                    maxNoInterferingUtil = Long.max(maxNoInterferingUtil, this.factorIncomingMsg.get(curId)[j]);
                }
            }
            utils[i - 1][0] = maxInterferingUtil;
            utils[i - 1][1] = maxNoInterferingUtil;
            accNoInterfering += maxNoInterferingUtil;
            diff[i - 1] = maxInterferingUtil - maxNoInterferingUtil;
        }
        int[] sortedIdx = argsort(diff, false);
        long maxUtil = Long.MIN_VALUE;
        for (int cnt = 0 ; cnt <= sortedIdx.length; cnt++){ // linear scan to enumerate all possible outcomes (i.e., all possible numbers of interfering APs)
            long localUtil = this.hop[targetVal][cnt];
            long msgUtil = accInterfering + accNoInterfering;
            maxUtil = Long.max(maxUtil, localUtil + msgUtil);
            if (cnt != sortedIdx.length){  // greedily select the next AP with the maximum diff value to be interfering
                accInterfering += utils[sortedIdx[cnt]][0]; // enforce integrity constraint: one AP can either interfer (+) or not interfer (-)
                accNoInterfering -= utils[sortedIdx[cnt]][1];
            }
        }
        return maxUtil;
    }

    // maximization for an AP other than current AP
    private long max(int targetId, int targetVal){
        long res = Long.MIN_VALUE;
        for (int k = 0; k < this.domain; k++){ // partition according to the assignment of current AP
            long[][] utils = new long[this.factor.dimOrdering.length - 2][2]; // utility table for non-target APs, note that current AP is also excluded, so we minus 2
            long[] diff = new long[this.factor.dimOrdering.length - 2];
            int idx = 0;
            long accInterfering = 0;
            long accNoInterfering = 0;
            for (int i = 1; i < this.factor.dimOrdering.length; i++){
                int curId = this.factor.dimOrdering[i];
                if (curId == targetId){ // skip the target AP
                    continue;
                }
                long maxInterferingUtil = Long.MIN_VALUE;
                long maxNoInterferingUtil = Long.MIN_VALUE;
                int domain = this.factor.dimDomains.get(curId);
                // the domain of the current running AP (curId) is divided into two halves: the one is interfering with current AP, and the one is not interfering with current AP
                for (int j = 0; j < domain; j++){
                    if (j >= k - this.inferRange && j <= k + this.inferRange){ // interfering
                        maxInterferingUtil = Long.max(maxInterferingUtil, this.factorIncomingMsg.get(curId)[j]);
                    }
                    else { // not interfering
                        maxNoInterferingUtil = Long.max(maxNoInterferingUtil, this.factorIncomingMsg.get(curId)[j]);
                    }
                }
                utils[idx][0] = maxInterferingUtil;
                utils[idx][1] = maxNoInterferingUtil;
                accNoInterfering += maxNoInterferingUtil;
                diff[idx++] = maxInterferingUtil - maxNoInterferingUtil;
            }
            int[] sortedIdx = argsort(diff, false);
            int interferingCnt = 0;
            if (targetVal >= k - this.inferRange && targetVal <= k + this.inferRange){
                interferingCnt++;  // if the target AP is interfering with current AP in partition k, then record it
            }
            long maxUtil = Long.MIN_VALUE;
            for (int cnt = 0 ; cnt <= sortedIdx.length; cnt++){ // linear scan to enumerate all possible outcomes (i.e., all possible numbers of interfering APs)
                int curInterferingCnt = interferingCnt + cnt;
                long localUtil = this.hop[k][curInterferingCnt];
                long msgUtil = accInterfering + accNoInterfering;
                maxUtil = Long.max(maxUtil, localUtil + msgUtil);
                if (cnt != sortedIdx.length){
                    accInterfering += utils[sortedIdx[cnt]][0];
                    accNoInterfering -= utils[sortedIdx[cnt]][1];
                }
            }
            res = Long.max(res, maxUtil + this.factorIncomingMsg.get(this.id)[k]);
        }
        return res;
    }

    @Override
    protected void disposeMessage(int src, int type, Object o) {
        switch (type){
            case MSG_R:
                this.variableIncomingMsg.put(src, (long[]) o);
                break;
            case MSG_Q:
                this.factorIncomingMsg.put(src, (long[]) o);
        }

    }

    @Override
    protected void onTimestepAdvanced() {

        switch (this.phase){
            case PHASE_FACTOR:
                if (this.cycle-- == 0){
                    this.terminate();
                    return;
                }
                this.computeFactorMsgs();
                this.phase = PHASE_VARIABLE;
                break;
            case PHASE_VARIABLE:
                this.computeVariableMsgs();
                this.phase = PHASE_FACTOR;
                long[] util = new long[this.domain];
                for (int i : this.variableIncomingMsg.keySet()){
                    for (int j = 0; j < util.length; j++){
                        util[j] += this.variableIncomingMsg.get(i)[j];
                    }
                }
                this.val = AlgoUtils.argMax(util);
        }
    }

    private void computeVariableMsgs(){
        for (int target : this.variableIncomingMsg.keySet()){
            long[] msg = new long[this.domain];
            for (int i : this.variableIncomingMsg.keySet()){
                if (i == target){
                    continue;
                }
                long[] util = this.variableIncomingMsg.get(i);
                for (int j = 0; j < util.length; j++){
                    msg[j] += util[j];
                }
            }
            this.sendMessage(target, MSG_Q, msg);
        }
    }

    public static int[] argsort(final long[] a, final boolean ascending) {
        Integer[] indexes = new Integer[a.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }
        Arrays.sort(indexes, new Comparator<Integer>() {
            @Override
            public int compare(final Integer i1, final Integer i2) {
                return (ascending ? 1 : -1) * Long.compare(a[i1], a[i2]);
            }
        });
        int[] idx = new int[indexes.length];
        for (int i = 0; i < idx.length; i++){
            idx[i] = indexes[i];
        }
        return idx;
    }
}
