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
class NetworkBlackholeAttackTest {

    //    @Container
    //    private static final TomcatContainer target = new TomcatContainer();
    @Container
    private static final Iperf3ServerContainer target = new Iperf3ServerContainer();
    @Container
    private static final Iperf3ClientContainer tester = new Iperf3ClientContainer(target);

    private long normalBandwidth;

    @BeforeEach
    void setUp() {
        tester.stopRunningMeasures();

        this.normalBandwidth = (tester.measureBandwidth() + tester.measureBandwidth() + tester.measureBandwidth()) / 3;
    }

    @Test
    void should_blackhole_all_traffic() {
        Steadybit.networkBlackhole()
                .port(target.getIperf3Port())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureBandwidth()).isEqualTo(0);
                });
        assertThat(tester.measureBandwidth()).isCloseTo(this.normalBandwidth, Percentage.withPercentage(10));
    }
}