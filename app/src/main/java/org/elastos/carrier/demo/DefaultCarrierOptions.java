package org.elastos.carrier.demo;

import java.util.ArrayList;

import org.elastos.carrier.Carrier;

public class DefaultCarrierOptions extends Carrier.Options {
    public DefaultCarrierOptions(String path) {
        super();

        setOptions(path);
    }

    private void setOptions(String path) {
        setUdpEnabled(true);
        setPersistentLocation(path);

        ArrayList<BootstrapNode> arrayList = new ArrayList<>();
        BootstrapNode node = new BootstrapNode();
        node.setIpv4("13.58.208.50");
        node.setPort("33445");
        node.setPublicKey("89vny8MrKdDKs7Uta9RdVmspPjnRMdwMmaiEW27pZ7gh");
        arrayList.add(node);

        node = new BootstrapNode();
        node.setIpv4("18.216.102.47");
        node.setPort("33445");
        node.setPublicKey("G5z8MqiNDFTadFUPfMdYsYtkUDbX5mNCMVHMZtsCnFeb");
        arrayList.add(node);

        node = new BootstrapNode();
        node.setIpv4("18.216.6.197");
        node.setPort("33445");
        node.setPublicKey("H8sqhRrQuJZ6iLtP2wanxt4LzdNrN2NNFnpPdq1uJ9n2");
        arrayList.add(node);

        node = new BootstrapNode();
        node.setIpv4("52.83.171.135");
        node.setPort("33445");
        node.setPublicKey("5tuHgK1Q4CYf4K5PutsEPK5E3Z7cbtEBdx7LwmdzqXHL");
        arrayList.add(node);

        node = new BootstrapNode();
        node.setIpv4("52.83.191.228");
        node.setPort("33445");
        node.setPublicKey("3khtxZo89SBScAMaHhTvD68pPHiKxgZT6hTCSZZVgNEm");
        arrayList.add(node);

        setBootstrapNodes(arrayList);
    }
}

