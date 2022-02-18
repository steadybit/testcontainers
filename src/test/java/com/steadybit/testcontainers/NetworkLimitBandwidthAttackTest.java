package com.steadybit.testcontainers;

import com.steadybit.testcontainers.measure.Iperf3ClientContainer;
import com.steadybit.testcontainers.measure.Iperf3ServerContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;
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

    private Bandwidth normalBandwidth;
    private Bandwidth attackBandwidth;

    @BeforeEach
    void setUp() {
        tester.stopRunningMeasures();

        this.normalBandwidth = Bandwidth.mbit((tester.measureBandwidth() + tester.measureBandwidth() + tester.measureBandwidth()) / 3);
        this.attackBandwidth = Bandwidth.mbit(normalBandwidth.getValue() / 2);
    }

    @Test
    void should_limit_all_traffic() {
        Steadybit.networkLimitBandwidth(attackBandwidth)
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.attackBandwidth.getValue(), withPercentage(10));
                });
        assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth.getValue(), withPercentage(10));
    }

    @Test
    void should_limit_all_traffic_using_port_filter() {

        // match
        Steadybit.networkLimitBandwidth(this.attackBandwidth)
                .destPort(tester.getDataPort())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.attackBandwidth.getValue(), withPercentage(10));
                });

        // mismatch
        Steadybit.networkLimitBandwidth(this.attackBandwidth)
                .destPort(tester.getDataPort() + 999)
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth.getValue(), withPercentage(10));
                });

        assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth.getValue(), withPercentage(10));
    }

    @Test
    void should_limit_all_traffic_using_ip_filter() {

        // match
        Steadybit.networkLimitBandwidth(this.attackBandwidth)
                .destAddress(tester.getIperfClientAddress())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.attackBandwidth.getValue(), withPercentage(10));
                });

        // mismatch
        Steadybit.networkLimitBandwidth(this.attackBandwidth)
                .destAddress("1.1.1.1")
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth.getValue(), withPercentage(10));
                });

        assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth.getValue(), withPercentage(10));
    }

    @Test
    void should_limit_all_traffic_using_ip_and_port_filter() {

        // match
        Steadybit.networkLimitBandwidth(this.attackBandwidth)
                .destAddress(tester.getIperfClientAddress())
                .destPort(tester.getDataPort())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.attackBandwidth.getValue(), withPercentage(10));
                });

        // mismatch address
        Steadybit.networkLimitBandwidth(this.attackBandwidth)
                .destAddress("1.1.1.1")
                .destPort(tester.getDataPort())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth.getValue(), withPercentage(10));
                });

        // mismatch port
        Steadybit.networkLimitBandwidth(this.attackBandwidth)
                .destAddress(tester.getIperfClientAddress())
                .destPort(tester.getDataPort() + 999)
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth.getValue(), withPercentage(10));
                });

        assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth.getValue(), withPercentage(10));
    }

}