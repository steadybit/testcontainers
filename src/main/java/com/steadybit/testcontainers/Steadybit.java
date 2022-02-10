package com.steadybit.testcontainers;

public class Steadybit {
    private static final Integer DEFAULT_DNS_PORT = 53;

    public static NetworkDelayPackagesAttack.Builder networkDelayPackages() {
        return new NetworkDelayPackagesAttack.Builder();
    }

    public static NetworkLoosePackagesAttack.Builder networkLoosePackages() {
        return new NetworkLoosePackagesAttack.Builder();
    }

    public static NetworkCorruptPackagesAttack.Builder networkCorruptPackages() {
        return new NetworkCorruptPackagesAttack.Builder();
    }

    public static NetworkLimitBandwidthAttack.Builder networkLimitBandwidth() {
        return new NetworkLimitBandwidthAttack.Builder();
    }

    public static NetworkBlackholeAttack.Builder networkBlackhole() {
        return new NetworkBlackholeAttack.Builder();
    }

    public static NetworkBlackholeAttack.Builder networkBlockDns() {
        return new NetworkBlackholeAttack.Builder().port(DEFAULT_DNS_PORT);
    }

}
