package com.steadybit.testcontainers.iprule;

import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Capability;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

public class TestcontainersIpRule extends IpRule {
    private final String containerId;

    public TestcontainersIpRule(String containerId) {
        this.containerId = containerId;
    }

    @Override
    protected Result executeBatch(String... ipRuleCommands) {
        TestcontainersIpRule.IpRuleContainer container = new TestcontainersIpRule.IpRuleContainer()
                .withCommand(ipRuleCommands)
                .withNetworkMode("container:" + containerId);

        try {
            container.start();
            return container.getResult();
        } finally {
            container.stop();
        }
    }

    private static class IpRuleContainer extends GenericContainer<TestcontainersIpRule.IpRuleContainer> {
        public IpRuleContainer() {
            super("cilium/netperf:latest");
        }

        @Override
        protected void configure() {
            this.withStartupCheckStrategy(new OneShotStartupCheckStrategy());
            this.withCreateContainerCmdModifier(cmd -> {
                StringBuilder shCommand = new StringBuilder("(");
                for (String tcCommand : cmd.getCmd()) {
                    shCommand.append("echo '").append(tcCommand).append("';");
                }
                shCommand.append(") | ip -batch -");

                cmd.getHostConfig().withCapAdd(Capability.NET_ADMIN, Capability.NET_RAW);
                cmd.withEntrypoint("sh", "-c");
                cmd.withCmd(shCommand.toString());
            });
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
