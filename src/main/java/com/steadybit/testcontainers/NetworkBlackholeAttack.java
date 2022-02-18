package com.steadybit.testcontainers;

import com.steadybit.testcontainers.dns.TestcontainersDnsResolver;
import com.steadybit.testcontainers.iprule.TestcontainersIpRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NetworkBlackholeAttack implements ContainerAttack {
    private static final Logger log = LoggerFactory.getLogger(NetworkBlackholeAttack.class);
    private final List<Container<?>> containers;
    private final Set<String> addresses;
    private final Integer port;
    private final Map<String, List<String[]>> rules = new HashMap<>();

    protected NetworkBlackholeAttack(Builder builder) {
        this.containers = builder.containers;
        this.addresses = builder.addresses;
        this.port = builder.port;
    }

    @Override
    public synchronized void start() {
        for (Container<?> container : this.containers) {
            List<String[]> rulesToAdd = this.getRules(container);
            this.rules.put(container.getContainerId(), rulesToAdd);
            new TestcontainersIpRule(container.getContainerId()).add(rulesToAdd);
            log.info("Started {} on {}", this.getClass().getSimpleName(), container.getContainerName());
        }
    }

    @Override
    public synchronized void stop() {
        for (Container<?> container : this.containers) {
            List<String[]> rulesToDelete = this.rules.get(container.getContainerId());
            if (rulesToDelete != null) {
                new TestcontainersIpRule(container.getContainerId()).delete(rulesToDelete);
                log.info("Started {} on {}", this.getClass().getSimpleName(), container.getContainerName());
            }
        }
    }

    private List<String[]> getRules(Container<?> container) {
        List<String[]> rules = new ArrayList<>();
        List<String> ips = this.resolveAddresses(container, this.addresses);

        if (this.port == null && this.addresses.isEmpty()) {
            rules.add(new String[] { "blackhole" });
        } else if (this.port == null) {
            for (String ip : ips) {
                rules.add(new String[] { "blackhole", "to", ip });
                rules.add(new String[] { "blackhole", "from", ip });
            }
        } else if (this.addresses.isEmpty()) {
            rules.add(new String[] { "blackhole", "dport", Integer.toString(port) });
            rules.add(new String[] { "blackhole", "sport", Integer.toString(port) });
        } else {
            for (String address : addresses) {
                rules.add(new String[] { "blackhole", "to", address, "dport", Integer.toString(port) });
                rules.add(new String[] { "blackhole", "from", address, "sport", Integer.toString(port) });
            }
        }
        return rules;
    }

    private List<String> resolveAddresses(Container<?> container, Set<String> addresses) {
        return new TestcontainersDnsResolver(container).resolve(addresses);
    }

    public static class Builder extends ContainerAttack.Builder<NetworkBlackholeAttack> {
        private Set<String> addresses = Collections.emptySet();
        private Integer port = null;

        public Builder() {
        }

        public Builder address(String... addresses) {
            this.addresses = new HashSet<>(Arrays.asList(addresses));
            return this;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        @Override
        protected NetworkBlackholeAttack build() {
            return new NetworkBlackholeAttack(this);
        }
    }

}
