package com.steadybit.testcontainers;

import com.steadybit.testcontainers.trafficcontrol.TrafficControl;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.QDisc.HTB;
import org.testcontainers.containers.Container;

public class NetworkLimitBandwidthAttack extends AbstractTrafficControlAttack {
    private final String bandwidth;

    private NetworkLimitBandwidthAttack(Builder builder) {
        super(builder);
        // todo validate bandwidth
        this.bandwidth = builder.bandwidth;
    }

    @Override
    protected void addQdisc(Container<?> container, TrafficControl.RuleSet rules) {
        rules.qdiscRule(HTB, "1:", null, "default", "30");
        rules.classRule(HTB, HANDLE_AFFECTED, "1:", "rate", this.bandwidth);
    }

    public static class Builder extends AbstractTrafficControlAttack.Builder<NetworkLimitBandwidthAttack> {
        private String bandwidth;

        public Builder() {
        }

        public Builder bandwidth(String bandwidth) {
            this.bandwidth = bandwidth;
            return this;
        }

        @Override
        protected NetworkLimitBandwidthAttack build() {
            return new NetworkLimitBandwidthAttack(this);
        }
    }

}
