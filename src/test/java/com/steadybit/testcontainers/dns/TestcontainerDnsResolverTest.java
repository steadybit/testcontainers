package com.steadybit.testcontainers.dns;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

@Testcontainers
public class TestcontainerDnsResolverTest {
    @Container
    static GenericContainer<?> toys = new GenericContainer<>("steadybit/bestseller-toys:1.0.0-SNAPSHOT")
            .withExposedPorts(8081)
            .waitingFor(Wait.forHttp("/actuator/health"));

    @Test
    void should_resolve_from_container() {
        List<String> resolved = new TestcontainerDnsResolver(toys).resolve(
                Arrays.asList("127.0.0.1", "heise.de", "does-not-exist")
        );
        assertThat(resolved).contains("127.0.0.1", "193.99.144.80");
    }
}