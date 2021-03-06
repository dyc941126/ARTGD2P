package org.dyc.main;

import org.dyc.algo.hop.HOPMaxsumMeasurement;
import org.dyc.algo.maxsum.MaxsumAgent;
import org.dyc.algo.maxsum.MaxsumMeasurement;
import org.dyc.core.AgentManager;
import org.dyc.core.Constraint;

public class Test {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        AgentManager manager = new AgentManager("D:\\test\\1.xml", "agent_manifest.xml", "Maxsum");
        MaxsumAgent.STEP_SIZE = 1;
        MaxsumAgent.ACCELERATION_ALGO = "FDSP";
        MaxsumAgent.CYCLE = 2000;
        manager.getMailer().setPrintCycle(true);
        manager.run();
//        System.out.println(((HOPMaxsumMeasurement) manager.getMeasurement()).elapse);
        MaxsumMeasurement measurement = (MaxsumMeasurement) manager.getMeasurement();
        System.out.println("Pruned rate: " + measurement.getPrunedRate());
        System.out.println("Concurrent pruned rate: " + measurement.getConcurrentPrunedRate());
        System.out.println("Preprocessing elapse: " + measurement.getPreprocessingElapse());
        System.out.println("Total elapse: " + measurement.getTotalElapse());
        System.out.println("Memory usage: " + measurement.getMemoryUsage());
        System.out.println(System.currentTimeMillis() - start);
    }
}
