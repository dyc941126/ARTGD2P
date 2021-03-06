package org.dyc.algo.maxsum;

import org.dyc.algo.maxsum.acceleration.*;
import org.dyc.core.Constraint;

import java.util.*;

public abstract class AbstractFunctionNode {
    protected Constraint function;
    protected Map<Integer, long[]> incomeMsg = new HashMap<>();
    protected long vanillaBasicOp = 0;
    protected long currentBasicOp = 0;
    private long vbo;
    private long vcc;
    protected long vanillaCC;
    protected long currentCC;

    public AbstractFunctionNode(Constraint function){
        this.function = function;
        this.vbo = 1;
        for (int var : this.function.dimOrdering){
            int len = this.function.dimDomains.get(var);
            long[] msg = new long[len];
            Arrays.fill(msg, 0);
            this.vbo *= len;
            this.incomeMsg.put(var, msg);
        }
        this.vcc = this.vbo;
        this.vbo *= (this.function.dimDomains.size() - 1);
    }

    public long[] max(int id){
        this.vanillaBasicOp += this.vbo;
        this.vanillaCC += this.vcc;
        return null;
    }

    public void init() {

    }

    public void updateMsg(int id, long[] msg){
        this.incomeMsg.put(id, msg);
    }

    public static AbstractFunctionNode createFunctionNodes(Constraint function, String algo, String weightedCriterion, int stepSize, boolean dynamic, int sortingDepth){
        if (algo.equalsIgnoreCase("GDP")){
            return new GDP(function, dynamic);
        }
        else if (algo.equalsIgnoreCase("FDSP")){
            return new FDSP(function);
        }
        else if (algo.equalsIgnoreCase("ART-GD2P")){
            return new ART_GD2P(function, stepSize);
        }
        else if (algo.equalsIgnoreCase("PTS")){
            return new PTS(function, sortingDepth, stepSize, weightedCriterion);
        }
        return new Vanilla(function);
    }
}
