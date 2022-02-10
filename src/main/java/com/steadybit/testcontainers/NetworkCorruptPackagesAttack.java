package com.steadybit.testcontainers;

import com.steadybit.testcontainers.trafficcontrol.TrafficControl;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.QDisc.NETEM;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.QDisc.PRIO;
import org.testcontainers.containers.Container;

public class NetworkCorruptPackagesAttack extends AbstractTrafficControlAttack {
    private final int corruptionPercentage;

    private NetworkCorruptPackagesAttack(Builder builder) {
        super(builder);
        // todo validate corruptionPercentage should be between 0-100
        this.corruptionPercentage = builder.corruptionPercentage;
    }

    @Override
    protected void addQdisc(Container<?> container, TrafficControl.RuleSet rules) {
        rules.qdiscRule(PRIO, "1:", null);
        rules.qdiscRule(NETEM, "30:", HANDLE_AFFECTED, "corrupt", corruptionPercentage + "%");
    }

    public static class Builder extends AbstractTrafficControlAttack.Builder<NetworkCorruptPackagesAttack> {
        private int corruptionPercentage;

        public Builder() {
        }

        public Builder corruptionPercentage(int corruptionPercentage) {
            this.corruptionPercentage = corruptionPercentage;
            return this;
        }

        @Override
        protected NetworkCorruptPackagesAttack build() {
            return new NetworkCorruptPackagesAttack(this);
        }
    }

}
