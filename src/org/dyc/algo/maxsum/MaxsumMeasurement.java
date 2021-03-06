package org.dyc.algo.maxsum;

import org.dyc.core.Agent;
import org.dyc.core.Measurement;

public class MaxsumMeasurement  extends Measurement {
    private long totalBasicOp;
    private long concurrentBasicOp;
    private long vanillaBasicOp;
    private long vanillaConcurrentBasicOp;
    private long tmpC;
    private long tmpVC;
    private long preprocessingElapse;
    private long totalElapse;
    private long memoryUsage;
    private long vanillaNCCCs;
    private long currentNCCCs;
    private boolean preprocessingLogged = false;

    @Override
    public void onStart() {

    }

    @Override
    public void measure(Agent agent) {
        MaxsumAgent maxsumAgent = (MaxsumAgent) agent;
        if (!this.preprocessingLogged){
            this.preprocessingElapse = Long.max(agent.getSimulatedTime(), this.preprocessingElapse);
        }
        this.totalElapse = Long.max(this.totalElapse, agent.getSimulatedTime());
        long accC = 0;
        long accVC = 0;
        for (AbstractFunctionNode functionNode : maxsumAgent.functionNodes.values()){
            this.vanillaBasicOp += functionNode.vanillaBasicOp;
            this.totalBasicOp += functionNode.currentBasicOp;
            accC += functionNode.currentBasicOp;
            accVC += functionNode.vanillaBasicOp;
            functionNode.vanillaBasicOp = functionNode.currentBasicOp = 0;
        }
        this.vanillaNCCCs = Long.max(this.vanillaNCCCs, maxsumAgent.vanillaNCCCs);
        this.currentNCCCs = Long.max(this.currentNCCCs, maxsumAgent.currentNCCCs);
        this.tmpVC = Long.max(this.tmpVC, accVC);
        this.tmpC = Long.max(this.tmpC, accC);
    }

    public double getNCCCSpeedup(){
        return 100 * (1 - this.currentNCCCs * 1.0 / this.vanillaNCCCs);
    }

    public double getPrunedRate(){
        return 100 * (1 - this.totalBasicOp * 1.0 / this.vanillaBasicOp);
    }

    public double getConcurrentPrunedRate(){
        return 100 * (1 - this.concurrentBasicOp * 1.0 / this.vanillaConcurrentBasicOp);
    }

    @Override
    public void onFinished() {

    }

    public long getPreprocessingElapse(){
        return this.preprocessingElapse;
    }

    public long getTotalElapse(){
        return this.totalElapse;
    }

    public double getMemoryUsage() {
        return memoryUsage * 1.0 / (1024 * 1024);
    }

    @Override
    public void onCycleEnd() {
        if (!this.preprocessingLogged){
            System.gc();
            System.out.println("gc");
            this.memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            this.preprocessingLogged = true;
        }
        this.concurrentBasicOp += this.tmpC;
        this.vanillaConcurrentBasicOp += this.tmpVC;
        this.tmpC = this.tmpVC = 0;
    }
}
