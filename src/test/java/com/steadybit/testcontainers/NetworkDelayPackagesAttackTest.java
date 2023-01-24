package com.steadybit.testcontainers;

import com.steadybit.testcontainers.measure.EchoTcpContainer;
import com.steadybit.testcontainers.trafficcontrol.TestcontainersTrafficControl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

@Testcontainers
class NetworkDelayPackagesAttackTest {

    @Container
    private static final EchoTcpContainer target = new EchoTcpContainer();

    @Test
    void should_delay_egress_traffic() {
        long withoutAttack = target.measureRoundtrip();

        Steadybit.networkDelayPackages(Duration.ofMillis(200))
                .forContainers(target).exec(() -> assertThat(target.measureRoundtrip()).isCloseTo(withoutAttack + 200L, offset(50L)));

        assertThat(target.measureRoundtrip()).isCloseTo(withoutAttack, offset(50L));
    }

    @Test
    void should_delay_egress_traffic_using_port_filter() {
        long withoutAttack = target.measureRoundtrip();

        //match
        Steadybit.networkDelayPackages(Duration.ofMillis(200))
                .factory(containerId -> TestcontainersTrafficControl.usingImage("praqma/network-multitool:latest").forContainer(containerId))
                .destPort(target.getEchoPortInContainer())
                .forContainers(target).exec(() -> assertThat(target.measureRoundtrip()).isCloseTo(withoutAttack + 200L, offset(50L)));

        //mismatch
        Steadybit.networkDelayPackages(Duration.ofMillis(200))
                .destPort(target.getEchoPortInContainer() + 999)
                .forContainers(target).exec(() -> assertThat(target.measureRoundtrip()).isCloseTo(withoutAttack, offset(50L)));

        assertThat(target.measureRoundtrip()).isCloseTo(withoutAttack, offset(50L));
    }

    @Test
    void should_delay_egress_traffic_using_ip_filter() {
        long withoutAttack = target.measureRoundtrip();

        //match
        Steadybit.networkDelayPackages(Duration.ofMillis(200))
                .destAddress(target.getEchoAddressInContainer())
                .forContainers(target).exec(() -> assertThat(target.measureRoundtrip()).isCloseTo(withoutAttack + 200L, offset(50L)));

        //mismatch
        Steadybit.networkDelayPackages(Duration.ofMillis(200))
                .destAddress("1.1.1.1")
                .forContainers(target).exec(() -> assertThat(target.measureRoundtrip()).isCloseTo(withoutAttack, offset(50L)));

        assertThat(target.measureRoundtrip()).isCloseTo(withoutAttack, offset(50L));
    }

    @Test
    void should_delay_egress_traffic_using_ip_and_port_filter() {
        long withoutAttack = target.measureRoundtrip();

        //match
        Steadybit.networkDelayPackages(Duration.ofMillis(200))
                .destAddress(target.getEchoAddressInContainer())
                .destPort(target.getEchoPortInContainer())
                .forContainers(target).exec(() -> assertThat(target.measureRoundtrip()).isCloseTo(withoutAttack + 200L, offset(50L)));

        //mismatch address
        Steadybit.networkDelayPackages(Duration.ofMillis(200))
                .destAddress("1.1.1.1")
                .destPort(target.getEchoPortInContainer())
                .forContainers(target).exec(() -> assertThat(target.measureRoundtrip()).isCloseTo(withoutAttack, offset(50L)));

        //mismatch port
        Steadybit.networkDelayPackages(Duration.ofMillis(200))
                .destAddress(target.getEchoAddressInContainer())
                .destPort(target.getEchoPortInContainer() + 999)
                .forContainers(target).exec(() -> assertThat(target.measureRoundtrip()).isCloseTo(withoutAttack, offset(50L)));

        assertThat(target.measureRoundtrip()).isCloseTo(withoutAttack, offset(50L));
    }
}