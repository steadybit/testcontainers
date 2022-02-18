package com.steadybit.testcontainers;

import java.time.Duration;

public class Steadybit {
    private static final Integer DEFAULT_DNS_PORT = 53;

    /**
     * Delays egress tcp/udp network packages for containers (on eth0 by default)
     */
    public static NetworkDelayPackagesAttack.Builder networkDelayPackages(Duration delay) {
        return new NetworkDelayPackagesAttack.Builder().delay(delay);
    }

    /**
     * Looses egress tcp/udp network packages for containers (on eth0 by default)
     */
    public static NetworkLoosePackagesAttack.Builder networkLoosePackages(int lossPercentage) {
        return new NetworkLoosePackagesAttack.Builder().lossPercentage(lossPercentage);
    }

    /**
     * Corrupts egress tcp/udp network packages for containers (on eth0 by default)
     */
    public static NetworkCorruptPackagesAttack.Builder networkCorruptPackages(int corruptionPercentage) {
        return new NetworkCorruptPackagesAttack.Builder().corruptionPercentage(corruptionPercentage);
    }

    /**
     * Limits tcp/udp network bandwidth for containers (on eth0 by default)
     */
    public static NetworkLimitBandwidthAttack.Builder networkLimitBandwidth(Bandwidth bandwidth) {
        return new NetworkLimitBandwidthAttack.Builder().bandwidth(bandwidth);
    }

    /**
     * Blocks all network traffic for containers
     */
    public static NetworkBlackholeAttack.Builder networkBlackhole() {
        return new NetworkBlackholeAttack.Builder();
    }

    /**
     * Blocks all network traffic for containers on dns port (53)
     */
    public static NetworkBlackholeAttack.Builder networkBlockDns() {
        return new NetworkBlackholeAttack.Builder().port(DEFAULT_DNS_PORT);
    }

}
