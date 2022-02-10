/*
 * Copyright 2021 steadybit GmbH. All rights reserved.
 */

package com.steadybit.testcontainers.measure;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.commons.net.echo.EchoTCPClient;
import static org.assertj.core.api.Assertions.assertThat;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.IOException;

public class EchoTcpContainer extends GenericContainer<EchoTcpContainer> {
    private final EchoTCPClient echoClient = new EchoTCPClient();
    private int port = 2000;

    public EchoTcpContainer() {
        super("alpine/socat:latest");
    }

    public EchoTcpContainer withEchoPort(int port) {
        this.port = port;
        return this;
    }

    public EchoTCPClient getEchoClient() {
        return this.echoClient;
    }

    public int getEchoPortInContainer() {
        try {
            ExecResult result = this.execInContainer("sh", "-c",
                    "netstat -tn | grep ESTABLISHED | grep \":" + this.port + "\" | cut -c\"45-65\" | cut -d\":\" -f2");
            return Integer.parseInt(result.getStdout().trim());
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException("Couldn't get echo port in container");
        }
    }

    public String getEchoAddressInContainer() {
        return this.getCurrentContainerInfo().getNetworkSettings().getGateway();
    }

    public long measureRoundtrip() {
        EchoTCPClient echo = this.getEchoClient();
        byte[] message = "Hello World".getBytes();
        byte[] received = new byte[message.length];

        try {
            long start = System.currentTimeMillis();
            IOUtils.write(message, echo.getOutputStream());
            IOUtils.read(echo.getInputStream(), received, 0, received.length);
            long duration = System.currentTimeMillis() - start;
            assertThat(received).isEqualTo(message);
            return duration;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        try {
            this.echoClient.connect(this.getContainerIpAddress(), this.getMappedPort(this.port));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void containerIsStopping(InspectContainerResponse containerInfo) {
        try {
            this.echoClient.disconnect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void configure() {
        super.configure();
        this.withPrivilegedMode(true);
        this.withExposedPorts(this.port);
        this.withCommand("-v", "-d", "-d", "tcp-l:" + this.port + ",fork", "exec:'/bin/cat'");
    }
}
