package org.dyc.algo.hop;

import org.dyc.core.Agent;
import org.dyc.core.Measurement;

public class HOPMaxsumMeasurement extends Measurement {
    private long elapse;


    @Override
    public void onStart() {
    }

    @Override
    public void measure(Agent agent) {
        this.elapse = Long.max(this.elapse, agent.getSimulatedTime());
    }

    public long getElapse() {
        return elapse;
    }

    @Override
    public void onFinished() {

    }

    @Override
    public void onCycleEnd() {

    }
}
