package com.steadybit.testcontainers;

import com.steadybit.testcontainers.trafficcontrol.TrafficControl;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.QDisc.HTB;
import org.testcontainers.containers.Container;

public class NetworkLimitBandwidthAttack extends AbstractTrafficControlAttack {
    private final Bandwidth bandwidth;

    private NetworkLimitBandwidthAttack(Builder builder) {
        super(builder);
        if (builder.bandwidth == null) {
            throw new IllegalArgumentException("bandwidth must not be null");
        }
        this.bandwidth = builder.bandwidth;
    }

    @Override
    protected void addQdisc(Container<?> container, TrafficControl.RuleSet rules) {
        rules.qdiscRule(HTB, "1:", null, "default", "30");
        rules.classRule(HTB, HANDLE_AFFECTED, "1:", "rate", this.bandwidth.toString());
    }

    public static class Builder extends AbstractTrafficControlAttack.Builder<NetworkLimitBandwidthAttack> {
        private Bandwidth bandwidth;

        public Builder() {
        }

        public Builder bandwidth(Bandwidth bandwidth) {
            this.bandwidth = bandwidth;
            return this;
        }

        @Override
        protected NetworkLimitBandwidthAttack build() {
            return new NetworkLimitBandwidthAttack(this);
        }
    }

}
