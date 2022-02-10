package com.steadybit.testcontainers;

import com.steadybit.testcontainers.measure.Iperf3ClientContainer;
import com.steadybit.testcontainers.measure.Iperf3ServerContainer;
import static org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class NetworkLimitBandwidthAttackTest {

    @Container
    private static final Iperf3ServerContainer target = new Iperf3ServerContainer();
    @Container
    private static final Iperf3ClientContainer tester = new Iperf3ClientContainer(target);

    private long normalBandwidth;
    private long attackBandwidth;

    @BeforeEach
    void setUp() {
        tester.stopRunningMeasures();

        this.normalBandwidth = (tester.measureBandwidth() + tester.measureBandwidth() + tester.measureBandwidth()) / 3;
        this.attackBandwidth = (this.normalBandwidth / 2);
    }

    @Test
    void should_limit_all_traffic() {
        Steadybit.networkLimitBandwidth()
                .bandwidth(this.attackBandwidth + "mbit")
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.attackBandwidth, Percentage.withPercentage(10));
                });
        assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth, Percentage.withPercentage(10));
    }

    @Test
    void should_limit_all_traffic_using_port_filter() {

        // match
        Steadybit.networkLimitBandwidth()
                .bandwidth(this.attackBandwidth + "mbit")
                .destPort(target.getIperf3Port())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.attackBandwidth, Percentage.withPercentage(10));
                });

        // mismatch
        Steadybit.networkLimitBandwidth()
                .bandwidth(this.attackBandwidth + "mbit")
                .destPort(target.getIperf3Port() + 999)
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth, Percentage.withPercentage(10));
                });

        assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth, Percentage.withPercentage(10));
    }

    @Test
    void should_limit_all_traffic_using_ip_filter() {

        // match
        Steadybit.networkLimitBandwidth()
                .bandwidth(this.attackBandwidth + "mbit")
                .destAddress(target.getIperf3Address())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.attackBandwidth, Percentage.withPercentage(10));
                });

        // mismatch
        Steadybit.networkLimitBandwidth()
                .bandwidth(this.attackBandwidth + "mbit")
                .destAddress("1.1.1.1")
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth, Percentage.withPercentage(10));
                });

        assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth, Percentage.withPercentage(10));
    }

    @Test
    void should_limit_all_traffic_using_ip_and_port_filter() {

        // match
        Steadybit.networkLimitBandwidth()
                .bandwidth(this.attackBandwidth + "mbit")
                .destAddress(target.getIperf3Address())
                .destPort(target.getIperf3Port())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.attackBandwidth, Percentage.withPercentage(10));
                });

        // mismatch address
        Steadybit.networkLimitBandwidth()
                .bandwidth(this.attackBandwidth + "mbit")
                .destAddress("1.1.1.1")
                .destPort(target.getIperf3Port())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth, Percentage.withPercentage(10));
                });

        // mismatch port
        Steadybit.networkLimitBandwidth()
                .bandwidth(this.attackBandwidth + "mbit")
                .destAddress(target.getIperf3Address())
                .destPort(target.getIperf3Port() + 999)
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth, Percentage.withPercentage(10));
                });

        assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth, Percentage.withPercentage(10));
    }

}