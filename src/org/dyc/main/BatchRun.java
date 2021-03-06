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

public class BatchRun {
    public static void main(String[] args) throws Exception{
        Options options = new Options();
        options.addOption("p", true, "problem directory");
        options.addOption("c", true, "cycles");
        options.addOption("t", true, "step size");
        options.addOption("a", true, "acceleration algorithm");
        options.addOption("cr", true, "sorting criterion");
        options.addOption("s", true, "sorting depth");
        options.addOption("am", true, "agent manifest file");
        options.addOption("v", true, "validation counts");
        options.addOption("sd", true, "two-level directory");

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
        if (file.isFile()){
            System.out.println("please specify a directory");
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
                MaxsumAgent.ACCELERATION_ALGO = "ART-GD2P";
                identifier.append("ART-GD2P").append("-").append(cmd.getOptionValue("t"));
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
        boolean twoLevel = Boolean.parseBoolean(cmd.getOptionValue("sd", "true"));
        double t = Double.parseDouble(cmd.getOptionValue("t", "1"));
        int c = Integer.parseInt(cmd.getOptionValue("c", "2000"));
        String am = cmd.getOptionValue("am");
        int valid = Integer.parseInt(cmd.getOptionValue("v", "1"));
        if (!twoLevel){
            solveDirectory(cmd.getOptionValue("p"), identifier.toString(), am, valid, algo, t, c);
        }
        else {
            for (File subDir : file.listFiles()){
                if (subDir.isDirectory()){
                    solveDirectory(subDir.getAbsolutePath(), identifier.toString(), am, valid, algo, t, c);
                }
            }
        }
    }

    private static void solveDirectory(String directory, String identifier, String amPath, int validCnt, String algo, double t, int c) throws Exception{
        String resDirPath = Paths.get(directory, identifier).toString();
        File resDirFile = new File(resDirPath);
        if (resDirFile.exists()){
            for (File f : resDirFile.listFiles()){
                f.delete();
            }
            resDirFile.delete();
        }
        resDirFile.mkdir();
        File file = new File(directory);
        for (String f : file.list()){
            if (f.endsWith(".xml")){
                for (int i = 0; i < validCnt; i++) {
                    String problemPath = Paths.get(directory, f).toString();
                    AgentManager manager = null;
                    if (algo.equalsIgnoreCase("HOP")){
                        manager = new AgentManager(problemPath, amPath, "HOP");
                    }
                    else {
                        manager = new AgentManager(problemPath, amPath, "Maxsum");
                    }
                    if (algo.equalsIgnoreCase("ART-GD2P") || algo.equalsIgnoreCase("PTS")) {
                        MaxsumAgent.STEP_SIZE = (int) (t * Constraint.SCALE);
                    }
                    if (algo.equalsIgnoreCase("HOP")){
                        HOPMaxsumAgent.CYCLE = c;
                    }
                    else {
                        MaxsumAgent.CYCLE = c;
                    }
                    manager.run();
                    if (algo.equalsIgnoreCase("HOP")){
                        HOPMaxsumMeasurement measurement = (HOPMaxsumMeasurement) manager.getMeasurement();
                        String resP = Paths.get(resDirPath, "elapse.txt").toString();
                        PrintWriter printWriter = new PrintWriter(new FileWriter(resP, true));
                        printWriter.println(measurement.getElapse());
                        printWriter.close();
                    }
                    else {
                        MaxsumMeasurement measurement = (MaxsumMeasurement) manager.getMeasurement();
                        String resP = Paths.get(resDirPath, "prunedRate.txt").toString();
                        PrintWriter printWriter = new PrintWriter(new FileWriter(resP, true));
                        printWriter.println(measurement.getPrunedRate());
                        printWriter.close();

                        resP = Paths.get(resDirPath, "concurrentPrunedRate.txt").toString();
                        printWriter = new PrintWriter(new FileWriter(resP, true));
                        printWriter.println(measurement.getConcurrentPrunedRate());
                        printWriter.close();

                        resP = Paths.get(resDirPath, "ncccSpeedup.txt").toString();
                        printWriter = new PrintWriter(new FileWriter(resP, true));
                        printWriter.println(measurement.getNCCCSpeedup());
                        printWriter.close();

                        resP = Paths.get(resDirPath, "elapse.txt").toString();
                        printWriter = new PrintWriter(new FileWriter(resP, true));
                        printWriter.println(measurement.getPreprocessingElapse() + "\t" + measurement.getTotalElapse());
                        printWriter.close();

                        resP = Paths.get(resDirPath, "memory.txt").toString();
                        printWriter = new PrintWriter(new FileWriter(resP, true));
                        printWriter.println(measurement.getMemoryUsage());
                        printWriter.close();

                        resP = Paths.get(resDirPath, "checksum.txt").toString();
                        printWriter = new PrintWriter(new FileWriter(resP, true));
                        printWriter.println(manager.getCosts().get(567));
                        printWriter.close();
                        System.gc();
                    }
                }
            }
        }
    }
}
