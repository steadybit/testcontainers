package com.steadybit.testcontainers.trafficcontrol;

import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Capability;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

public class TestcontainersTrafficControl extends TrafficControl {
    private final String containerId;
    private final String image;

    private TestcontainersTrafficControl(String image, String containerId) {
        this.containerId = containerId;
        this.image = image;
    }

    public static TestcontainersTrafficControl forContainer(String containerId) {
        return TestcontainersTrafficControl.usingImage("praqma/network-multitool:latest").forContainer(containerId);
    }

    public static TestcontainersTrafficControl.ImageSpec usingImage(String image) {
        return new TestcontainersTrafficControl.ImageSpec(image);
    }

    public static class ImageSpec {
        private final String image;

        private ImageSpec(String image) {
            this.image = image;
        }

        public TestcontainersTrafficControl forContainer(String containerId) {
            return new TestcontainersTrafficControl(this.image, containerId);
        }
    }

    @Override
    protected Result executeBatch(String... tcCommands) {
        TcContainer container = new TcContainer(this.image)
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
        public TcContainer(String dockerImageName) {
            super(dockerImageName);
        }

        @Override
        protected void configure() {
            this.withStartupCheckStrategy(new OneShotStartupCheckStrategy());
            this.withCreateContainerCmdModifier(cmd -> {
                StringBuilder shCommand = new StringBuilder("(");
                for (String tcCommand : cmd.getCmd()) {
                    shCommand.append("echo '").append(tcCommand).append("';");
                }
                shCommand.append(") | tc -force -batch -");

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
