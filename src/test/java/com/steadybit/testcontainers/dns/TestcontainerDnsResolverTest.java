package com.steadybit.testcontainers.dns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Testcontainers
public class TestcontainerDnsResolverTest {
    @Container
    static GenericContainer<?> toys = new GenericContainer<>("steadybit/bestseller-toys:1.0.0-SNAPSHOT")
            .withExposedPorts(8081)
            .withExtraHost("some-extra-host", "192.168.2.1")
            .withExtraHost("some-extra-host", "192.168.2.2")
            .waitingFor(Wait.forHttp("/actuator/health"));

    @Test
    void should_resolve() {
        List<String> resolved = new TestcontainersDnsResolver(toys).resolve(Arrays.asList("127.0.0.1", "heise.de", "some-extra-host"));
        assertThat(resolved).containsExactlyInAnyOrder("127.0.0.1", "193.99.144.80", "192.168.2.1", "192.168.2.2");
    }

    @Test
    void should_throw_when_not_resolved() {
        assertThatThrownBy(() -> new TestcontainersDnsResolver(toys).resolve(Collections.singletonList("doesnt-exist")))
                .hasMessageContaining("doesnt-exist");
    }
}