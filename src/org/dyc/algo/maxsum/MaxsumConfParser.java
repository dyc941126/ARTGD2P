package org.dyc.algo.maxsum;

import org.dyc.core.AlgoConfParser;
import org.dyc.core.Constraint;
import org.jdom2.Element;

public class MaxsumConfParser extends AlgoConfParser {
    public MaxsumConfParser(Element root) {
        super(root);
    }

    @Override
    public void parse() {
        MaxsumAgent.CYCLE = Integer.parseInt(root.getAttributeValue("cycle"));
        MaxsumAgent.STEP_SIZE = (int) (Double.parseDouble(root.getAttributeValue("stepSize", "1")) * Constraint.SCALE);
        MaxsumAgent.ACCELERATION_ALGO = root.getAttributeValue("algo", "");
        MaxsumAgent.DYNAMIC = Boolean.parseBoolean(root.getAttributeValue("dynamic", "false"));
        MaxsumAgent.WEIGHTED_CRITERION = root.getAttributeValue("weightedCriterion", "");
        MaxsumAgent.SORTING_DEPTH = Integer.parseInt(root.getAttributeValue("sortingDepth", "1"));
    }
}
