package com.steadybit.testcontainers.dns;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.utility.DockerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class TestcontainerDnsResolver implements DnsResolver {
    private final String targetContainerId;

    public TestcontainerDnsResolver(Container<?> targetContainer) {
        targetContainerId = targetContainer.getContainerId();
    }

    @Override
    public List<String> resolve(List<String> addresses) {
        GetentHostsContainer container = new GetentHostsContainer()
                .withCommand(addresses.toArray(new String[0]))
                .withNetworkMode("container:" + targetContainerId);
        try {
            container.start();

            List<String> result = new ArrayList<>();
            try (Scanner scanner = new Scanner(container.getLogs(OutputFrame.OutputType.STDOUT))) {
                while (scanner.hasNext()) {
                    result.add(scanner.next());
                    scanner.nextLine();
                }
            }

            return result;
        } finally {
            container.stop();
        }
    }

    private static class GetentHostsContainer extends GenericContainer<GetentHostsContainer> {
        public GetentHostsContainer() {
            super("gaiadocker/iproute2:latest");

            this.withStartupCheckStrategy(new StartupCheckStrategy() {
                @Override
                public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
                    InspectContainerResponse.ContainerState state = getCurrentState(dockerClient, containerId);

                    if (!DockerStatus.isContainerStopped(state)) {
                        return StartupStatus.NOT_YET_KNOWN;
                    }
                    return StartupStatus.SUCCESSFUL;
                }
            });
            this.withCreateContainerCmdModifier(cmd -> {
                cmd.withEntrypoint("getent", "hosts");
            });
        }
    }
}