/*
 * Copyright 2022 steadybit GmbH. All rights reserved.
 */

package com.steadybit.testcontainers.measure;

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.function.Consumer;

public class Iperf3ClientContainer extends GenericContainer<Iperf3ClientContainer> {
    private static final Logger log = LoggerFactory.getLogger(Iperf3ClientContainer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Iperf3ServerContainer server;
    private int dataPort = 5000;

    public Iperf3ClientContainer(Iperf3ServerContainer server) {
        super("mlabbe/iperf3:3.9-r1-hc");
        this.server = server;
    }

    public Iperf3ClientContainer withDataPort(int port) {
        this.dataPort = port;
        return this;
    }

    public int getDataPort() {
        return this.dataPort;
    }

    public int measureLoss() {
        try {
            String[] command = { "iperf3", "-c", this.server.getIperf3Address(), "-p",
                    Integer.toString(this.server.getIperf3Port()), "-u", "-t1", "--bind",
                    "0.0.0.0",
                    "--reverse", "--cport", Integer.toString(this.dataPort), "--json" };
            ExecResult result = this.execInContainer(command);
            if (result.getExitCode() == 0) {
                JsonNode root = this.objectMapper.readTree(result.getStdout().replace("\n", ""));
                return (int) Math.round(root.at("/end/sum/lost_percent").asDouble());
            }
            throw new RuntimeException("Execution [" + String.join(" ", command) + "] failed: RC=" + result.getExitCode() + " " + result.getStdout());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Execution failed:", e);
        }
    }

    public long measureBandwidth(String maxBitrate) {
        try {
            String[] command = { "iperf3", "-c", this.server.getIperf3Address(), "-p",
                    Integer.toString(this.server.getIperf3Port()), "-t 1", "--bind",
                    "0.0.0.0", "--udp", "--bitrate", maxBitrate, "--reverse", "--cport", Integer.toString(this.dataPort), "--json" };
            ExecResult result = this.execInContainer(command);
            if (result.getExitCode() == 0) {
                JsonNode root = this.objectMapper.readTree(result.getStdout().replace("\n", ""));
                return Math.round(root.at("/end/sum/bits_per_second").asDouble() / 1_000_000);
            }
            throw new RuntimeException("Execution [" + String.join(" ", command) + "] failed: RC=" + result.getExitCode() + " " + result.getStderr());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Execution failed:", e);
        }
    }

    @Override
    protected void configure() {
        super.configure();
        this.setWaitStrategy(null);
        this.withCreateContainerCmdModifier(new Consumer<CreateContainerCmd>() {
            @Override
            public void accept(CreateContainerCmd cmd) {
                cmd.withEntrypoint("/bin/sh");
                cmd.withTty(true);
            }
        });
    }

    public void stopRunningMeasures() {
        try {
            this.execInContainer("pkill", "iperf3");
        } catch (InterruptedException | IOException e) {
            log.warn("Error killing iperf3", e);
        }
    }

}
