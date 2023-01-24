package com.steadybit.testcontainers;

import com.steadybit.testcontainers.dns.TestcontainersDnsResolver;
import com.steadybit.testcontainers.trafficcontrol.TestcontainersTrafficControl;
import com.steadybit.testcontainers.trafficcontrol.TrafficControl;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.Filter.U32;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.Protocol.IP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class AbstractTrafficControlAttack implements ContainerAttack {
    private static final Logger log = LoggerFactory.getLogger(AbstractTrafficControlAttack.class);
    protected final static String HANDLE_AFFECTED = "1:3";
    private final List<Container<?>> containers;
    private final String networkInterface;
    private final Set<String> destAddresses;
    private final Integer destPort;
    private final Map<String, TrafficControl.RuleSet> rules = new HashMap<>();

    private final Function<String, TrafficControl> factory;

    protected AbstractTrafficControlAttack(Builder<? extends AbstractTrafficControlAttack> builder) {
        this.containers = builder.containers;
        if (builder.networkInterface == null || builder.networkInterface.isEmpty()) {
            throw new IllegalArgumentException("networkInterface must not be null or empry");
        }
        this.networkInterface = builder.networkInterface;
        this.destAddresses = builder.destAddresses;
        this.destPort = builder.destPort;
        this.factory = builder.factory;
    }

    @Override
    public synchronized void start() {
        for (Container<?> container : this.containers) {
            TrafficControl.RuleSet rulesToAdd = this.getRules(container);
            this.rules.put(container.getContainerId(), rulesToAdd);
            factory.apply(container.getContainerId()).add(rulesToAdd);
            log.info("Started {} on {}", this.getClass().getSimpleName(), container.getContainerName());
        }
    }

    @Override
    public synchronized void stop() {
        for (Container<?> container : this.containers) {
            TrafficControl.RuleSet rulesToDelete = this.rules.get(container.getContainerId());
            if (rulesToDelete != null) {
                factory.apply(container.getContainerId()).delete(rulesToDelete);
                log.info("Stopped {} on {}", this.getClass().getSimpleName(), container.getContainerName());
            }
        }
    }

    private TrafficControl.RuleSet getRules(Container<?> container) {
        TrafficControl.RuleSet rules = new TrafficControl.RuleSet(this.networkInterface);
        this.addQdisc(container, rules);
        this.addFilter(container, rules);
        return rules;
    }

    private void addFilter(Container<?> container, TrafficControl.RuleSet rules) {
        int prio = 1;
        List<String> ips = this.resolveAddresses(container, this.destAddresses);
        if (ips.isEmpty()) {
            if (this.destPort == null) {
                //Matches all traffic
                rules.filterRule("1:", IP, prio++, U32, "match", "u32", "0", "0", "flowid", HANDLE_AFFECTED);
            } else {
                //Matches all traffic on port
                rules.filterRule("1:", IP, prio++, U32, "match", "ip", "dport", Integer.toString(this.destPort), "0xffff", "flowid", HANDLE_AFFECTED);
                rules.filterRule("1:", IP, prio++, U32, "match", "ip", "sport", Integer.toString(this.destPort), "0xffff", "flowid", HANDLE_AFFECTED);
            }
        } else {
            for (String ip : ips) {
                if (this.destPort == null) {
                    rules.filterRule("1:", IP, prio++, U32, "match", "ip", "dst", ip, "flowid", HANDLE_AFFECTED);
                    rules.filterRule("1:", IP, prio++, U32, "match", "ip", "src", ip, "flowid", HANDLE_AFFECTED);
                } else {
                    rules.filterRule("1:", IP, prio++, U32, "match", "ip", "dst", ip, "match", "ip", "dport", Integer.toString(this.destPort), "0xffff",
                            "flowid", HANDLE_AFFECTED);
                    rules.filterRule("1:", IP, prio++, U32, "match", "ip", "src", ip, "match", "ip", "dport", Integer.toString(this.destPort), "0xffff",
                            "flowid", HANDLE_AFFECTED);
                }
            }
        }
    }

    protected abstract void addQdisc(Container<?> container, TrafficControl.RuleSet rules);

    private List<String> resolveAddresses(Container<?> container, Set<String> addresses) {
        return new TestcontainersDnsResolver(container).resolve(addresses);
    }

    public static abstract class Builder<T extends AbstractTrafficControlAttack> extends ContainerAttack.Builder<T> {
        public Function<String, TrafficControl> factory = TestcontainersTrafficControl::forContainer;
        private String networkInterface = "eth0";
        private Set<String> destAddresses = Collections.emptySet();
        private Integer destPort = null;

        protected Builder() {
        }

        public Builder<T> networkInterface(String networkInterface) {
            this.networkInterface = networkInterface;
            return this;
        }

        public Builder<T> destAddress(String... destAddresses) {
            this.destAddresses = new HashSet<>(Arrays.asList(destAddresses));
            return this;
        }

        public Builder<T> destPort(Integer destPort) {
            this.destPort = destPort;
            return this;
        }

        public Builder<T> factory(Function<String, TrafficControl> factory) {
            this.factory = factory;
            return this;
        }
    }
}
