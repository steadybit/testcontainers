package com.steadybit.testcontainers.iprule;

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
    protected String execute(String... ipRuleCommands) {
        TestcontainersIpRule.IpRuleContainer container = new TestcontainersIpRule.IpRuleContainer()
                .withCommand(ipRuleCommands)
                .withNetworkMode("container:" + containerId);

        try {
            container.start();
            return container.getLogs(OutputFrame.OutputType.STDOUT);
        } finally {
            container.stop();
        }
    }

    private static class IpRuleContainer extends GenericContainer<TestcontainersIpRule.IpRuleContainer> {
        public IpRuleContainer() {
            super("gaiadocker/iproute2:latest");
            this.withStartupCheckStrategy(new OneShotStartupCheckStrategy());
            this.withCreateContainerCmdModifier(cmd -> {
                cmd.getHostConfig().withCapAdd(Capability.NET_ADMIN, Capability.NET_RAW);
                cmd.withEntrypoint("ip", "rule");
            });
        }
    }
}
