/*
 * Copyright 2021 steadybit GmbH. All rights reserved.
 */

package com.steadybit.testcontainers.measure;

import org.testcontainers.containers.GenericContainer;

public class Iperf3ServerContainer extends GenericContainer<Iperf3ServerContainer> {
    private int port = 5201;

    public Iperf3ServerContainer() {
        super("cilium/netperf:latest");
    }

    public Iperf3ServerContainer withIperf3Port(int iperf3Port) {
        this.port = iperf3Port;
        return this;
    }

    public int getIperf3Port() {
        return port;
    }

    public String getIperf3Address() {
        return this.getCurrentContainerInfo().getNetworkSettings().getIpAddress();
    }

    @Override
    protected void configure() {
        super.configure();
        this.withCommand("-s", "-p", String.valueOf(this.port));
        this.withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint("iperf3");
        });
    }
}
