package com.steadybit.testcontainers.dns;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.net.InetAddressUtils;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.utility.DockerStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TestcontainersDnsResolver {
    private final String targetContainerId;
    private final DockerClient dockerClient = DockerClientFactory.lazyClient();

    public TestcontainersDnsResolver(Container<?> targetContainer) {
        targetContainerId = targetContainer.getContainerId();
    }

    public List<String> resolve(Collection<String> addresses) {
        if (addresses.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> unresolved = new ArrayList<>();
        List<String> resolved = new ArrayList<>();

        for (String address : addresses) {
            if (InetAddressUtils.isIPv4Address(address) | InetAddressUtils.isIPv6Address(address)) {
                resolved.add(address);
            } else {
                unresolved.add(address);
            }
        }

        resolveUsingExtraHosts(unresolved, resolved);
        resolveUsingContainer(unresolved, resolved);

        if (!unresolved.isEmpty()) {
            throw new RuntimeException("Could not resolve hosts: " + unresolved);
        }

        return resolved;
    }

    private void resolveUsingExtraHosts(List<String> unresolved, List<String> resolved) {
        Map<String, List<String>> extraHosts = Arrays.stream(this.dockerClient.inspectContainerCmd(targetContainerId).exec()
                        .getHostConfig()
                        .getExtraHosts())
                .map(s -> s.split(":", 2))
                .collect(groupingBy(s -> s[0], mapping(s -> s[1], toList())));

        for (String address : new ArrayList<>(unresolved)) {
            List<String> ips = extraHosts.get(address);
            if (ips != null) {
                unresolved.remove(address);
                resolved.addAll(ips);
            }
        }
    }

    private void resolveUsingContainer(List<String> unresolved, List<String> resolved) {
        GetentHostsContainer container = new GetentHostsContainer()
                .withCommand(unresolved.toArray(new String[0]))
                .withNetworkMode("container:" + targetContainerId);
        try {
            container.start();
            try (Scanner scanner = new Scanner(container.getLogs(OutputFrame.OutputType.STDOUT))) {

                while (scanner.hasNext()) {
                    String ip = scanner.next();
                    String hostname = scanner.next();
                    scanner.nextLine();
                    resolved.add(ip);
                    unresolved.remove(hostname);
                }
            }

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