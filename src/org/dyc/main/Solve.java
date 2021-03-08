package org.dyc.main;

import org.apache.commons.cli.*;
import org.dyc.algo.hop.HOPMaxsumAgent;
import org.dyc.algo.hop.HOPMaxsumMeasurement;
import org.dyc.algo.maxsum.MaxsumAgent;
import org.dyc.algo.maxsum.MaxsumMeasurement;
import org.dyc.core.AgentManager;
import org.dyc.core.Constraint;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class Solve {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("p", true, "problem file");
        options.addOption("c", true, "cycles");
        options.addOption("t", true, "step size");
        options.addOption("a", true, "acceleration algorithm");
        options.addOption("cr", true, "sorting criterion");
        options.addOption("s", true, "sorting depth");
        options.addOption("am", true, "agent manifest file");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }
        File file = new File(cmd.getOptionValue("p"));
        if (!file.isFile() || !file.getName().endsWith(".xml")){
            System.out.println("please specify a problem file");
            System.exit(1);
        }
        StringBuilder identifier = new StringBuilder();
        String algo = cmd.getOptionValue("a").toUpperCase();
        switch (algo){
            case "GDP":
                MaxsumAgent.ACCELERATION_ALGO = "GDP";
                MaxsumAgent.DYNAMIC = false;
                identifier.append("GDP");
                break;
            case "GD2P":
                MaxsumAgent.ACCELERATION_ALGO = "GDP";
                MaxsumAgent.DYNAMIC = true;
                identifier.append("GD2P");
                break;
            case "FDSP":
                MaxsumAgent.ACCELERATION_ALGO = "FDSP";
                identifier.append("FDSP");
                break;
            case "ART-GD2P":
            case "ST-GD2P":
                MaxsumAgent.ACCELERATION_ALGO = "ART-GD2P";
                identifier.append("ST-GD2P").append("-").append(cmd.getOptionValue("t"));
                break;
            case "PTS":
                MaxsumAgent.ACCELERATION_ALGO = "PTS";
                MaxsumAgent.SORTING_DEPTH = Integer.parseInt(cmd.getOptionValue("s"));
                MaxsumAgent.WEIGHTED_CRITERION = cmd.getOptionValue("cr");
                identifier.append("PTS").append("-").append(cmd.getOptionValue("t")).append('-').append(cmd.getOptionValue("s"))
                        .append("-").append(cmd.getOptionValue("cr"));
                break;
            case "HOP":
                identifier.append("HOP");
                break;
            default:
                throw new RuntimeException("unknown algorithm!");
        }
        double t = Double.parseDouble(cmd.getOptionValue("t", "1"));
        int c = Integer.parseInt(cmd.getOptionValue("c", "2000"));
        String am = cmd.getOptionValue("am");
        AgentManager manager = null;
        if (algo.equalsIgnoreCase("HOP")){
            manager = new AgentManager(cmd.getOptionValue("p"), am, "HOP");
        }
        else {
            manager = new AgentManager(cmd.getOptionValue("p"), am, "Maxsum");
        }
        if (algo.equalsIgnoreCase("ART-GD2P") || algo.equalsIgnoreCase("ST-GD2P") || algo.equalsIgnoreCase("PTS")) {
            MaxsumAgent.STEP_SIZE = (int) (t * Constraint.SCALE);
        }
        if (algo.equalsIgnoreCase("HOP")){
            HOPMaxsumAgent.CYCLE = c;
        }
        else {
            MaxsumAgent.CYCLE = c;
        }
        manager.getMailer().setPrintCycle(false);
        manager.run();
        System.out.println(identifier);
        if (algo.equalsIgnoreCase("HOP")){
            HOPMaxsumMeasurement measurement = (HOPMaxsumMeasurement) manager.getMeasurement();
            System.out.println("elapse:\t" + measurement.getElapse());
        }
        else {
            MaxsumMeasurement measurement = (MaxsumMeasurement) manager.getMeasurement();
            System.out.println("pruned rate:\t" + measurement.getPrunedRate());
            System.out.println("pruned concurrent rate:\t" + measurement.getConcurrentPrunedRate());
            System.out.println("NCCCs speedup:\t" + measurement.getNCCCSpeedup());
            System.out.println("preprocessing elapse:\t" + measurement.getPreprocessingElapse());
            System.out.println("total elapse:\t" + measurement.getTotalElapse());
        }
    }
}
