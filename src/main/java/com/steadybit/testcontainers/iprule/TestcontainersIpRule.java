package com.steadybit.testcontainers.iprule;

import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Capability;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

public class TestcontainersIpRule extends IpRule {
    private final String containerId;
    private final String image;

    private TestcontainersIpRule(String image, String containerId) {
        this.image = image;
        this.containerId = containerId;
    }

    public static TestcontainersIpRule forContainer(String containerId) {
        return TestcontainersIpRule.usingImage("praqma/network-multitool:latest").forContainer(containerId);
    }

    public static TestcontainersIpRule.ImageSpec usingImage(String image) {
        return new TestcontainersIpRule.ImageSpec(image);
    }

    public static class ImageSpec {
        private final String image;

        private ImageSpec(String image) {
            this.image = image;
        }

        public TestcontainersIpRule forContainer(String containerId) {
            return new TestcontainersIpRule(this.image, containerId);
        }
    }

    @Override
    protected Result executeBatch(String... ipRuleCommands) {
        try (IpRuleContainer container = new IpRuleContainer(this.image)
                .withCommand(ipRuleCommands)
                .withNetworkMode("container:" + containerId)) {
            container.start();
            return container.getResult();
        }
    }

    private static class IpRuleContainer extends GenericContainer<TestcontainersIpRule.IpRuleContainer> {
        public IpRuleContainer(String image) {
            super(image);
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
