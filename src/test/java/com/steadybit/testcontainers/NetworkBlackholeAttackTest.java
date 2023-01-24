package com.steadybit.testcontainers;

import com.steadybit.testcontainers.iprule.TestcontainersIpRule;
import com.steadybit.testcontainers.measure.EchoTcpContainer;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class NetworkBlackholeAttackTest {
    @Container
    private final EchoTcpContainer target = new EchoTcpContainer();

    @Test
    void should_blackhole_all_traffic() {
        Steadybit.networkBlackhole()
                .forContainers(target)
                .exec(() -> {
                    assertThat(target.ping()).isFalse();
                });

        assertThat(target.ping()).isTrue();
    }

    @Test
    void should_blackhole_traffic_using_port_filter() {
        //match
        Steadybit.networkBlackhole()
                .port(target.getEchoPortInContainer())
                .factory(containerId -> TestcontainersIpRule.usingImage("praqma/network-multitool:latest").forContainer(containerId))
                .forContainers(target).exec(() -> assertThat(target.ping()).isFalse());

        //mismatch
        Steadybit.networkBlackhole()
                .port(target.getEchoPortInContainer() + 999)
                .forContainers(target).exec(() -> assertThat(target.ping()).isTrue());

        assertThat(target.ping()).isTrue();
    }

    @Test
    void should_blackhole_traffic_using_ip_filter() {
        //match
        Steadybit.networkBlackhole()
                .address(target.getEchoAddressInContainer())
                .forContainers(target).exec(() -> assertThat(target.ping()).isFalse());

        //mismatch
        Steadybit.networkBlackhole()
                .address("1.1.1.1")
                .forContainers(target).exec(() -> assertThat(target.ping()).isTrue());

        assertThat(target.ping()).isTrue();
    }

    @Test
    void should_blackhole_traffic_using_ip_and_port_filter() {
        //match
        Steadybit.networkBlackhole()
                .address(target.getEchoAddressInContainer())
                .port(target.getEchoPortInContainer())
                .forContainers(target).exec(() -> assertThat(target.ping()).isFalse());

        //mismatch address
        Steadybit.networkBlackhole()
                .address("1.1.1.1")
                .port(target.getEchoPortInContainer())
                .forContainers(target).exec(() -> assertThat(target.ping()).isTrue());

        //mismatch port
        Steadybit.networkBlackhole()
                .address(target.getEchoAddressInContainer())
                .port(target.getEchoPortInContainer() + 999)
                .forContainers(target).exec(() -> assertThat(target.ping()).isTrue());

        assertThat(target.ping()).isTrue();
    }
}