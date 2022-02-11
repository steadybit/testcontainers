package com.steadybit.testcontainers;

import com.steadybit.testcontainers.measure.Iperf3ClientContainer;
import com.steadybit.testcontainers.measure.Iperf3ServerContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.data.Offset.offset;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class NetworkCorruptPackagesAttackTest {
    @Container
    private static final Iperf3ServerContainer target = new Iperf3ServerContainer();
    @Container
    private static final Iperf3ClientContainer tester = new Iperf3ClientContainer(target);

    @BeforeEach
    void setUp() {
        tester.stopRunningMeasures();
    }

    @Test
    void should_corrupt_packages() {
        Steadybit.networkCorruptPackages()
                .corruptionPercentage(20)
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isCloseTo(20, offset(5));
                });
        assertThat(tester.measureLoss()).isLessThan(5);
    }

    @Test
    void should_corrupt_packages_using_port_filter() {
        //match
        Steadybit.networkCorruptPackages()
                .corruptionPercentage(20)
                .destPort(tester.getDataPort())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isCloseTo(20, offset(5));
                });

        // mismatch
        Steadybit.networkCorruptPackages()
                .corruptionPercentage(20)
                .destPort(tester.getDataPort() + 999)
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isLessThan(5);
                });
        assertThat(tester.measureLoss()).isLessThan(5);
    }

    @Test
    void should_corrupt_packages_using_ip_filter() {
        //match
        Steadybit.networkCorruptPackages()
                .corruptionPercentage(20)
                .destAddress(tester.getIperfClientAddress())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isCloseTo(20, offset(5));
                });

        // mismatch
        Steadybit.networkCorruptPackages()
                .corruptionPercentage(20)
                .destAddress("1.1.1.1")
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isLessThan(5);
                });
        assertThat(tester.measureLoss()).isLessThan(5);
    }

    @Test
    void should_corrupt_packages_using_ip_and_port_filter() {
        //match
        Steadybit.networkCorruptPackages()
                .corruptionPercentage(20)
                .destAddress(tester.getIperfClientAddress())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isCloseTo(20, offset(5));
                });

        // mismatch address
        Steadybit.networkCorruptPackages()
                .corruptionPercentage(20)
                .destAddress("1.1.1.1")
                .destPort(tester.getDataPort())
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isLessThan(5);
                });

        // mismatch port
        Steadybit.networkCorruptPackages()
                .corruptionPercentage(20)
                .destAddress(tester.getIperfClientAddress())
                .destPort(tester.getDataPort() + 999)
                .forContainers(target)
                .exec(() -> {
                    assertThat(tester.measureLoss()).isLessThan(5);
                });

        assertThat(tester.measureLoss()).isLessThan(5);
    }

    @Test
    void should_validate_corruptionPercentage() {
        Exception exceptionToLow = assertThrows(RuntimeException.class, () -> {
            Steadybit.networkCorruptPackages()
                    .corruptionPercentage(-1)
                    .forContainers(target);
        });
        assertThat(exceptionToLow.getMessage()).isEqualTo("corruptionPercentage should be between 0-100");

        Exception exceptionToHigh = assertThrows(RuntimeException.class, () -> {
            Steadybit.networkCorruptPackages()
                    .corruptionPercentage(101)
                    .forContainers(target);
        });
        assertThat(exceptionToHigh.getMessage()).isEqualTo("corruptionPercentage should be between 0-100");

        assertDoesNotThrow(() -> {
            Steadybit.networkCorruptPackages()
                    .corruptionPercentage(99)
                    .forContainers(target);
        });
    }
}