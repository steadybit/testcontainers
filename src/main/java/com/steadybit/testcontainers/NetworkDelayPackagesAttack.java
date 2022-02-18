package com.steadybit.testcontainers;

import com.steadybit.testcontainers.trafficcontrol.TrafficControl;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.QDisc.NETEM;
import static com.steadybit.testcontainers.trafficcontrol.TrafficControl.QDisc.PRIO;
import org.testcontainers.containers.Container;

import java.time.Duration;

public class NetworkDelayPackagesAttack extends AbstractTrafficControlAttack {
    private final Duration delay;
    private final Duration jitter;

    private NetworkDelayPackagesAttack(Builder builder) {
        super(builder);
        if (builder.delay == null) {
            throw new IllegalArgumentException("delay must not be null");
        }
        this.delay = builder.delay;
        this.jitter = builder.jitter != null ? builder.jitter : Duration.ZERO;
    }

    @Override
    protected void addQdisc(Container<?> container, TrafficControl.RuleSet rules) {
        rules.qdiscRule(PRIO, "1:", null);
        rules.qdiscRule(NETEM, "30:", HANDLE_AFFECTED, "delay", delay.toMillis() + "ms", jitter.toMillis() + "ms");
    }

    public static class Builder extends AbstractTrafficControlAttack.Builder<NetworkDelayPackagesAttack> {
        private Duration delay = Duration.ZERO;
        private Duration jitter = Duration.ZERO;

        public Builder() {
        }

        public Builder delay(Duration delay) {
            this.delay = delay;
            return this;
        }

        public Builder jitter(Duration jitter) {
            this.jitter = jitter;
            return this;
        }

        @Override
        protected NetworkDelayPackagesAttack build() {
            return new NetworkDelayPackagesAttack(this);
        }
    }

}
