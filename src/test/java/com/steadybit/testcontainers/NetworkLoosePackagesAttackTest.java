package com.steadybit.testcontainers;

import com.steadybit.testcontainers.measure.Iperf3ClientContainer;
import com.steadybit.testcontainers.measure.Iperf3ServerContainer;
import static org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class NetworkLoosePackagesAttackTest {

    @Container
    private static final Iperf3ServerContainer target = new Iperf3ServerContainer();
    @Container
    private static final Iperf3ClientContainer tester = new Iperf3ClientContainer(target);

    @BeforeEach
    void setUp() {
        tester.stopRunningMeasures();
    }

    @Test
    void should_loose_some_packages() {
        Steadybit.networkLoosePackages()
                .lossPercentage(20)
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isCloseTo(20, Offset.offset(5));
                });
        assertThat(tester.measureLoss()).isLessThan(5);
    }

    @Test
    void should_loose_some_packages_using_port_filter() {

        //match
        Steadybit.networkLoosePackages()
                .lossPercentage(20)
                .destPort(target.getIperf3Port())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isCloseTo(20, Offset.offset(5));
                });

        // mismatch
        Steadybit.networkLoosePackages()
                .lossPercentage(20)
                .destPort(target.getIperf3Port() + 999)
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isCloseTo(20, Offset.offset(5));
                });
        assertThat(tester.measureLoss()).isLessThan(5);
    }

    @Test
    void should_loose_some_packages_using_ip_filter() {

        //match
        Steadybit.networkLoosePackages()
                .lossPercentage(20)
                .destAddress(target.getIperf3Address())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isCloseTo(20, Offset.offset(5));
                });

        // mismatch
        Steadybit.networkLoosePackages()
                .lossPercentage(20)
                .destAddress("1.1.1.1")
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isCloseTo(20, Offset.offset(5));
                });
        assertThat(tester.measureLoss()).isLessThan(5);
    }

    @Test
    void should_loose_some_packages_using_ip_and_port_filter() {

        //match
        Steadybit.networkLoosePackages()
                .lossPercentage(20)
                .destAddress(target.getIperf3Address())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isCloseTo(20, Offset.offset(5));
                });

        // mismatch address
        Steadybit.networkLoosePackages()
                .lossPercentage(20)
                .destAddress("1.1.1.1")
                .destPort(target.getIperf3Port())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isCloseTo(20, Offset.offset(5));
                });

        // mismatch port
        Steadybit.networkLoosePackages()
                .lossPercentage(20)
                .destAddress(target.getIperf3Address())
                .destPort(target.getIperf3Port() + 999)
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isCloseTo(20, Offset.offset(5));
                });

        assertThat(tester.measureLoss()).isLessThan(5);
    }
}