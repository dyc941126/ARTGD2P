package org.dyc.algo.hop;

import org.dyc.core.AlgoConfParser;
import org.jdom2.Element;

public class HOPMaxsumConfParser extends AlgoConfParser {
    public HOPMaxsumConfParser(Element root) {
        super(root);
    }

    @Override
    public void parse() {
        HOPMaxsumAgent.CYCLE = Integer.parseInt(root.getAttributeValue("cycle"));
    }
}
