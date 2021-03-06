package org.dyc.algo.maxsum;

import org.dyc.core.Agent;
import org.dyc.core.Constraint;
import org.dyc.core.Mailer;
import org.dyc.utilities.AlgoUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaxsumAgent extends Agent {
    public static int CYCLE = 100;
    public static String ACCELERATION_ALGO = "";
    public static int STEP_SIZE = 1;
    public static String WEIGHTED_CRITERION = "";
    public static boolean DYNAMIC = false;
    public static int SORTING_DEPTH = 1;


    private static final int MSG_Q = 0;
    private static final int MSG_R = 1;
    private static final int MSG_NCCCS = 2;

    private int remainingCycle;

    protected Map<String, AbstractFunctionNode> functionNodes;
    private Map<String, Integer> functionNodeLocation;
    private Map<String, long[]> incomingRMessages;
    private int totalArity;
    private int rcvR;
    private int rcvQ;

    protected long vanillaNCCCs;
    protected long currentNCCCs;

    public MaxsumAgent(int id, int domain, int[] neighbors, List<Constraint> constraints, Mailer mailer) {
        super(id, domain, neighbors, constraints, mailer);
    }

    @Override
    protected void onStart() {
        this.remainingCycle = CYCLE;
        this.functionNodeLocation = new HashMap<>();
        this.functionNodes = new HashMap<>();
        this.incomingRMessages = new HashMap<>();
        for (Constraint constraint : this.constraints){
            this.functionNodeLocation.put(constraint.getId(), constraint.getHostId());
            long[] msg = new long[this.domain];
            Arrays.fill(msg, 0);
            this.incomingRMessages.put(constraint.getId(), msg);
            if (this.id == constraint.getHostId()){
                AbstractFunctionNode node = AbstractFunctionNode.createFunctionNodes(constraint, ACCELERATION_ALGO, WEIGHTED_CRITERION, STEP_SIZE, DYNAMIC, SORTING_DEPTH);
                node.init();
                System.out.println(constraint.getId() + "init");
                totalArity += constraint.dimDomains.size();
                this.functionNodes.put(constraint.getId(), node);
            }
        }
        this.computeRMessages();
    }

    private void computeRMessages(){
        for (AbstractFunctionNode node : this.functionNodes.values()){
            for (int target : node.function.dimOrdering){
                long[] res = node.max(target);
                this.sendMessage(target, MSG_R, new QRMessage(res, node.function.getId()));
            }
            this.vanillaNCCCs += node.vanillaCC;
            node.vanillaCC = 0;
            this.currentNCCCs += node.currentCC;
            node.currentCC = 0;
        }
        for (int n : this.neighbors){
            this.sendMessage(n, MSG_NCCCS, new long[]{this.currentNCCCs, this.vanillaNCCCs});
        }
    }

    private class QRMessage {
        long[] content;
        String src;

        public QRMessage(long[] content, String src) {
            this.content = content;
            this.src = src;
        }
    }

    @Override
    protected void disposeMessage(int src, int typ, Object content) {
        switch (typ){
            case MSG_Q:
                QRMessage qMessage = (QRMessage) content;
                this.functionNodes.get(qMessage.src).updateMsg(src, qMessage.content);
                this.rcvQ ++;
                if (this.rcvQ == this.totalArity){
                    this.rcvQ = 0;
                    this.computeRMessages();
                }
                break;
            case MSG_R:
                QRMessage rMessage = (QRMessage) content;
                this.incomingRMessages.put(rMessage.src, rMessage.content);
                this.rcvR ++;
                if (this.rcvR == this.constraints.size()) {
                    this.rcvR = 0;
                    this.remainingCycle--;
                    if (this.remainingCycle == 0){
                        this.terminate();
                        return;
                    }
//                    System.out.println(this.id + " ready");
                    computeQMessages();
                    long[] belief = new long[this.domain];
                    Arrays.fill(belief, 0);
                    for (long[] msg : incomingRMessages.values()){
                        for (int i = 0; i < msg.length; i++){
                            belief[i] += msg[i];
                        }
                    }
                    this.val = AlgoUtils.argMax(belief);
                }
                break;
            case MSG_NCCCS:
                long[] ncccs = (long[]) content;
                this.currentNCCCs = Long.max(this.currentNCCCs, ncccs[0]);
                this.vanillaNCCCs = Long.max(this.vanillaNCCCs, ncccs[1]);
        }
    }

    private void computeQMessages(){
        for (String target : incomingRMessages.keySet()){
            long[] msg = new long[this.domain];
            for (String con : incomingRMessages.keySet()){
                if (target.equals(con)){
                    continue;
                }
                long[] incomeMsg = this.incomingRMessages.get(con);
                for (int i = 0; i < msg.length; i++){
                    msg[i] += incomeMsg[i];
                }
            }
            this.sendMessage(this.functionNodeLocation.get(target), MSG_Q, new QRMessage(msg, target));
        }
    }

    @Override
    protected void onTimestepAdvanced() {
        if (this.constraints.size() == 0){
            this.terminate();
        }
    }
}
