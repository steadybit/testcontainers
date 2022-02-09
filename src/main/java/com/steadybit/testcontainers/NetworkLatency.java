package com.steadybit.testcontainers;

import com.steadybit.testcontainers.trafficcontrol.TestcontainersTrafficControl;
import com.steadybit.testcontainers.trafficcontrol.TrafficControl;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.Filter.U32;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.Protocol.IP;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.QDisc.NETEM;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.QDisc.PRIO;
import org.testcontainers.containers.Container;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkLatency extends ContainerAttack<NetworkLatency> {
    protected final static String HANDLE_UNAFFECTED = "1:1"; //TODO MOVE
    protected final static String HANDLE_AFFECTED = "1:3"; //TODO MOVE

    private Duration delay = Duration.ZERO;
    private Duration jitter = Duration.ZERO;
    private String networkInterface = "eth0";
    private Set<String> dstAddress = Collections.emptySet();
    private Integer dstPort = null;
    private TrafficControl.RuleSet rules;

    NetworkLatency() {
    }

    public NetworkLatency delay(Duration delay) {
        this.delay = delay;
        return this;
    }

    public NetworkLatency jitter(Duration jitter) {
        this.jitter = jitter;
        return this;
    }

    public NetworkLatency networkInterface(String networkInterface) {
        this.networkInterface = networkInterface;
        return this;
    }

    public NetworkLatency dstAddress(String... dstAddresses) {
        this.dstAddress = new HashSet<>(Arrays.asList(dstAddresses));
        return this;
    }

    public NetworkLatency dstPort(Integer dstPort) {
        this.dstPort = dstPort;
        return this;
    }

    private TrafficControl.RuleSet getRules() {
        TrafficControl.RuleSet rules = new TrafficControl.RuleSet(this.networkInterface);

        rules.qdiscRule(PRIO, "1:", null);
        rules.qdiscRule(NETEM, "30:", HANDLE_AFFECTED, "delay", delay.toMillis() + "ms", jitter.toMillis() + "ms");

        int prio = 1;
        List<String> ips = this.resolveAddresses(this.dstAddress);
        if (ips.isEmpty()) {
            if (this.dstPort == null) {
                //Matches all traffic
                rules.filterRule("1:", IP, prio++, U32, "match", "u32", "0", "0", "flowid", HANDLE_AFFECTED);
            } else {
                //Matches all traffic on port
                rules.filterRule("1:", IP, prio++, U32, "match", "ip", "dport", Integer.toString(this.dstPort), "0xffff", "flowid", HANDLE_AFFECTED);
                rules.filterRule("1:", IP, prio++, U32, "match", "ip", "sport", Integer.toString(this.dstPort), "0xffff", "flowid", HANDLE_AFFECTED);
            }
        } else {
            for (String ip : ips) {
                if (this.dstPort == null) {
                    rules.filterRule("1:", IP, prio++, U32, "match", "ip", "dst", ip, "flowid", HANDLE_AFFECTED);
                    rules.filterRule("1:", IP, prio++, U32, "match", "ip", "src", ip, "flowid", HANDLE_AFFECTED);
                } else {
                    rules.filterRule("1:", IP, prio++, U32, "match", "ip", "dst", ip, "match", "ip", "dport", Integer.toString(this.dstPort), "0xffff",
                            "flowid", HANDLE_AFFECTED);
                    rules.filterRule("1:", IP, prio++, U32, "match", "ip", "src", ip, "match", "ip", "dport", Integer.toString(this.dstPort), "0xffff",
                            "flowid", HANDLE_AFFECTED);
                }
            }
        }

        return rules;
    }

    private List<String> resolveAddresses(Set<String> dstAddress) {
        //TODO resolve addresses
        return new ArrayList<>(dstAddress);
    }

    @Override
    public void start() {
        this.rules = this.getRules();
        for (Container<?> container : this.containers) {
            new TestcontainersTrafficControl(container.getContainerId()).add(this.rules);
        }
    }

    @Override
    public void stop() {
        for (Container<?> container : this.containers) {
            new TestcontainersTrafficControl(container.getContainerId()).delete(this.rules);
        }
    }
}
