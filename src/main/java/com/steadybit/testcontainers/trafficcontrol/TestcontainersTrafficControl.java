package com.steadybit.testcontainers.trafficcontrol;

import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Capability;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

public class TestcontainersTrafficControl extends TrafficControl {
    private final String containerId;

    public TestcontainersTrafficControl(String containerId) {
        this.containerId = containerId;
    }

    @Override
    protected Result executeBatch(String... tcCommands) {
        TcContainer container = new TcContainer()
                .withCommand(tcCommands)
                .withNetworkMode("container:" + containerId);

        try {
            container.start();
            return container.getResult();
        } finally {
            container.stop();
        }
    }

    private static class TcContainer extends GenericContainer<TcContainer> {
        private String[] tcCommands;

        public TcContainer() {
            super("gaiadocker/iproute2:latest");

            this.withStartupCheckStrategy(new OneShotStartupCheckStrategy());
            this.withCreateContainerCmdModifier(cmd -> {
                StringBuilder shCommand = new StringBuilder("(");
                for (String tcCommand : tcCommands) {
                    shCommand.append("echo '").append(tcCommand).append("';");
                }
                shCommand.append(") | tc -force -batch -");

                cmd.getHostConfig().withCapAdd(Capability.NET_ADMIN, Capability.NET_RAW);
                cmd.withEntrypoint("sh");
                cmd.withCmd("-c", shCommand.toString());
            });
        }

        @Override
        public TcContainer withCommand(String... tcCommands) {
            this.tcCommands = tcCommands;
            return this;
        }

        public Result getResult() {
            Integer statusCode = this.dockerClient.waitContainerCmd(this.getContainerId()).exec(new WaitContainerResultCallback()).awaitStatusCode();
            return new Result(statusCode != null ? statusCode : -1,
                    this.getLogs(OutputFrame.OutputType.STDOUT),
                    this.getLogs(OutputFrame.OutputType.STDERR)
            );
        }
    }
}
