package com.steadybit.testcontainers;

import com.steadybit.testcontainers.trafficcontrol.TrafficControl;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.QDisc.NETEM;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.QDisc.PRIO;
import org.testcontainers.containers.Container;

public class NetworkLoosePackagesAttack extends AbstractTrafficControlAttack {
    private final int lossPercentage;

    private NetworkLoosePackagesAttack(Builder builder) {
        super(builder);
        if (builder.lossPercentage < 0 || builder.lossPercentage > 100) {
            throw new IllegalArgumentException("lossPercentage must be between 0-100");
        }
        this.lossPercentage = builder.lossPercentage;
    }

    @Override
    protected void addQdisc(Container<?> container, TrafficControl.RuleSet rules) {
        rules.qdiscRule(PRIO, "1:", null);
        rules.qdiscRule(NETEM, "30:", HANDLE_AFFECTED, "loss", "random", lossPercentage + "%");
    }

    public static class Builder extends AbstractTrafficControlAttack.Builder<NetworkLoosePackagesAttack> {
        private int lossPercentage;

        public Builder() {
        }

        public Builder lossPercentage(int lossPercentage) {
            this.lossPercentage = lossPercentage;
            return this;
        }

        @Override
        protected NetworkLoosePackagesAttack build() {
            return new NetworkLoosePackagesAttack(this);
        }
    }

}
